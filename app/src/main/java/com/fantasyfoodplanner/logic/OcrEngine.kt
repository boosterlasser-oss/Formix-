package com.fantasyfoodplanner.logic

import kotlin.math.abs
import kotlin.math.min

/**
 * OCR ENGINE V3 – PROFESSIONELLER NÄHRWERT-SCANNER
 *
 * Parst ML Kit OCR-Klartext und extrahiert: kcal, Protein, Kohlenhydrate, Fett.
 *
 * Professionelle Features:
 * - Fuzzy Keyword Matching (erkennt OCR-Fehler wie "Kohienhydrate", "Eiwei6", "Fetl")
 * - Multi-Strategy Parsing (Zeilen-basiert + Look-Ahead + Regex-Fallback)
 * - kJ/kcal korrekt bis 3800 kJ / 900 kcal (physikalisches Maximum pro 100g)
 * - Spalten-Handling (pro 100g = erste Zahlenspalte)
 * - Robuste Sub-Zeilen-Erkennung
 * - Akkumulation über mehrere Frames (Median-Werte)
 * - Selektives Merge (überschreibt nur erkannte Werte, nicht 0er)
 */
object OcrEngine {

    data class ScanResult(
        val name: String? = null,
        val kcal100: Double = 0.0,
        val protein100: Double = 0.0,
        val carbs100: Double = 0.0,
        val fat100: Double = 0.0,
        val portionSize: Double? = null,
        val isPlausible: Boolean = false,
        val diff100: Double = 0.0,
        val attempt: Int = 1,
        // Flags: welche Werte tatsächlich erkannt wurden
        val hasKcal: Boolean = false,
        val hasProtein: Boolean = false,
        val hasCarbs: Boolean = false,
        val hasFat: Boolean = false
    )

    // ─── Schlüsselwörter ───
    private val K_KCAL = listOf(
        "energie", "kcal", "kalorien", "energy", "brennwert", "energiegehalt",
        "calorieën", "brennw", "energiew", "kalorieën"
    )
    private val K_FAT = listOf(
        "fett", "fat", "lipid", "matières grasses", "grasas", "gesamtfett",
        "vetten", "vet", "fettgehalt", "fetl", "feti"
    )
    private val K_CARBS = listOf(
        "kohlenhydrate", "kohlenhydrat", "carbohydrate", "carbs", "glucides",
        "kohlenh", "koolhydraten", "kohlenhydr", "kohienhydrate",
        "kohlenhydrote", "kohienhyd"
    )
    private val K_PROT = listOf(
        "eiweiß", "eiweiss", "eiweis", "eiweib", "eiweiβ", "eiwei6",
        "protein", "protéines", "proteïne", "eiwei", "eiwel", "eiweif",
        "proteín", "proteine"
    )
    private val K_PORTION = listOf(
        "portion", "serving", "beutel", "becher", "schale",
        "portionsgröße", "portionsgroesse", "riegel", "scheibe", "stück"
    )
    private val K_SUB_START = listOf(
        "davon", "dav.", "dav ", "of which", "dont ", "waarvan", "- dav", "-dav"
    )

    // ═══════════════════════════════════════════
    // ENTRY POINT
    // ═══════════════════════════════════════════

    fun parseLiveText(rawText: String): ScanResult? {
        if (rawText.isBlank()) return null
        val cleaned = cleanOcrText(rawText)

        // Strategie 1: Zeilen-basiert mit Look-Ahead
        val result1 = parseWithLookAhead(cleaned)

        // Strategie 2: Regex-basiert über den ganzen Text
        val result2 = parseWithRegex(cleaned)

        // Bestes Ergebnis wählen
        return pickBest(result1, result2)
    }

    // ═══════════════════════════════════════════════════════
    // STRATEGIE 1: Zeilen-basiert mit Look-Ahead (2 Zeilen tief)
    // ═══════════════════════════════════════════════════════

    private fun parseWithLookAhead(text: String): ScanResult? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var kcal = -1.0
        var protein = -1.0
        var carbs = -1.0
        var fat = -1.0
        var portionSize: Double? = null
        var pendingCategory: String? = null
        var pendingAge = 0  // Wie viele Zeilen seit dem Keyword

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val lower = line.lowercase()

            // Sub-Zeile → überspringen + pending resetten
            if (isSubLine(lower)) {
                pendingCategory = null
                pendingAge = 0
                i++
                continue
            }

            val numbers = extractNumbers(line)

            // ── LOOK-AHEAD: Keyword auf vorheriger Zeile, Zahl auf dieser ──
            if (pendingCategory != null && numbers.isNotEmpty() && pendingAge <= 2) {
                val assigned = assignValue(pendingCategory, lower, numbers,
                    kcal, fat, carbs, protein)
                if (assigned != null) {
                    when (pendingCategory) {
                        "kcal" -> if (kcal < 0) kcal = assigned
                        "fat" -> if (fat < 0) fat = assigned
                        "carbs" -> if (carbs < 0) carbs = assigned
                        "protein" -> if (protein < 0) protein = assigned
                    }
                    pendingCategory = null
                    pendingAge = 0
                    i++
                    continue
                }
            }

            // Pending verfällt nach 2 Zeilen
            if (pendingCategory != null) {
                pendingAge++
                if (pendingAge > 2) {
                    pendingCategory = null
                    pendingAge = 0
                }
            }

            // ── Kategorie erkennen (exakt + fuzzy) ──
            val category = detectCategory(lower, kcal, fat, carbs, protein)

            if (category != null && numbers.isNotEmpty()) {
                val value = assignValue(category, lower, numbers, kcal, fat, carbs, protein)
                if (value != null) {
                    when (category) {
                        "kcal" -> kcal = value
                        "fat" -> fat = value
                        "carbs" -> carbs = value
                        "protein" -> protein = value
                        "portion" -> portionSize = numbers.firstOrNull { it in 10.0..2000.0 }
                    }
                    pendingCategory = null
                    pendingAge = 0
                } else if (category == "kcal") {
                    // kJ gefunden aber kein kcal → Zeile als kJ nutzen, aber weiter suchen
                    val kjVal = numbers.firstOrNull { it > 200 }
                    if (kjVal != null && kcal < 0) kcal = kjVal / 4.184
                    pendingCategory = "kcal"
                    pendingAge = 0
                }
            } else if (category != null && numbers.isEmpty()) {
                // Keyword ohne Zahl → Look-Ahead
                pendingCategory = category
                pendingAge = 0
            }

            i++
        }

        return buildResult(kcal, protein, carbs, fat, portionSize)
    }

    // ═══════════════════════════════════════════
    // STRATEGIE 2: Regex-basiert über den ganzen Text
    // ═══════════════════════════════════════════

    private fun parseWithRegex(text: String): ScanResult? {
        val full = text.lowercase()

        var kcal = -1.0
        var protein = -1.0
        var carbs = -1.0
        var fat = -1.0

        // kcal: "XXX kcal" irgendwo im Text
        val kcalMatch = Regex("""(\d+)\s*kcal""").find(full)
        if (kcalMatch != null) {
            val v = kcalMatch.groupValues[1].toDoubleOrNull()
            if (v != null && v in 1.0..960.0) kcal = v
        }

        // Wenn kein kcal aber kJ vorhanden
        if (kcal < 0) {
            val kjMatch = Regex("""(\d[\d.]*)\s*kj""").find(full)
            if (kjMatch != null) {
                val raw = kjMatch.groupValues[1].replace(".", "")
                val v = raw.toDoubleOrNull()
                if (v != null && v > 50) kcal = v / 4.184
            }
        }

        // Fett: "fett XX,X" oder "fat XX.X"
        val fatMatch = Regex("""(?:fett|fat)\s+(\d+[,.]?\d*)\s*g?""").find(full)
        if (fatMatch != null) {
            val v = parseGermanNumber(fatMatch.groupValues[1])
            if (v != null && v in 0.0..100.0) fat = v
        }

        // Kohlenhydrate
        val carbMatch = Regex("""(?:kohlenhydrat\w*|carbohydrat\w*|carbs)\s+(\d+[,.]?\d*)\s*g?""").find(full)
        if (carbMatch != null) {
            val v = parseGermanNumber(carbMatch.groupValues[1])
            if (v != null && v in 0.0..100.0) carbs = v
        }

        // Protein
        val protMatch = Regex("""(?:eiwei[ßsb6βflI]\w*|protein\w*)\s+(\d+[,.]?\d*)\s*g?""").find(full)
        if (protMatch != null) {
            val v = parseGermanNumber(protMatch.groupValues[1])
            if (v != null && v in 0.0..100.0) protein = v
        }

        return buildResult(kcal, protein, carbs, fat, null)
    }

    // ═══════════════════════════════════════════
    // ERGEBNIS BAUEN
    // ═══════════════════════════════════════════

    private fun buildResult(
        kcal: Double, protein: Double, carbs: Double, fat: Double,
        portionSize: Double?
    ): ScanResult? {
        val hk = kcal >= 0
        val hp = protein >= 0
        val hc = carbs >= 0
        val hf = fat >= 0
        val found = listOf(hk, hp, hc, hf).count { it }
        if (found < 2) return null

        val k = kcal.coerceAtLeast(0.0)
        val p = protein.coerceAtLeast(0.0)
        val c = carbs.coerceAtLeast(0.0)
        val f = fat.coerceAtLeast(0.0)

        val kcalCalc = p * 4.0 + c * 4.0 + f * 9.0
        val diff = abs(k - kcalCalc)

        // Plausibilität: 40% Toleranz (echte Etiketten haben Rundungsfehler + Ballaststoffe)
        val plausible = k > 0
                && diff <= maxOf(80.0, k * 0.40)
                && p in 0.0..100.0 && c in 0.0..100.0 && f in 0.0..100.0
                && (p + c + f) > 0

        return ScanResult(
            name = null,
            kcal100 = k, protein100 = p, carbs100 = c, fat100 = f,
            portionSize = portionSize, isPlausible = plausible,
            diff100 = diff, attempt = 1,
            hasKcal = hk, hasProtein = hp, hasCarbs = hc, hasFat = hf
        )
    }

    // ═══════════════════════════════════════════
    // BESTES ERGEBNIS WÄHLEN
    // ═══════════════════════════════════════════

    private fun pickBest(a: ScanResult?, b: ScanResult?): ScanResult? {
        if (a == null) return b
        if (b == null) return a
        // Plausibel gewinnt
        if (a.isPlausible && !b.isPlausible) return a
        if (b.isPlausible && !a.isPlausible) return b
        // Mehr erkannte Werte gewinnt
        val countA = listOf(a.hasKcal, a.hasProtein, a.hasCarbs, a.hasFat).count { it }
        val countB = listOf(b.hasKcal, b.hasProtein, b.hasCarbs, b.hasFat).count { it }
        return if (countA >= countB) a else b
    }

    // ═══════════════════════════════════════════
    // WERT ZUWEISEN (für eine Kategorie)
    // ═══════════════════════════════════════════

    private fun assignValue(
        category: String, lower: String, numbers: List<Double>,
        kcal: Double, fat: Double, carbs: Double, protein: Double
    ): Double? {
        return when (category) {
            "kcal" -> resolveKcal(lower, numbers)
            "fat" -> if (fat < 0) numbers.firstOrNull { it in 0.0..100.0 } else null
            "carbs" -> if (carbs < 0) numbers.firstOrNull { it in 0.0..100.0 } else null
            "protein" -> if (protein < 0) numbers.firstOrNull { it in 0.0..100.0 } else null
            "portion" -> numbers.firstOrNull { it in 10.0..2000.0 }
            else -> null
        }
    }

    // ═══════════════════════════════════════════
    // KATEGORIE-ERKENNUNG (exakt + fuzzy)
    // ═══════════════════════════════════════════

    private fun detectCategory(
        lower: String, kcal: Double, fat: Double, carbs: Double, protein: Double
    ): String? {
        // Exaktes Matching zuerst
        if (K_KCAL.any { lower.contains(it) } && kcal < 0) return "kcal"
        if (K_CARBS.any { lower.contains(it) } && carbs < 0) return "carbs"
        if (K_PROT.any { lower.contains(it) } && protein < 0) return "protein"
        if (K_FAT.any { lower.contains(it) } && fat < 0) return "fat"
        if (K_PORTION.any { lower.contains(it) }) return "portion"

        // Fuzzy Matching (Levenshtein Distanz ≤ 2)
        val words = lower.split(Regex("""[\s/]+""")).filter { it.length >= 4 }
        for (word in words) {
            if (carbs < 0 && fuzzyMatch(word, "kohlenhydrate", 3)) return "carbs"
            if (protein < 0 && fuzzyMatch(word, "eiweiß", 2)) return "protein"
            if (protein < 0 && fuzzyMatch(word, "protein", 2)) return "protein"
            if (fat < 0 && fuzzyMatch(word, "fett", 1)) return "fat"
            if (kcal < 0 && fuzzyMatch(word, "brennwert", 2)) return "kcal"
            if (kcal < 0 && fuzzyMatch(word, "energie", 2)) return "kcal"
        }

        return null
    }

    // ═══════════════════════════════════════════
    // FUZZY MATCHING (Levenshtein)
    // ═══════════════════════════════════════════

    private fun fuzzyMatch(input: String, target: String, maxDist: Int): Boolean {
        if (input == target) return true
        if (abs(input.length - target.length) > maxDist) return false
        // Für kurze Wörter: input muss mindestens 3/4 der Zielzeichenlänge haben
        if (input.length < target.length - maxDist) return false
        return levenshtein(input, target) <= maxDist
    }

    private fun levenshtein(a: String, b: String): Int {
        val m = a.length; val n = b.length
        if (m == 0) return n; if (n == 0) return m
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) for (j in 1..n) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            dp[i][j] = min(min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost)
        }
        return dp[m][n]
    }

    // ═══════════════════════════════════════════
    // SUB-ZEILEN ERKENNUNG
    // ═══════════════════════════════════════════

    private fun isSubLine(lower: String): Boolean {
        if (K_SUB_START.any { lower.startsWith(it) }) return true
        if ((lower.contains("gesättigte") || lower.contains("gesaettigte")
                    || lower.contains("saturated")) && !K_FAT.any { lower.startsWith(it) }) return true
        if (lower.startsWith("zucker") || lower.startsWith("sugar")) return true
        if (lower.startsWith("salz") || lower.startsWith("salt")) return true
        if (lower.startsWith("ballaststoff") || lower.startsWith("fibre") || lower.startsWith("fiber")) return true
        if (lower.startsWith("natrium") || lower.startsWith("sodium")) return true
        if (lower.startsWith("vitamin") || lower.startsWith("calcium") || lower.startsWith("eisen")) return true
        if (lower.startsWith("nährwert") || lower.startsWith("nutrition") || lower.startsWith("durchschnitt")) return true
        return false
    }

    // ═══════════════════════════════════════════
    // KCAL / KJ LOGIK
    // ═══════════════════════════════════════════

    private fun resolveKcal(lower: String, numbers: List<Double>): Double? {
        val hasKj = lower.contains("kj") || lower.contains("kilojoule")
        val hasKcal = lower.contains("kcal") || lower.contains("kalorien")

        // Beide → kleinere ist kcal (max 960, physikalisches Maximum = Öl ~900)
        if (hasKj && hasKcal && numbers.size >= 2) {
            return numbers.filter { it in 1.0..960.0 }.minOrNull()
        }
        // Nur kcal
        if (hasKcal && !hasKj) {
            return numbers.firstOrNull { it in 1.0..960.0 }
        }
        // Nur kJ → umrechnen
        if (hasKj && !hasKcal) {
            val kjVal = numbers.firstOrNull { it > 50 }
            return if (kjVal != null) kjVal / 4.184 else null
        }
        // Generisch (Brennwert ohne Einheit)
        if (numbers.size >= 2) {
            val sorted = numbers.filter { it > 0 }.sorted()
            if (sorted.size >= 2) {
                // Größere ist kJ, kleinere ist kcal
                return if (sorted.first() <= 960) sorted.first()
                else sorted.first() / 4.184
            }
        }
        val single = numbers.firstOrNull { it > 0 } ?: return null
        return if (single > 960) single / 4.184 else single
    }

    // ═══════════════════════════════════════════
    // TEXT-BEREINIGUNG
    // ═══════════════════════════════════════════

    private fun cleanOcrText(text: String): String {
        return text
            .replace(Regex("""(\s*\.){2,}\s*"""), " ")     // Punktlinien
            .replace(Regex("""\s{3,}"""), "  ")             // Vielfache Leerzeichen
            .replace(Regex("""<\s*"""), "")                  // < bei Spurenwerten
            .replace(Regex("""(?<=\s|^)[lI](?=\d)"""), "1") // OCR l/I → 1
            .replace(Regex("""\|"""), "")                    // Pipe-Zeichen (OCR-Artefakt)
            .replace(Regex("""\[|\]"""), "")                 // Eckige Klammern
            .replace("–", "-").replace("—", "-")            // Striche normalisieren
    }

    // ═══════════════════════════════════════════
    // ZAHLEN EXTRAKTION
    // ═══════════════════════════════════════════

    private fun extractNumbers(text: String): List<Double> {
        val cleaned = text.lowercase()
            .replace("kcal", " ").replace("kj", " ")
            .replace("mg", " ").replace("µg", " ")
            // "g" als Gramm-Einheit entfernen (nur als alleinstehende Einheit nach Zahl/Leerzeichen)
            .replace(Regex("""(?<=[\d\s])g(?=\s|$)"""), " ")
            .replace("ml", " ")
            .replace("%", " ").replace("*", " ")
            .trim()

        // Zahlen nur extrahieren wenn sie nicht Teil eines Wortes sind (kein Buchstabe direkt davor)
        val regex = Regex("""(?<![a-zäöüß])(\d{1,3}(?:\.\d{3})+(?:,\d+)?|\d+,\d+|\d+\.\d+|\d+)""")

        return regex.findAll(cleaned).mapNotNull { m ->
            val raw = m.value
            if (raw.isBlank()) return@mapNotNull null
            val normalized = when {
                raw.matches(Regex("""\d{1,3}(\.\d{3})+(,\d+)?""")) ->
                    raw.replace(".", "").replace(",", ".")
                raw.contains(",") -> raw.replace(",", ".")
                else -> raw
            }
            normalized.toDoubleOrNull()
        }.toList()
    }

    private fun parseGermanNumber(s: String): Double? {
        return s.replace(",", ".").toDoubleOrNull()
    }

    // ═══════════════════════════════════════════
    // MULTI-FRAME AKKUMULATION
    // ═══════════════════════════════════════════

    /**
     * Sammelt Ergebnisse über mehrere Frames und gibt den stabilsten Median zurück.
     * Wird vom LiveScannerScreen aufgerufen.
     */
    class FrameAccumulator(private val maxFrames: Int = 8) {
        private val history = mutableListOf<ScanResult>()

        fun addFrame(result: ScanResult?) {
            if (result != null && (result.hasKcal || result.hasProtein || result.hasCarbs || result.hasFat)) {
                history.add(result)
                if (history.size > maxFrames) history.removeAt(0)
            }
        }

        fun getStableResult(): ScanResult? {
            if (history.size < 2) return history.lastOrNull()

            val kcals = history.filter { it.hasKcal }.map { it.kcal100 }
            val prots = history.filter { it.hasProtein }.map { it.protein100 }
            val carbs = history.filter { it.hasCarbs }.map { it.carbs100 }
            val fats = history.filter { it.hasFat }.map { it.fat100 }

            val k = median(kcals)
            val p = median(prots)
            val c = median(carbs)
            val f = median(fats)

            if (k <= 0) return history.lastOrNull()

            val kcalCalc = p * 4.0 + c * 4.0 + f * 9.0
            val diff = abs(k - kcalCalc)
            val plausible = k > 0 && diff <= maxOf(80.0, k * 0.40)
                    && (p + c + f) > 0

            return ScanResult(
                kcal100 = k, protein100 = p, carbs100 = c, fat100 = f,
                isPlausible = plausible, diff100 = diff,
                hasKcal = kcals.isNotEmpty(), hasProtein = prots.isNotEmpty(),
                hasCarbs = carbs.isNotEmpty(), hasFat = fats.isNotEmpty()
            )
        }

        fun isStable(): Boolean {
            if (history.size < 3) return false
            val last3 = history.takeLast(3)
            val kcalRange = last3.filter { it.hasKcal }.map { it.kcal100 }
            if (kcalRange.size < 3) return false
            val spread = kcalRange.max() - kcalRange.min()
            return spread <= 20 // Innerhalb 20 kcal = stabil
        }

        val frameCount get() = history.size

        fun clear() = history.clear()

        private fun median(list: List<Double>): Double {
            if (list.isEmpty()) return 0.0
            val sorted = list.sorted()
            return if (sorted.size % 2 == 0) {
                (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
            } else {
                sorted[sorted.size / 2]
            }
        }
    }
}
