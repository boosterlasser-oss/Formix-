package com.fantasyfoodplanner.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.fantasyfoodplanner.data.AppDb
import com.fantasyfoodplanner.data.ScannedFoodEntity
import com.fantasyfoodplanner.data.remote.OffClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Code-Extraktion Ergebnis
 */
sealed class ExtractResult {
    data class NumericCode(val code: String, val codeType: String) : ExtractResult()
    data class NonNumeric(val rawValue: String) : ExtractResult()
}

/**
 * Repository für den Barcode/QR → Open Food Facts Flow.
 *
 * Flow:
 * 1. Scan → extractProductCode()
 * 2. Debounce (gleicher Code < 2500ms → ignorieren)
 * 3. Sofort lokal speichern (PENDING)
 * 4. Asynchron API-Lookup
 * 5. Lokales Update (OK / NOT_FOUND / ERROR)
 */
class FoodScanRepository(private val context: Context) {

    private val dao = AppDb.get(context).scannedFoodDao()
    private val api = OffClient.api

    // Debounce
    private var lastCode: String? = null
    private var lastTime: Long = 0L
    @Volatile private var isProcessing = false

    // ════════════════════════════════════════════
    // CODE EXTRAKTION
    // ════════════════════════════════════════════

    fun extractProductCode(rawValue: String): ExtractResult {
        val cleaned = rawValue.trim().replace(Regex("[\\p{Cc}\\p{Cf}]"), "")

        // Nur Ziffern und 8-14 Stellen → Barcode
        if (cleaned.matches(Regex("^\\d{8,14}$"))) {
            val type = when (cleaned.length) {
                8 -> "EAN8"
                12 -> "UPC"
                13 -> "EAN13"
                14 -> "EAN14"
                else -> "EAN"
            }
            return ExtractResult.NumericCode(cleaned, type)
        }

        // URL oder gemischter Text → Zahlenfolge 8-14 suchen
        val match = Regex("\\d{8,14}").find(cleaned)
        if (match != null) {
            val code = match.value
            val type = when (code.length) {
                8 -> "EAN8"
                12 -> "UPC"
                13 -> "EAN13"
                14 -> "EAN14"
                else -> "EAN"
            }
            return ExtractResult.NumericCode(code, type)
        }

        // Kein numerischer Code gefunden
        return ExtractResult.NonNumeric(cleaned)
    }

    // ════════════════════════════════════════════
    // HANDLE SCAN
    // ════════════════════════════════════════════

    /**
     * Haupteinstiegspunkt: Wird bei jedem erkannten Code aufgerufen.
     *
     * @param rawValue Der rohe Scan-Wert
     * @param format   Optionales Format (z.B. "QR_CODE", "EAN_13")
     * @return ScannedFoodEntity oder null bei Debounce
     */
    suspend fun handleScan(rawValue: String, format: String? = null): ScannedFoodEntity? {
        val extracted = extractProductCode(rawValue)

        when (extracted) {
            is ExtractResult.NonNumeric -> {
                // Non-numeric QR → sofort NOT_FOUND speichern
                val entity = ScannedFoodEntity(
                    code = extracted.rawValue.take(200), // Länge begrenzen
                    codeType = "QR",
                    status = "NOT_FOUND",
                    lastError = "non_numeric_qr"
                )
                dao.insertIgnore(entity)
                return dao.getByCode(entity.code)
            }
            is ExtractResult.NumericCode -> {
                val code = extracted.code
                val codeType = format?.uppercase()?.replace("_", "") ?: extracted.codeType

                // ── Debounce ──
                val now = System.currentTimeMillis()
                if (code == lastCode && (now - lastTime) < 2500L) {
                    return dao.getByCode(code) // Bestehenden Eintrag zurückgeben
                }
                if (isProcessing) return dao.getByCode(code)

                lastCode = code
                lastTime = now
                isProcessing = true

                try {
                    // ── Prüfe ob schon OK in DB ──
                    val existing = dao.getByCode(code)
                    if (existing != null && existing.status == "OK") {
                        // Cache hit – kein neuer API-Call
                        return existing
                    }

                    // ── Sofort lokal speichern (PENDING) ──
                    if (existing == null) {
                        dao.insertIgnore(
                            ScannedFoodEntity(
                                code = code,
                                codeType = codeType,
                                status = "PENDING"
                            )
                        )
                    } else {
                        dao.updateStatus(code, "PENDING", lastError = null)
                    }

                    // ── API Lookup ──
                    if (!isOnline()) {
                        dao.updateStatus(code, "PENDING", lastError = "offline")
                        return dao.getByCode(code)
                    }

                    return fetchAndUpdate(code)

                } catch (e: Exception) {
                    Log.e("FoodScanRepo", "handleScan error", e)
                    dao.updateStatus(code, "ERROR", lastError = e.message?.take(200))
                    return dao.getByCode(code)
                } finally {
                    // Lock nach kurzer Pause freigeben
                    kotlinx.coroutines.delay(800)
                    isProcessing = false
                }
            }
        }
    }

    /**
     * Retry für fehlgeschlagene/ausstehende Scans.
     */
    suspend fun retryFetch(code: String): ScannedFoodEntity? {
        dao.updateStatus(code, "PENDING", lastError = null)
        if (!isOnline()) {
            dao.updateStatus(code, "PENDING", lastError = "offline")
            return dao.getByCode(code)
        }
        return try {
            fetchAndUpdate(code)
        } catch (e: Exception) {
            dao.updateStatus(code, "ERROR", lastError = e.message?.take(200))
            dao.getByCode(code)
        }
    }

    // ════════════════════════════════════════════
    // OBSERVE
    // ════════════════════════════════════════════

    fun observeByCode(code: String): Flow<ScannedFoodEntity?> = dao.observeByCode(code)
    fun observeRecent(limit: Int = 50): Flow<List<ScannedFoodEntity>> = dao.observeRecent(limit)

    // ════════════════════════════════════════════
    // PRIVATE
    // ════════════════════════════════════════════

    private suspend fun fetchAndUpdate(code: String): ScannedFoodEntity? = withContext(Dispatchers.IO) {
        try {
            val response = api.getProduct(code)

            if (response.status == 1 && response.product != null) {
                val p = response.product
                val n = p.nutriments

                // Nährwerte: 100g bevorzugt, Fallback auf Serving
                val kcal = n?.energyKcal100g ?: n?.energyKcalServing
                val protein = n?.proteins100g ?: n?.proteinsServing
                val carbs = n?.carbohydrates100g ?: n?.carbohydratesServing
                val fat = n?.fat100g ?: n?.fatServing

                val name = p.productName?.ifBlank { null } ?: "Unbekanntes Produkt"
                val brand = p.brands?.ifBlank { null }

                dao.updateByCode(
                    code = code,
                    status = "OK",
                    name = name,
                    brand = brand,
                    imageUrl = p.imageFrontUrl,
                    kcal100g = kcal,
                    protein100g = protein,
                    carbs100g = carbs,
                    fat100g = fat,
                    nutritionGrade = p.nutritionGrades,
                    servingSize = p.servingSize,
                    lastError = null
                )
            } else {
                dao.updateStatus(code, "NOT_FOUND")
            }

            dao.getByCode(code)
        } catch (e: Exception) {
            Log.e("FoodScanRepo", "API error for $code", e)
            dao.updateStatus(code, "ERROR", lastError = e.message?.take(200))
            dao.getByCode(code)
        }
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

