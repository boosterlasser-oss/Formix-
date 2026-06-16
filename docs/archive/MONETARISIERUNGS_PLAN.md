# FORMIX Monetarisierungs-Plan

## 3-Stufen-Modell (Ohne Werbung!)

---

## 🆓 STUFE 1: KOSTENLOS (Free)

**Zielgruppe:** Einsteiger, die die App testen wollen

### Features:

**Training:**
- ✅ Manueller Trainingsplan (ohne KI)
- ✅ 3 Trainingstypen: BASICS, HOME, OTHER_ACTIVITY
- ✅ 10 Basis-Übungen mit Lottie-Animationen
- ✅ Einfaches Set-Tracking
- ✅ Wochenstatistik (letzte 7 Tage)
- ❌ CROSSFIT, STRENGTH (gesperrt)
- ❌ KI-Coach für Trainingsplanung
- ❌ Übungshistorie > 7 Tage

**Ernährung:**
- ✅ Manuelles Kalorien-Tracking
- ✅ 50 Basis-Rezepte
- ✅ Tagesübersicht
- ❌ KI-Coach für Ernährung
- ❌ Barcode-Scanner
- ❌ 500+ Rezepte
- ❌ Makro-Tracking (Protein, Carbs, Fat)

**Allgemein:**
- ✅ Basis-Dashboard
- ✅ Profil-Einstellungen
- ✅ Offline-Nutzung
- ❌ KI-Coach (komplett gesperrt)
- ❌ Coach Fly-In Animation
- ❌ Cloud-Backup
- ❌ Streak-System (nur 7 Tage sichtbar)

**Einschränkungen:**
- Max. 5 Workouts pro Woche speicherbar
- Max. 10 Mahlzeiten pro Tag
- Keine Datenexport-Funktion

---

## 💪 STUFE 2: PREMIUM (Mittlere Version)

**Preis:** 4,99€/Monat ODER 29,99€/Jahr (50% Ersparnis)

**Zielgruppe:** Aktive Sportler ohne KI-Bedarf

### Alles aus KOSTENLOS, plus:

**Training:**
- ✅ Alle 5 Trainingstypen (CROSSFIT, STRENGTH, BASICS, HOME, OTHER)
- ✅ Alle 41+ Übungen mit Animationen
- ✅ Vollständige Übungshistorie (unbegrenzt)
- ✅ Detaillierte Statistiken & Graphen
- ✅ Progressions-Tracking (Gewichtssteigerung)
- ✅ Workout-Templates erstellen & speichern
- ❌ KI-generierte Trainingspläne

**Ernährung:**
- ✅ Barcode-Scanner (OpenFoodFacts)
- ✅ Alle 500+ Rezepte
- ✅ Vollständiges Makro-Tracking
- ✅ Eigene Rezepte erstellen
- ✅ Wochenplaner für Mahlzeiten
- ❌ KI-Ernährungsberatung

**Allgemein:**
- ✅ Unbegrenzte Workouts & Mahlzeiten
- ✅ Vollständiges Streak-System
- ✅ Datenexport (CSV, JSON)
- ✅ Gewichtsverlauf-Graphen
- ✅ Ziel-Tracking mit Meilensteinen
- ❌ KI-Coach (weiterhin gesperrt)

---

## 👑 STUFE 3: PRO (Vollversion mit KI)

**Preis:** 9,99€/Monat ODER 59,99€/Jahr (50% Ersparnis)

**Alternativ: Lifetime 149,99€ (Einmalzahlung)**

**Zielgruppe:** Ambitionierte Sportler, die maximale Unterstützung wollen

### Alles aus PREMIUM, plus:

**KI-Coach (Das Herzstück!):**
- ✅ Vollständiger KI-Coach Zugang
- ✅ Coach Fly-In Animation beim Start
- ✅ KI-generierte personalisierte Trainingspläne
- ✅ KI-Ernährungsberatung
- ✅ Natürliche Spracheingabe ("Ich hab Fußball gespielt")
- ✅ Kontext-aware Coaching (weiß was du gerade machst)
- ✅ Live-Coach während dem Training
- ✅ Quick-Actions (Essen tracken per Sprache)
- ✅ Unbegrenzte KI-Nachrichten

**Exklusive Features:**
- ✅ Cloud-Backup & Sync
- ✅ Prioritäts-Support
- ✅ Früher Zugang zu neuen Features
- ✅ Keine Einschränkungen

---

## 💰 PREISÜBERSICHT

| Plan | Monatlich | Jährlich | Lifetime |
|------|-----------|----------|----------|
| **Free** | 0€ | 0€ | 0€ |
| **Premium** | 4,99€ | 29,99€ | 79,99€ |
| **Pro** | 9,99€ | 59,99€ | 149,99€ |

**Empfohlene Strategie:**
- Jährlich als "Beliebteste Wahl" markieren
- Lifetime für Early Adopters bewerben

---

## 🔧 TECHNISCHE UMSETZUNG

### 1. Feature-Flags System

**Neue Datei:** `app/src/main/java/com/fantasyfoodplanner/logic/SubscriptionManager.kt`

```kotlin
enum class SubscriptionTier {
    FREE,
    PREMIUM,
    PRO
}

object SubscriptionManager {
    private const val PREFS_NAME = "formix_subscription"
    
    fun getCurrentTier(ctx: Context): SubscriptionTier {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val tierName = prefs.getString("tier", "FREE") ?: "FREE"
        return SubscriptionTier.valueOf(tierName)
    }
    
    // Feature-Checks
    fun hasAiCoach(ctx: Context): Boolean = getCurrentTier(ctx) == SubscriptionTier.PRO
    fun hasBarcodeScanner(ctx: Context): Boolean = getCurrentTier(ctx) != SubscriptionTier.FREE
    fun hasAllRecipes(ctx: Context): Boolean = getCurrentTier(ctx) != SubscriptionTier.FREE
    fun hasAllTrainingTypes(ctx: Context): Boolean = getCurrentTier(ctx) != SubscriptionTier.FREE
    fun hasUnlimitedWorkouts(ctx: Context): Boolean = getCurrentTier(ctx) != SubscriptionTier.FREE
    fun hasCloudBackup(ctx: Context): Boolean = getCurrentTier(ctx) == SubscriptionTier.PRO
    fun hasFullStats(ctx: Context): Boolean = getCurrentTier(ctx) != SubscriptionTier.FREE
    
    // Workout-Limit für Free
    fun canSaveWorkout(ctx: Context): Boolean {
        if (getCurrentTier(ctx) != SubscriptionTier.FREE) return true
        
        // Free: Max 5 Workouts pro Woche
        val thisWeekCount = getWorkoutsThisWeek(ctx)
        return thisWeekCount < 5
    }
    
    // Für Play Store Billing
    fun setTier(ctx: Context, tier: SubscriptionTier) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("tier", tier.name)
            .apply()
    }
}
```

### 2. UI-Gates (Gesperrte Features anzeigen)

```kotlin
// Beispiel: KI-Coach Button
@Composable
fun CoachButton(ctx: Context, onClick: () -> Unit) {
    val isPro = SubscriptionManager.hasAiCoach(ctx)
    
    Box(
        modifier = Modifier
            .clickable {
                if (isPro) onClick()
                else showUpgradeDialog(ctx, "KI-Coach", SubscriptionTier.PRO)
            }
    ) {
        Row {
            Icon(Icons.Default.Psychology, "Coach")
            FText("KI-Coach")
            
            if (!isPro) {
                // Lock-Icon anzeigen
                Icon(
                    Icons.Default.Lock,
                    "Gesperrt - PRO benötigt",
                    tint = Color.Gray
                )
            }
        }
    }
}

// Upgrade-Dialog
@Composable
fun UpgradeDialog(
    feature: String,
    requiredTier: SubscriptionTier,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { FText("$feature freischalten", style = FantasyTypography.headlineMedium) },
        text = {
            Column {
                FText("Diese Funktion ist Teil von FORMIX ${requiredTier.name}.")
                Spacer(Modifier.height(8.dp))
                FText(
                    when (requiredTier) {
                        SubscriptionTier.PREMIUM -> "Ab 4,99€/Monat"
                        SubscriptionTier.PRO -> "Ab 9,99€/Monat"
                        else -> ""
                    },
                    color = FantasyColors.Accent
                )
            }
        },
        confirmButton = {
            FantasyButton("Upgraden") { onUpgrade() }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                FText("Später", color = Color.Gray)
            }
        }
    )
}
```

### 3. Google Play Billing Integration

**build.gradle.kts:**
```kotlin
dependencies {
    implementation("com.android.billingclient:billing-ktx:6.1.0")
}
```

**BillingManager.kt:**
```kotlin
class BillingManager(private val context: Context) {
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()
    
    // Product IDs (im Play Console definieren)
    companion object {
        const val PREMIUM_MONTHLY = "formix_premium_monthly"
        const val PREMIUM_YEARLY = "formix_premium_yearly"
        const val PREMIUM_LIFETIME = "formix_premium_lifetime"
        const val PRO_MONTHLY = "formix_pro_monthly"
        const val PRO_YEARLY = "formix_pro_yearly"
        const val PRO_LIFETIME = "formix_pro_lifetime"
    }
    
    fun launchPurchaseFlow(activity: Activity, productId: String) {
        // ... Google Play Billing Flow
    }
    
    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }
    
    private fun handlePurchase(purchase: Purchase) {
        // Abo-Tier setzen basierend auf Kauf
        val tier = when (purchase.products.first()) {
            PREMIUM_MONTHLY, PREMIUM_YEARLY, PREMIUM_LIFETIME -> SubscriptionTier.PREMIUM
            PRO_MONTHLY, PRO_YEARLY, PRO_LIFETIME -> SubscriptionTier.PRO
            else -> SubscriptionTier.FREE
        }
        SubscriptionManager.setTier(context, tier)
        
        // Purchase bestätigen
        acknowledgePurchase(purchase)
    }
}
```

---

## 📱 PLAY STORE SETUP

### 1. App-Varianten erstellen

**Option A: Eine App mit In-App-Käufen (EMPFOHLEN)**
- Eine APK/AAB für alle
- Subscriptions über Google Play Billing
- Einfacher zu verwalten

**Option B: Drei separate Apps**
- Formix Free (kostenlos)
- Formix Premium (4,99€)
- Formix Pro (9,99€)
- Mehr Aufwand, aber klarere Positionierung

### 2. Produkte im Play Console anlegen

```
Produkte → Abos → Neues Produkt:

1. formix_premium_monthly
   - Preis: 4,99€
   - Abrechnungszeitraum: Monatlich
   
2. formix_premium_yearly
   - Preis: 29,99€
   - Abrechnungszeitraum: Jährlich
   
3. formix_pro_monthly
   - Preis: 9,99€
   - Abrechnungszeitraum: Monatlich
   
4. formix_pro_yearly
   - Preis: 59,99€
   - Abrechnungszeitraum: Jährlich

Einmalzahlungen (In-App-Produkte):
5. formix_premium_lifetime - 79,99€
6. formix_pro_lifetime - 149,99€
```

### 3. Testphase

- **Interne Tests:** Eigene Geräte
- **Geschlossene Tests:** Freunde, Familie (20-100 Tester)
- **Offene Beta:** Öffentlich, aber als Beta markiert
- **Produktion:** Vollständiger Release

---

## 📊 MARKETING-STRATEGIE

### Free → Premium Conversion

**Trigger-Punkte für Upgrade-Hinweise:**

1. **5. Workout der Woche erreicht**
   → "Du trainierst fleißig! Mit Premium unbegrenzt weiter."

2. **Barcode scannen versucht**
   → "Barcode-Scanner ist ein Premium-Feature. Jetzt upgraden!"

3. **Gesperrten Trainingstyp antippen**
   → "CROSSFIT freischalten? Premium enthält alle Trainingstypen."

4. **Nach 2 Wochen aktiver Nutzung**
   → Push-Notification: "Du nutzt Formix regelmäßig! 50% auf Premium im ersten Monat."

### Premium → Pro Conversion

1. **KI-Coach Button antippen**
   → "Personalisierte Trainingspläne mit KI? Upgrade auf Pro!"

2. **"Ich bin müde" in Check-In schreiben**
   → "Mit Pro erstellt die KI automatisch einen angepassten Plan."

3. **Nach 1 Monat Premium**
   → "Du liebst Formix! Hole dir den vollen KI-Coach mit Pro."

---

## 📝 UMSETZUNGS-CHECKLISTE

### Phase 1: Feature-Gates (1-2 Tage)
- [ ] SubscriptionManager.kt erstellen
- [ ] Feature-Checks überall einbauen
- [ ] Upgrade-Dialog Komponente erstellen
- [ ] Lock-Icons bei gesperrten Features

### Phase 2: Paywall-Screen (1 Tag)
- [ ] Schöner Upgrade-Screen mit Preisvergleich
- [ ] Feature-Liste pro Tier
- [ ] "Beliebteste Wahl" Badge
- [ ] Testimonials (später)

### Phase 3: Google Play Billing (2-3 Tage)
- [ ] Billing-Library einbinden
- [ ] BillingManager.kt implementieren
- [ ] Purchase-Flow testen
- [ ] Subscription-Restore implementieren

### Phase 4: Play Console Setup (1 Tag)
- [ ] App signieren (Release-Keystore)
- [ ] Play Console Eintrag erstellen
- [ ] Screenshots & Beschreibung
- [ ] Produkte/Abos anlegen
- [ ] Interne Tests starten

### Phase 5: Testing & Launch
- [ ] Interne Tests (1 Woche)
- [ ] Geschlossene Beta (2 Wochen)
- [ ] Bugs fixen
- [ ] Produktion Release

---

## 💡 WICHTIGE HINWEISE

### Keine Werbung!
- ✅ Subscription-Modell ist nachhaltiger
- ✅ Bessere User Experience
- ✅ Höherer Lifetime Value pro User

### API-Kosten beachten!
- GPT-4o-mini kostet ca. 0,15$/1M Input Tokens
- Pro-User mit viel KI-Nutzung: ~0,50-2€/Monat Kosten
- Bei 9,99€/Monat: Gute Marge

### Rechtliches
- Impressum in App (Pflicht in DE)
- Datenschutzerklärung
- Widerrufsbelehrung für Abos
- AGB

---

## 🎯 ZIEL

**Launch-Datum:** [Datum eintragen]

**Ziele erstes Jahr:**
- 10.000 Downloads (Free)
- 500 Premium-Abonnenten (2.500€/Monat)
- 200 Pro-Abonnenten (2.000€/Monat)
- **Gesamt: ~4.500€/Monat**

Nach Abzug von:
- Google Play Gebühr (15-30%)
- API-Kosten (~200€)
- Server (falls Cloud-Backup): ~50€

**Netto: ~3.000-3.500€/Monat**

---

## NÄCHSTE SCHRITTE

1. **Zuerst:** Optimierungen abschließen (OPTIMIERUNGS_PROMPT.md)
2. **Dann:** Feature-Gates einbauen
3. **Dann:** Play Store Vorbereitung
4. **Dann:** Beta-Test
5. **Dann:** Launch! 🚀
