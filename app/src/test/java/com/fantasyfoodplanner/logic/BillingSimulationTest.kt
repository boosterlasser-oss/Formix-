package com.fantasyfoodplanner.logic

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * BillingSimulationTest – vollständige Testmatrix für die Billing-Simulation.
 *
 * SCHRITT 3: Kauf-Zustände und Tier-Erkennung
 * SCHRITT 4: Entitlement-Prüfung (Feature-Gates)
 * SCHRITT 5: UI-relevante Zustände (Limits, gesperrte Features)
 *
 * Testgruppen:
 *   A) Initialer Zustand (FREE)
 *   B) PREMIUM-Käufe – alle 3 Produkte × aktive Zustände
 *   C) PRO-Käufe – alle 3 Produkte × aktive Zustände
 *   D) Priorität: PRO schlägt PREMIUM
 *   E) Ablauf / Kündigung → Tier-Downgrade
 *   F) Fehlerszenarien (FAILED, BILLING_DISCONNECTED, unbekannte ID)
 *   G) App-Neustart (simulateAppRestart)
 *   H) Feature-Gates für FREE
 *   I) Feature-Gates für PREMIUM
 *   J) Feature-Gates für PRO
 *   K) Workout-Limit (FREE: max 5/Woche)
 *   L) Rezept-Limit (FREE: max 50)
 *   M) Erlaubte Trainingstypen
 *   N) PENDING-Kauf – kein Tier-Upgrade
 *   O) PURCHASED_UNACKNOWLEDGED – Tier trotzdem gesetzt
 */
class BillingSimulationTest {

    private lateinit var billing: FakeBillingLayer

    @Before
    fun setUp() {
        billing = FakeBillingLayer()
    }

    // ════════════════════════════════════════════════════════════
    // A) Initialer Zustand
    // ════════════════════════════════════════════════════════════

    @Test
    fun `A01 – Initialzustand ist FREE`() {
        assertEquals(SubscriptionTier.FREE, billing.getCurrentTier())
    }

    @Test
    fun `A02 – reset setzt Tier auf FREE zurueck`() {
        billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        billing.reset()
        assertEquals(SubscriptionTier.FREE, billing.getCurrentTier())
    }

    // ════════════════════════════════════════════════════════════
    // B) PREMIUM-Käufe
    // ════════════════════════════════════════════════════════════

    @Test
    fun `B01 – PREMIUM_MONTHLY aktiv setzt Tier auf PREMIUM`() {
        val result = billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        assertTrue(result is SimResult.Success)
        assertEquals(SubscriptionTier.PREMIUM, billing.getCurrentTier())
    }

    @Test
    fun `B02 – PREMIUM_YEARLY aktiv setzt Tier auf PREMIUM`() {
        billing.simulatePurchase(BillingManager.PREMIUM_YEARLY, FakePurchaseState.SUB_ACTIVE)
        assertEquals(SubscriptionTier.PREMIUM, billing.getCurrentTier())
    }

    @Test
    fun `B03 – PREMIUM_LIFETIME aktiv setzt Tier auf PREMIUM`() {
        billing.simulatePurchase(BillingManager.PREMIUM_LIFETIME, FakePurchaseState.INAPP_ACTIVE)
        assertEquals(SubscriptionTier.PREMIUM, billing.getCurrentTier())
    }

    @Test
    fun `B04 – PREMIUM_MONTHLY SUB_RENEWED setzt Tier auf PREMIUM`() {
        billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.SUB_RENEWED)
        assertEquals(SubscriptionTier.PREMIUM, billing.getCurrentTier())
    }

    @Test
    fun `B05 – PREMIUM_YEARLY SUB_CANCELED_STILL_VALID bleibt PREMIUM`() {
        billing.simulatePurchase(BillingManager.PREMIUM_YEARLY, FakePurchaseState.SUB_CANCELED_STILL_VALID)
        assertEquals(SubscriptionTier.PREMIUM, billing.getCurrentTier())
    }

    @Test
    fun `B06 – PREMIUM_MONTHLY PURCHASED_ACKNOWLEDGED setzt Tier auf PREMIUM`() {
        billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.PURCHASED_ACKNOWLEDGED)
        assertEquals(SubscriptionTier.PREMIUM, billing.getCurrentTier())
    }

    // ════════════════════════════════════════════════════════════
    // C) PRO-Käufe
    // ════════════════════════════════════════════════════════════

    @Test
    fun `C01 – PRO_MONTHLY aktiv setzt Tier auf PRO`() {
        val result = billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        assertTrue(result is SimResult.Success)
        assertEquals(SubscriptionTier.PRO, billing.getCurrentTier())
    }

    @Test
    fun `C02 – PRO_YEARLY aktiv setzt Tier auf PRO`() {
        billing.simulatePurchase(BillingManager.PRO_YEARLY, FakePurchaseState.SUB_ACTIVE)
        assertEquals(SubscriptionTier.PRO, billing.getCurrentTier())
    }

    @Test
    fun `C03 – PRO_LIFETIME aktiv setzt Tier auf PRO`() {
        billing.simulatePurchase(BillingManager.PRO_LIFETIME, FakePurchaseState.INAPP_ACTIVE)
        assertEquals(SubscriptionTier.PRO, billing.getCurrentTier())
    }

    @Test
    fun `C04 – PRO_MONTHLY SUB_RENEWED setzt Tier auf PRO`() {
        billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.SUB_RENEWED)
        assertEquals(SubscriptionTier.PRO, billing.getCurrentTier())
    }

    @Test
    fun `C05 – PRO_YEARLY SUB_CANCELED_STILL_VALID bleibt PRO`() {
        billing.simulatePurchase(BillingManager.PRO_YEARLY, FakePurchaseState.SUB_CANCELED_STILL_VALID)
        assertEquals(SubscriptionTier.PRO, billing.getCurrentTier())
    }

    @Test
    fun `C06 – PRO_LIFETIME PURCHASED_ACKNOWLEDGED setzt Tier auf PRO`() {
        billing.simulatePurchase(BillingManager.PRO_LIFETIME, FakePurchaseState.PURCHASED_ACKNOWLEDGED)
        assertEquals(SubscriptionTier.PRO, billing.getCurrentTier())
    }

    // ════════════════════════════════════════════════════════════
    // D) Priorität: PRO schlägt PREMIUM
    // ════════════════════════════════════════════════════════════

    @Test
    fun `D01 – PRO nach PREMIUM ueberschreibt Tier auf PRO`() {
        billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        assertEquals(SubscriptionTier.PRO, billing.getCurrentTier())
    }

    @Test
    fun `D02 – PREMIUM nach PRO aendert Tier NICHT zurueck`() {
        billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        // applyTierFromProduct prüft ordinal – PREMIUM kann PRO nicht überschreiben
        assertEquals(SubscriptionTier.PRO, billing.getCurrentTier())
    }

    @Test
    fun `D03 – recalculateTier waehlt hoechsten aktiven Tier`() {
        billing.simulatePurchase(BillingManager.PREMIUM_YEARLY, FakePurchaseState.SUB_ACTIVE)
        billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        // PRO läuft ab → sollte auf PREMIUM fallen
        billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.SUB_EXPIRED)
        assertEquals(SubscriptionTier.PREMIUM, billing.getCurrentTier())
    }

    // ════════════════════════════════════════════════════════════
    // E) Ablauf / Kündigung → Tier-Downgrade
    // ════════════════════════════════════════════════════════════

    @Test
    fun `E01 – SUB_EXPIRED setzt Tier auf FREE wenn kein anderes aktiv`() {
        billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.SUB_EXPIRED)
        assertEquals(SubscriptionTier.FREE, billing.getCurrentTier())
    }

    @Test
    fun `E02 – CANCELED setzt Tier auf FREE wenn kein anderes aktiv`() {
        billing.simulatePurchase(BillingManager.PRO_YEARLY, FakePurchaseState.SUB_ACTIVE)
        billing.simulatePurchase(BillingManager.PRO_YEARLY, FakePurchaseState.CANCELED)
        assertEquals(SubscriptionTier.FREE, billing.getCurrentTier())
    }

    @Test
    fun `E03 – NONE entfernt Produkt und berechnet Tier neu`() {
        billing.simulatePurchase(BillingManager.PREMIUM_LIFETIME, FakePurchaseState.INAPP_ACTIVE)
        billing.simulatePurchase(BillingManager.PREMIUM_LIFETIME, FakePurchaseState.NONE)
        assertEquals(SubscriptionTier.FREE, billing.getCurrentTier())
    }

    @Test
    fun `E04 – PRO laeuft ab aber PREMIUM noch aktiv – Tier bleibt PREMIUM`() {
        billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        billing.simulatePurchase(BillingManager.PREMIUM_YEARLY, FakePurchaseState.SUB_ACTIVE)
        billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.SUB_EXPIRED)
        assertEquals(SubscriptionTier.PREMIUM, billing.getCurrentTier())
    }

    // ════════════════════════════════════════════════════════════
    // F) Fehlerszenarien
    // ════════════════════════════════════════════════════════════

    @Test
    fun `F01 – FAILED liefert SimResult Error`() {
        val result = billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.FAILED)
        assertTrue(result is SimResult.Error)
    }

    @Test
    fun `F02 – FAILED veraendert Tier nicht`() {
        billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.FAILED)
        assertEquals(SubscriptionTier.FREE, billing.getCurrentTier())
    }

    @Test
    fun `F03 – BILLING_DISCONNECTED state liefert Error`() {
        val result = billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.BILLING_DISCONNECTED)
        assertTrue(result is SimResult.Error)
    }

    @Test
    fun `F04 – isConnected false blockiert jeden Kauf`() {
        billing.isConnected = false
        val result = billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        assertTrue(result is SimResult.Error)
        assertEquals(SubscriptionTier.FREE, billing.getCurrentTier())
    }

    @Test
    fun `F05 – unbekannte Produkt-ID liefert Error`() {
        val result = billing.simulatePurchase("formix_unknown_product", FakePurchaseState.SUB_ACTIVE)
        assertTrue(result is SimResult.Error)
    }

    @Test
    fun `F06 – PRODUCT_NOT_FOUND state liefert Error`() {
        val result = billing.simulatePurchase(BillingManager.PRO_YEARLY, FakePurchaseState.PRODUCT_NOT_FOUND)
        assertTrue(result is SimResult.Error)
    }

    @Test
    fun `F07 – nach Disconnect und Reconnect funktioniert Kauf wieder`() {
        billing.isConnected = false
        billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        assertEquals(SubscriptionTier.FREE, billing.getCurrentTier())

        billing.isConnected = true
        billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        assertEquals(SubscriptionTier.PRO, billing.getCurrentTier())
    }

    // ════════════════════════════════════════════════════════════
    // G) App-Neustart (simulateAppRestart)
    // ════════════════════════════════════════════════════════════

    @Test
    fun `G01 – Neustart mit aktivem Abo behaelt Tier`() {
        billing.simulatePurchase(BillingManager.PRO_YEARLY, FakePurchaseState.SUB_ACTIVE)
        val tierAfterRestart = billing.simulateAppRestart()
        assertEquals(SubscriptionTier.PRO, tierAfterRestart)
    }

    @Test
    fun `G02 – Neustart ohne Kaeufe ergibt FREE`() {
        val tierAfterRestart = billing.simulateAppRestart()
        assertEquals(SubscriptionTier.FREE, tierAfterRestart)
    }

    @Test
    fun `G03 – Neustart nach Ablauf ergibt FREE`() {
        billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.SUB_EXPIRED)
        val tierAfterRestart = billing.simulateAppRestart()
        assertEquals(SubscriptionTier.FREE, tierAfterRestart)
    }

    @Test
    fun `G04 – Neustart mit CANCELED_STILL_VALID behaelt Tier`() {
        billing.simulatePurchase(BillingManager.PREMIUM_YEARLY, FakePurchaseState.SUB_CANCELED_STILL_VALID)
        val tierAfterRestart = billing.simulateAppRestart()
        assertEquals(SubscriptionTier.PREMIUM, tierAfterRestart)
    }

    // ════════════════════════════════════════════════════════════
    // H) Feature-Gates FREE
    // ════════════════════════════════════════════════════════════

    @Test
    fun `H01 – FREE hat keinen KI-Coach`() {
        assertFalse(billing.hasAiCoach())
    }

    @Test
    fun `H02 – FREE hat keinen Barcode-Scanner`() {
        assertFalse(billing.hasBarcodeScanner())
    }

    @Test
    fun `H03 – FREE hat keine unbegrenzte Rezepte`() {
        assertFalse(billing.hasAllRecipes())
    }

    @Test
    fun `H04 – FREE hat kein Cloud-Backup`() {
        assertFalse(billing.hasCloudBackup())
    }

    @Test
    fun `H05 – FREE hat keine erweiterten Statistiken`() {
        assertFalse(billing.hasFullStats())
    }

    @Test
    fun `H06 – FREE hat keine unbegrenzten Workouts`() {
        assertFalse(billing.hasUnlimitedWorkouts())
    }

    // ════════════════════════════════════════════════════════════
    // I) Feature-Gates PREMIUM
    // ════════════════════════════════════════════════════════════

    @Test
    fun `I01 – PREMIUM hat Barcode-Scanner`() {
        billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        assertTrue(billing.hasBarcodeScanner())
    }

    @Test
    fun `I02 – PREMIUM hat alle Rezepte`() {
        billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        assertTrue(billing.hasAllRecipes())
    }

    @Test
    fun `I03 – PREMIUM hat alle Trainingstypen`() {
        billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        assertTrue(billing.hasAllTrainingTypes())
    }

    @Test
    fun `I04 – PREMIUM hat unbegrenzte Workouts`() {
        billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        assertTrue(billing.hasUnlimitedWorkouts())
    }

    @Test
    fun `I05 – PREMIUM hat vollstaendige Statistiken`() {
        billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        assertTrue(billing.hasFullStats())
    }

    @Test
    fun `I06 – PREMIUM hat KEINEN KI-Coach`() {
        billing.simulatePurchase(BillingManager.PREMIUM_YEARLY, FakePurchaseState.SUB_ACTIVE)
        assertFalse(billing.hasAiCoach())
    }

    @Test
    fun `I07 – PREMIUM hat KEIN Cloud-Backup`() {
        billing.simulatePurchase(BillingManager.PREMIUM_LIFETIME, FakePurchaseState.INAPP_ACTIVE)
        assertFalse(billing.hasCloudBackup())
    }

    // ════════════════════════════════════════════════════════════
    // J) Feature-Gates PRO
    // ════════════════════════════════════════════════════════════

    @Test
    fun `J01 – PRO hat KI-Coach`() {
        billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        assertTrue(billing.hasAiCoach())
    }

    @Test
    fun `J02 – PRO hat Barcode-Scanner`() {
        billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        assertTrue(billing.hasBarcodeScanner())
    }

    @Test
    fun `J03 – PRO hat Cloud-Backup`() {
        billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        assertTrue(billing.hasCloudBackup())
    }

    @Test
    fun `J04 – PRO hat alle Rezepte`() {
        billing.simulatePurchase(BillingManager.PRO_YEARLY, FakePurchaseState.SUB_ACTIVE)
        assertTrue(billing.hasAllRecipes())
    }

    @Test
    fun `J05 – PRO hat alle Trainingstypen`() {
        billing.simulatePurchase(BillingManager.PRO_YEARLY, FakePurchaseState.SUB_ACTIVE)
        assertTrue(billing.hasAllTrainingTypes())
    }

    @Test
    fun `J06 – PRO hat unbegrenzte Workouts`() {
        billing.simulatePurchase(BillingManager.PRO_LIFETIME, FakePurchaseState.INAPP_ACTIVE)
        assertTrue(billing.hasUnlimitedWorkouts())
    }

    // ════════════════════════════════════════════════════════════
    // K) Workout-Limit
    // ════════════════════════════════════════════════════════════

    @Test
    fun `K01 – FREE Workout-Limit ist 5`() {
        assertEquals(5, billing.getMaxWorkoutsPerWeek())
    }

    @Test
    fun `K02 – PREMIUM Workout-Limit ist unbegrenzt`() {
        billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        assertEquals(Int.MAX_VALUE, billing.getMaxWorkoutsPerWeek())
    }

    @Test
    fun `K03 – PRO Workout-Limit ist unbegrenzt`() {
        billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        assertEquals(Int.MAX_VALUE, billing.getMaxWorkoutsPerWeek())
    }

    @Test
    fun `K04 – nach Ablauf gilt wieder FREE-Limit`() {
        billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.SUB_EXPIRED)
        assertEquals(5, billing.getMaxWorkoutsPerWeek())
    }

    // ════════════════════════════════════════════════════════════
    // L) Rezept-Limit
    // ════════════════════════════════════════════════════════════

    @Test
    fun `L01 – FREE Rezept-Limit ist 50`() {
        assertEquals(50, billing.getMaxRecipes())
    }

    @Test
    fun `L02 – PREMIUM Rezept-Limit ist unbegrenzt`() {
        billing.simulatePurchase(BillingManager.PREMIUM_YEARLY, FakePurchaseState.SUB_ACTIVE)
        assertEquals(Int.MAX_VALUE, billing.getMaxRecipes())
    }

    @Test
    fun `L03 – PRO Rezept-Limit ist unbegrenzt`() {
        billing.simulatePurchase(BillingManager.PRO_LIFETIME, FakePurchaseState.INAPP_ACTIVE)
        assertEquals(Int.MAX_VALUE, billing.getMaxRecipes())
    }

    // ════════════════════════════════════════════════════════════
    // M) Erlaubte Trainingstypen
    // ════════════════════════════════════════════════════════════

    @Test
    fun `M01 – FREE erlaubt nur BASICS HOME OTHER_ACTIVITY`() {
        val allowed = billing.getAllowedTrainingTypes()
        assertEquals(3, allowed.size)
        assertTrue(allowed.contains(TrainingType.BASICS))
        assertTrue(allowed.contains(TrainingType.HOME))
        assertTrue(allowed.contains(TrainingType.OTHER_ACTIVITY))
    }

    @Test
    fun `M02 – PREMIUM erlaubt alle Trainingstypen`() {
        billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        val allowed = billing.getAllowedTrainingTypes()
        assertEquals(TrainingType.entries.size, allowed.size)
    }

    @Test
    fun `M03 – PRO erlaubt alle Trainingstypen`() {
        billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        val allowed = billing.getAllowedTrainingTypes()
        assertEquals(TrainingType.entries.size, allowed.size)
    }

    @Test
    fun `M04 – nach Ablauf wieder nur FREE Trainingstypen`() {
        billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        billing.simulatePurchase(BillingManager.PREMIUM_MONTHLY, FakePurchaseState.SUB_EXPIRED)
        val allowed = billing.getAllowedTrainingTypes()
        assertEquals(3, allowed.size)
    }

    // ════════════════════════════════════════════════════════════
    // N) PENDING – kein Tier-Upgrade
    // ════════════════════════════════════════════════════════════

    @Test
    fun `N01 – PENDING liefert SimResult Pending`() {
        val result = billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.PENDING)
        assertEquals(SimResult.Pending, result)
    }

    @Test
    fun `N02 – PENDING veraendert Tier nicht`() {
        billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.PENDING)
        assertEquals(SubscriptionTier.FREE, billing.getCurrentTier())
    }

    @Test
    fun `N03 – PENDING danach SUB_ACTIVE setzt Tier korrekt`() {
        billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.PENDING)
        billing.simulatePurchase(BillingManager.PRO_MONTHLY, FakePurchaseState.SUB_ACTIVE)
        assertEquals(SubscriptionTier.PRO, billing.getCurrentTier())
    }

    // ════════════════════════════════════════════════════════════
    // O) PURCHASED_UNACKNOWLEDGED
    // ════════════════════════════════════════════════════════════

    @Test
    fun `O01 – PURCHASED_UNACKNOWLEDGED liefert PendingAcknowledgement`() {
        val result = billing.simulatePurchase(
            BillingManager.PREMIUM_MONTHLY, FakePurchaseState.PURCHASED_UNACKNOWLEDGED
        )
        assertTrue(result is SimResult.PendingAcknowledgement)
    }

    @Test
    fun `O02 – PURCHASED_UNACKNOWLEDGED setzt Tier trotzdem auf PREMIUM`() {
        billing.simulatePurchase(
            BillingManager.PREMIUM_MONTHLY, FakePurchaseState.PURCHASED_UNACKNOWLEDGED
        )
        assertEquals(SubscriptionTier.PREMIUM, billing.getCurrentTier())
    }

    @Test
    fun `O03 – PURCHASED_UNACKNOWLEDGED PRO setzt Tier auf PRO`() {
        val result = billing.simulatePurchase(
            BillingManager.PRO_LIFETIME, FakePurchaseState.PURCHASED_UNACKNOWLEDGED
        )
        assertTrue(result is SimResult.PendingAcknowledgement)
        assertEquals(SubscriptionTier.PRO, billing.getCurrentTier())
    }

    @Test
    fun `O04 – UNACKNOWLEDGED bleibt bei Neustart aktiv`() {
        billing.simulatePurchase(
            BillingManager.PREMIUM_MONTHLY, FakePurchaseState.PURCHASED_UNACKNOWLEDGED
        )
        val tierAfterRestart = billing.simulateAppRestart()
        assertEquals(SubscriptionTier.PREMIUM, tierAfterRestart)
    }
}
