# FORMIX PROMPT TEIL 2: Monetarisierung & Play Store

## Projekt-Info
- **Pfad:** `D:\Entwicklung\Android\FORMIX`
- **Package:** `com.fantasyfoodplanner.fix.v4.debug` → Für Release: `com.formix.app`
- **Voraussetzung:** PROMPT_TEIL1 abgeschlossen (DB v8, Indizes, Accessibility)

## Deine Aufgabe

Implementiere das 3-Stufen-Abo-Modell und bereite Play Store Launch vor.

---

## 1. SUBSCRIPTIONMANAGER.KT ERSTELLEN

**Neue Datei:** `app/src/main/java/com/fantasyfoodplanner/logic/SubscriptionManager.kt`

```kotlin
package com.fantasyfoodplanner.logic

import android.content.Context
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

enum class SubscriptionTier { FREE, PREMIUM, PRO }

object SubscriptionManager {
    private const val PREFS = "formix_subscription"
    private val _tierFlow = MutableStateFlow(SubscriptionTier.FREE)
    val tierFlow: StateFlow<SubscriptionTier> = _tierFlow
    
    fun init(ctx: Context) {
        _tierFlow.value = getCurrentTier(ctx)
    }
    
    fun getCurrentTier(ctx: Context): SubscriptionTier {
        val name = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("tier", "FREE") ?: "FREE"
        return try { SubscriptionTier.valueOf(name) } catch (e: Exception) { SubscriptionTier.FREE }
    }
    
    fun setTier(ctx: Context, tier: SubscriptionTier) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("tier", tier.name).apply()
        _tierFlow.value = tier
    }
    
    // Feature-Checks
    fun hasAiCoach(ctx: Context) = getCurrentTier(ctx) == SubscriptionTier.PRO
    fun hasBarcodeScanner(ctx: Context) = getCurrentTier(ctx) != SubscriptionTier.FREE
    fun hasAllRecipes(ctx: Context) = getCurrentTier(ctx) != SubscriptionTier.FREE
    fun hasAllTrainingTypes(ctx: Context) = getCurrentTier(ctx) != SubscriptionTier.FREE
    fun hasCloudBackup(ctx: Context) = getCurrentTier(ctx) == SubscriptionTier.PRO
    fun hasFullStats(ctx: Context) = getCurrentTier(ctx) != SubscriptionTier.FREE
    fun hasUnlimitedWorkouts(ctx: Context) = getCurrentTier(ctx) != SubscriptionTier.FREE
    
    // Free-Limits
    fun getMaxWorkoutsPerWeek(ctx: Context) = if (getCurrentTier(ctx) == SubscriptionTier.FREE) 5 else Int.MAX_VALUE
    fun getMaxRecipes(ctx: Context) = if (getCurrentTier(ctx) == SubscriptionTier.FREE) 50 else Int.MAX_VALUE
    
    // Workout-Counter für Free
    fun canSaveWorkout(ctx: Context, db: AppDb): Boolean {
        if (getCurrentTier(ctx) != SubscriptionTier.FREE) return true
        val weekStart = LocalDate.now().minusDays(7).toEpochDay()
        val count = db.workoutDao().countWorkoutsSince(weekStart)
        return count < 5
    }
}
```

---

## 2. DAO ERWEITERN (für Free-Limit)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/data/Dao.kt`

Hinzufügen in WorkoutDao:

```kotlin
@Query("SELECT COUNT(*) FROM WorkoutEntry WHERE dateEpochDay >= :since")
suspend fun countWorkoutsSince(since: Long): Int
```

---

## 3. FEATURE-GATE KOMPONENTE

**Datei:** `app/src/main/java/com/fantasyfoodplanner/ui/FantasyKit.kt`

Am Ende der Datei hinzufügen:

```kotlin
// Feature-Gate Komponente
@Composable
fun FeatureGate(
    ctx: Context,
    requiredTier: SubscriptionTier,
    featureName: String,
    content: @Composable () -> Unit
) {
    val currentTier = SubscriptionManager.getCurrentTier(ctx)
    val hasAccess = when (requiredTier) {
        SubscriptionTier.FREE -> true
        SubscriptionTier.PREMIUM -> currentTier != SubscriptionTier.FREE
        SubscriptionTier.PRO -> currentTier == SubscriptionTier.PRO
    }
    
    if (hasAccess) {
        content()
    } else {
        LockedFeatureCard(featureName, requiredTier)
    }
}

@Composable
fun LockedFeatureCard(featureName: String, requiredTier: SubscriptionTier) {
    var showUpgrade by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showUpgrade = true }
            .alpha(0.6f),
        colors = CardDefaults.cardColors(containerColor = FantasyColors.Surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, "Gesperrt", tint = FantasyColors.TextSecondary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                FText(featureName, style = FantasyTypography.bodyLarge)
                FText(
                    "${requiredTier.name} erforderlich",
                    style = FantasyTypography.bodySmall,
                    color = FantasyColors.TextSecondary
                )
            }
            Icon(Icons.Default.ArrowForward, "Upgraden", tint = FantasyColors.Accent)
        }
    }
    
    if (showUpgrade) {
        UpgradeDialog(requiredTier) { showUpgrade = false }
    }
}

@Composable
fun UpgradeDialog(requiredTier: SubscriptionTier, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FantasyColors.Surface,
        title = { FText("Upgrade auf ${requiredTier.name}", style = FantasyTypography.headlineSmall) },
        text = {
            Column {
                FText(
                    when (requiredTier) {
                        SubscriptionTier.PREMIUM -> "Alle Trainingstypen, Barcode-Scanner, 500+ Rezepte und mehr!"
                        SubscriptionTier.PRO -> "Vollständiger KI-Coach, Cloud-Backup, Priority Support!"
                        else -> ""
                    }
                )
                Spacer(Modifier.height(12.dp))
                FText(
                    when (requiredTier) {
                        SubscriptionTier.PREMIUM -> "Ab 4,99 EUR/Monat oder 29,99 EUR/Jahr"
                        SubscriptionTier.PRO -> "Ab 9,99 EUR/Monat oder 59,99 EUR/Jahr"
                        else -> ""
                    },
                    color = FantasyColors.Accent,
                    style = FantasyTypography.titleMedium
                )
            }
        },
        confirmButton = {
            FantasyButton("Jetzt upgraden") {
                // TODO: BillingManager.launchPurchase(...)
                onDismiss()
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                FText("Spaeter", color = FantasyColors.TextSecondary)
            }
        }
    )
}
```

---

## 4. BILLING DEPENDENCY

**Datei:** `app/build.gradle.kts`

In dependencies Block:

```kotlin
implementation("com.android.billingclient:billing-ktx:6.2.0")
```

---

## 5. BILLINGMANAGER.KT ERSTELLEN

**Neue Datei:** `app/src/main/java/com/fantasyfoodplanner/logic/BillingManager.kt`

```kotlin
package com.fantasyfoodplanner.logic

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class BillingManager(private val context: Context) : PurchasesUpdatedListener {
    
    companion object {
        // Subscriptions
        const val PREMIUM_MONTHLY = "formix_premium_monthly"
        const val PREMIUM_YEARLY = "formix_premium_yearly"
        const val PRO_MONTHLY = "formix_pro_monthly"
        const val PRO_YEARLY = "formix_pro_yearly"
        // Lifetime (In-App Products)
        const val PREMIUM_LIFETIME = "formix_premium_lifetime"
        const val PRO_LIFETIME = "formix_pro_lifetime"
    }
    
    val products = MutableStateFlow<List<ProductDetails>>(emptyList())
    
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()
    
    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    queryPurchases()
                }
            }
            override fun onBillingServiceDisconnected() { /* Retry-Logic */ }
        })
    }
    
    private fun queryProducts() {
        val subParams = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PREMIUM_MONTHLY).setProductType(BillingClient.ProductType.SUBS).build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PREMIUM_YEARLY).setProductType(BillingClient.ProductType.SUBS).build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRO_MONTHLY).setProductType(BillingClient.ProductType.SUBS).build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRO_YEARLY).setProductType(BillingClient.ProductType.SUBS).build(),
            )).build()
        
        billingClient.queryProductDetailsAsync(subParams) { _, details ->
            products.value = details
        }
    }
    
    private fun queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { _, purchases ->
            purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                .forEach { handlePurchase(it) }
        }
    }
    
    fun launchPurchase(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )).build()
        billingClient.launchBillingFlow(activity, params)
    }
    
    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        }
    }
    
    private fun handlePurchase(purchase: Purchase) {
        val tier = when {
            purchase.products.any { it.contains("pro") } -> SubscriptionTier.PRO
            purchase.products.any { it.contains("premium") } -> SubscriptionTier.PREMIUM
            else -> SubscriptionTier.FREE
        }
        SubscriptionManager.setTier(context, tier)
        
        // Acknowledge
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken).build()
            billingClient.acknowledgePurchase(params) { }
        }
    }
}
```

---

## 6. FEATURE-GATES EINBAUEN

### KI-Coach (Dashboard.kt / CoachChatSheet.kt)

```kotlin
// Vor Coach-Button
if (SubscriptionManager.hasAiCoach(ctx)) {
    CoachFlyIn(...)  // Bestehendes KI-Feature
} else {
    LockedFeatureCard("KI-Coach", SubscriptionTier.PRO)
}
```

### Trainingstypen (WorkoutScreen)

```kotlin
val allowedTypes = if (SubscriptionManager.hasAllTrainingTypes(ctx)) {
    WorkoutType.entries
} else {
    listOf(WorkoutType.BASICS, WorkoutType.HOME, WorkoutType.OTHER_ACTIVITY)
}
```

### Barcode-Scanner

```kotlin
if (SubscriptionManager.hasBarcodeScanner(ctx)) {
    BarcodeScanner(...)
} else {
    LockedFeatureCard("Barcode-Scanner", SubscriptionTier.PREMIUM)
}
```

---

## 7. PLAY STORE CHECKLISTE

### Play Console Produkte anlegen:

| Produkt-ID | Typ | Preis |
|------------|-----|-------|
| formix_premium_monthly | Abo | 4,99 EUR |
| formix_premium_yearly | Abo | 29,99 EUR |
| formix_pro_monthly | Abo | 9,99 EUR |
| formix_pro_yearly | Abo | 59,99 EUR |
| formix_premium_lifetime | In-App | 79,99 EUR |
| formix_pro_lifetime | In-App | 149,99 EUR |

### App-Signierung (Du machst das selbst):
1. Android Studio: Build > Generate Signed Bundle/APK
2. Neuen Keystore erstellen (sicher aufbewahren!)
3. AAB (Android App Bundle) waehlen, nicht APK

### Play Store Eintrag:

**App-Name:** FORMIX - Fitness & Ernaehrung

**Kurzbeschreibung (80 Zeichen):**
```
Dein KI-Coach fuer Training, Ernaehrung und Gewichtskontrolle
```

**Lange Beschreibung:**
```
FORMIX ist dein persoenlicher Fitness-Begleiter mit KI-Coach!

TRAINING
- 5 Trainingstypen: CrossFit, Kraft, Basics, Home, Aktivitaeten
- 40+ Uebungen mit Animationen
- Progressions-Tracking
- Workout-Statistiken

ERNAEHRUNG
- 500+ gesunde Rezepte
- Kalorien- und Makro-Tracking
- Barcode-Scanner
- Mahlzeitenplaner

KI-COACH (Pro)
- Personalisierte Trainingsplaene
- Ernaehrungsberatung
- Sprachsteuerung
- Adaptives Coaching

KOSTENLOS STARTEN
Die Basis-Version ist komplett kostenlos. Upgrade auf Premium oder Pro fuer alle Features.

Premium: 4,99 EUR/Monat
Pro mit KI: 9,99 EUR/Monat
```

### Screenshots benoetigt:
1. Dashboard mit Statistiken
2. Workout-Screen mit Animation
3. Rezept-Ansicht
4. KI-Coach Chat
5. Fortschritts-Graphen

---

## 8. LAUNCH-SCHRITTE

1. **Intern testen** - Eigenes Geraet, alle Features pruefen
2. **Play Console** - App-Eintrag erstellen, Infos ausfuellen
3. **Produkte anlegen** - Alle 6 Abo/In-App Produkte
4. **AAB hochladen** - Signiertes Bundle
5. **Interner Test** - 1-2 Wochen, Billing testen
6. **Geschlossene Beta** - Optional, 10-50 Tester
7. **Produktion** - Release!

---

## Nach Abschluss

- App vollstaendig getestet mit allen Abo-Tiers
- Play Store Eintrag live
- Billing funktioniert
- Fertig zum Launch!
