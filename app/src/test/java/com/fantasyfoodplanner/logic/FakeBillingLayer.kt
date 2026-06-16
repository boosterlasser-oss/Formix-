package com.fantasyfoodplanner.logic

/**
 * FakeBillingLayer – Simulationsebene für Billing-Tests.
 *
 * Spiegelt die echte Logik aus BillingManager + SubscriptionManager
 * ohne echten Google Play Kontext.
 * Verändert NICHT den echten Code – lediglich eine Test-Schicht.
 *
 * ──────────────────────────────────────────────────────────────
 * SCHRITT 1 – ERKANNTE PRODUKTE AUS DEM ECHTEN CODE:
 *
 * Das System kennt 3 Tiers / 2 kaufbare Stufen:
 *
 *   Tier A: FREE
 *     - Kein Produkt / kein Kauf nötig
 *     - Standardzustand
 *     - Rolle: Basisnutzung mit Limits
 *
 *   Tier B: PREMIUM  (2 Subscriptions + 1 Lifetime)
 *     - formix_premium_monthly   → Subscription, 4,99€/Monat
 *     - formix_premium_yearly    → Subscription, 29,99€/Jahr
 *     - formix_premium_lifetime  → In-App Einmalkauf, 79,99€
 *     - Rolle: Alle Features außer KI-Coach
 *
 *   Tier C: PRO  (2 Subscriptions + 1 Lifetime)
 *     - formix_pro_monthly       → Subscription, 9,99€/Monat
 *     - formix_pro_yearly        → Subscription, 59,99€/Jahr
 *     - formix_pro_lifetime      → In-App Einmalkauf, 149,99€
 *     - Rolle: Alle Features inkl. KI-Coach – höchste Stufe
 *
 * Priorität: PRO > PREMIUM > FREE
 * ──────────────────────────────────────────────────────────────
 */

// ── Simulierbare Zustände ──────────────────────────────────────

enum class FakePurchaseState {
    NONE,                    // Kein Kauf vorhanden
    PURCHASED_UNACKNOWLEDGED,// Kauf erfolgt, noch nicht bestätigt
    PURCHASED_ACKNOWLEDGED,  // Kauf erfolgt und bestätigt
    PENDING,                 // Kauf ausstehend (z.B. Prepaid)
    CANCELED,                // Kauf abgebrochen vom User
    FAILED,                  // Kauf fehlgeschlagen (Netzwerk etc.)
    SUB_ACTIVE,              // Abo aktiv
    SUB_RENEWED,             // Abo verlängert
    SUB_CANCELED_STILL_VALID,// Abo gekündigt, noch gültig bis Periodenende
    SUB_EXPIRED,             // Abo abgelaufen
    INAPP_ACTIVE,            // Einmalkauf aktiv
    PRODUCT_NOT_FOUND,       // Produkt-ID nicht gefunden
    BILLING_DISCONNECTED,    // Keine Verbindung zu Play Billing
}

// ── Fake-Produkt ───────────────────────────────────────────────

data class FakeProduct(
    val productId: String,
    val type: FakeProductType,
    val tier: SubscriptionTier
)

enum class FakeProductType { SUBSCRIPTION, INAPP }

// ── Alle bekannten Produkte (direkt aus BillingManager.kt) ─────

val ALL_FAKE_PRODUCTS = listOf(
    FakeProduct(BillingManager.PREMIUM_MONTHLY,  FakeProductType.SUBSCRIPTION, SubscriptionTier.PREMIUM),
    FakeProduct(BillingManager.PREMIUM_YEARLY,   FakeProductType.SUBSCRIPTION, SubscriptionTier.PREMIUM),
    FakeProduct(BillingManager.PREMIUM_LIFETIME, FakeProductType.INAPP,        SubscriptionTier.PREMIUM),
    FakeProduct(BillingManager.PRO_MONTHLY,      FakeProductType.SUBSCRIPTION, SubscriptionTier.PRO),
    FakeProduct(BillingManager.PRO_YEARLY,       FakeProductType.SUBSCRIPTION, SubscriptionTier.PRO),
    FakeProduct(BillingManager.PRO_LIFETIME,     FakeProductType.INAPP,        SubscriptionTier.PRO),
)

// ── FakeBillingLayer ───────────────────────────────────────────

class FakeBillingLayer {

    // Interner Tier-Speicher (simuliert SharedPreferences)
    private var currentTier: SubscriptionTier = SubscriptionTier.FREE

    // Aktive Produkte (simuliert queryExistingPurchases)
    private val activePurchases = mutableMapOf<String, FakePurchaseState>()

    // Verbindungsstatus
    var isConnected: Boolean = true

    // ── Tier direkt setzen (simuliert handlePurchase) ──────────

    fun simulatePurchase(productId: String, state: FakePurchaseState): SimResult {
        if (!isConnected) return SimResult.Error("Billing nicht verbunden")

        val product = ALL_FAKE_PRODUCTS.find { it.productId == productId }
            ?: return SimResult.Error("Produkt-ID nicht gefunden: $productId")

        activePurchases[productId] = state

        return when (state) {
            FakePurchaseState.PURCHASED_ACKNOWLEDGED,
            FakePurchaseState.SUB_ACTIVE,
            FakePurchaseState.SUB_RENEWED,
            FakePurchaseState.SUB_CANCELED_STILL_VALID,
            FakePurchaseState.INAPP_ACTIVE -> {
                // Tier-Upgrade anwenden (echte Logik aus handlePurchase)
                applyTierFromProduct(product)
                SimResult.Success(currentTier)
            }
            FakePurchaseState.PURCHASED_UNACKNOWLEDGED -> {
                // Kauf da aber noch nicht bestätigt – Tier wird trotzdem gesetzt
                // (echte App würde acknowledgePurchase aufrufen)
                applyTierFromProduct(product)
                SimResult.PendingAcknowledgement(currentTier)
            }
            FakePurchaseState.SUB_EXPIRED,
            FakePurchaseState.CANCELED -> {
                // Abo abgelaufen oder abgebrochen → Tier neu berechnen
                activePurchases.remove(productId)
                recalculateTier()
                SimResult.Success(currentTier)
            }
            FakePurchaseState.FAILED -> SimResult.Error("Kauf fehlgeschlagen")
            FakePurchaseState.NONE -> {
                activePurchases.remove(productId)
                recalculateTier()
                SimResult.Success(currentTier)
            }
            FakePurchaseState.PRODUCT_NOT_FOUND -> SimResult.Error("Produkt nicht gefunden")
            FakePurchaseState.BILLING_DISCONNECTED -> SimResult.Error("Billing getrennt")
            FakePurchaseState.PENDING -> SimResult.Pending
        }
    }

    // ── Echte handlePurchase-Logik (identisch zu BillingManager) ──

    private fun applyTierFromProduct(product: FakeProduct) {
        // Priorität: PRO > PREMIUM (höchste aktive Stufe gewinnt)
        val newTier = product.tier
        if (newTier.ordinal > currentTier.ordinal) {
            currentTier = newTier
        }
    }

    // ── Tier neu berechnen aus allen aktiven Käufen ────────────

    private fun recalculateTier() {
        val activeValid = activePurchases.filter { (_, state) ->
            state in listOf(
                FakePurchaseState.PURCHASED_ACKNOWLEDGED,
                FakePurchaseState.SUB_ACTIVE,
                FakePurchaseState.SUB_RENEWED,
                FakePurchaseState.SUB_CANCELED_STILL_VALID,
                FakePurchaseState.INAPP_ACTIVE,
                FakePurchaseState.PURCHASED_UNACKNOWLEDGED
            )
        }
        currentTier = if (activeValid.isEmpty()) {
            SubscriptionTier.FREE
        } else {
            activeValid.keys
                .mapNotNull { id -> ALL_FAKE_PRODUCTS.find { it.productId == id }?.tier }
                .maxByOrNull { it.ordinal } ?: SubscriptionTier.FREE
        }
    }

    // ── App-Neustart simulieren (wie queryExistingPurchases) ───

    fun simulateAppRestart(): SubscriptionTier {
        recalculateTier()
        return currentTier
    }

    // ── Alle Käufe zurücksetzen ────────────────────────────────

    fun reset() {
        currentTier = SubscriptionTier.FREE
        activePurchases.clear()
        isConnected = true
    }

    // ── Aktuellen Tier abfragen ────────────────────────────────

    fun getCurrentTier(): SubscriptionTier = currentTier

    // ── Feature-Checks (identisch zu SubscriptionManager) ─────

    fun hasAiCoach()            = currentTier == SubscriptionTier.PRO
    fun hasBarcodeScanner()     = currentTier != SubscriptionTier.FREE
    fun hasAllRecipes()         = currentTier != SubscriptionTier.FREE
    fun hasAllTrainingTypes()   = currentTier != SubscriptionTier.FREE
    fun hasCloudBackup()        = currentTier == SubscriptionTier.PRO
    fun hasFullStats()          = currentTier != SubscriptionTier.FREE
    fun hasUnlimitedWorkouts()  = currentTier != SubscriptionTier.FREE
    fun getMaxWorkoutsPerWeek() = if (currentTier == SubscriptionTier.FREE) 5 else Int.MAX_VALUE
    fun getMaxRecipes()         = if (currentTier == SubscriptionTier.FREE) 50 else Int.MAX_VALUE

    fun getAllowedTrainingTypes(): List<TrainingType> {
        return if (currentTier == SubscriptionTier.FREE) {
            listOf(TrainingType.BASICS, TrainingType.HOME, TrainingType.OTHER_ACTIVITY)
        } else {
            TrainingType.entries
        }
    }
}

// ── Ergebnis-Typen ─────────────────────────────────────────────

sealed class SimResult {
    data class Success(val tier: SubscriptionTier) : SimResult()
    data class Error(val message: String) : SimResult()
    data class PendingAcknowledgement(val tier: SubscriptionTier) : SimResult()
    object Pending : SimResult()
}
