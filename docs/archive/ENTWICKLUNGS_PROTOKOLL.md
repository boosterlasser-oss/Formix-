# FORMIX – Vollständiges Entwicklungsprotokoll
**Erstellt:** April 2026  
**Entwickler:** Kim Stefan Schäfer  
**App-Version:** 3.1.0 / Build 17  
**Package:** `com.fantasyfoodplanner.fix.v4`  
**Backup:** `D:\Backups\FORMIX_Backup_20260407_1638`

---

## Übersicht aller durchgeführten Arbeiten

### TEIL 1 – Code-Optimierungen
### TEIL 2 – Monetarisierung (Google Play Billing)
### TEIL 3 – Play Store Fertigstellung (Datenschutz, Impressum, Upgrade-UI)

Alle drei Teile wurden vollständig implementiert.  
Letzter Build: **BUILD SUCCESSFUL** – keine Errors, nur harmlose Warnings.

---

## TEIL 1 – Code-Optimierungen

### 1.1 Datenbank-Indizes (`Entities.kt`)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/data/Entities.kt`  
**Zweck:** Schnellere Datenbankabfragen durch Indizes auf häufig gefilterten Spalten.

| Entity | Neue Indizes |
|---|---|
| `WeightEntry` | `dateEpochDay` |
| `WorkoutEntry` | `dateEpochDay`, `type` |
| `ExerciseLog` | `dateEpochDay`, `exerciseName`, `(workoutType, dateEpochDay)` (Kombi) |
| `SetLog` | `exerciseLogId` (Foreign Key) |
| `MealEntry` | `dateEpochDay` |
| `ManualMealEntry` | `dateEpochDay` |
| `Recipe` | `name` |
| `Product` | `name` |
| `ScannedFoodEntity` | `code` (UNIQUE – bereits in Migration 6→7) |

**Vorher:** Keine Indizes → Vollständige Table-Scans bei jeder Abfrage  
**Nachher:** Index-gestützte Abfragen → deutlich schnellere Ladezeiten

---

### 1.2 Datenbank-Migration (`AppDb.kt`)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/data/AppDb.kt`  
**Änderung:** Version 7 → 8

```kotlin
// MIGRATION_7_8 erstellt alle Indizes für bestehende Datenbanken
private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_WeightEntry_dateEpochDay ON WeightEntry(dateEpochDay)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_WorkoutEntry_dateEpochDay ON WorkoutEntry(dateEpochDay)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_WorkoutEntry_type ON WorkoutEntry(type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_ExerciseLog_dateEpochDay ON ExerciseLog(dateEpochDay)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_ExerciseLog_exerciseName ON ExerciseLog(exerciseName)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_ExerciseLog_workoutType_dateEpochDay ON ExerciseLog(workoutType, dateEpochDay)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_SetLog_exerciseLogId ON SetLog(exerciseLogId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_MealEntry_dateEpochDay ON MealEntry(dateEpochDay)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_ManualMealEntry_dateEpochDay ON ManualMealEntry(dateEpochDay)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_Recipe_name ON Recipe(name)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_Product_name ON Product(name)")
    }
}
```

**Wichtig:** Bestehende App-Instanzen werden automatisch migriert – keine Datenverluste.

---

### 1.3 Neue DAO-Queries (`Dao.kt`)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/data/Dao.kt`

Neue Methoden:

| DAO | Methode | Zweck |
|---|---|---|
| `MealDao` | `getMealsForDate(date)` | Mahlzeiten für einen Tag |
| `MealDao` | `getMealsInRange(start, end)` | Mahlzeiten für einen Datumsbereich |
| `WorkoutDao` | `getWorkoutsForDate(date)` | Workouts für einen Tag |
| `WorkoutDao` | `getWorkoutsInRange(start, end)` | Workouts für einen Datumsbereich |
| `WorkoutDao` | `countWorkoutsSince(since)` | Anzahl Workouts seit Datum (für Free-Limit) |

---

### 1.4 Verschlüsselung API-Key (`SettingsManager.kt`)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/logic/SettingsManager.kt`

**Änderungen:**
- API-Key wird jetzt mit **Android EncryptedSharedPreferences (AES256-GCM)** gespeichert
- Automatische **Migration**: alter unverschlüsselter Key wird beim ersten Aufruf in verschlüsselte Prefs übertragen und aus normalen Prefs gelöscht
- Fallback auf normale Prefs wenn Gerät Verschlüsselung nicht unterstützt
- Preference-Key-Migration: `gemini_api_key` → `ai_api_key` und `gemini_enabled` → `ai_enabled`

```kotlin
private fun getSecurePrefs(ctx: Context): SharedPreferences {
    return try {
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            ctx, SECURE_PREFS_NAME, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        ctx.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
    }
}
```

---

### 1.5 Accessibility (`FantasyKit.kt`)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/ui/FantasyKit.kt`

**Änderungen am `FantasyButton`:**
- Mindestgröße 48dp × 48dp (`sizeIn(minWidth=48.dp, minHeight=48.dp)`) – Pflicht für Accessibility
- Semantik-Annotation: `semantics { role = Role.Button; contentDescription = label }`

**Neue Komponenten:**

| Komponente | Beschreibung |
|---|---|
| `ErrorCard` | Rote Fehlerkarte mit Retry- und Dismiss-Button |
| `FantasyTypography` | 8 einheitliche Text-Stile (displayLarge bis labelSmall) |
| `FantasySpacing` | Einheitliches Abstands-System: xs=4dp, s=8dp, m=16dp, l=24dp, xl=32dp, xxl=48dp |
| `FeatureGate` | Composable-Wrapper der Inhalt nur anzeigt wenn Feature freigeschaltet |
| `LockedFeatureCard` | Gesperrte Feature-Karte mit Schloss-Icon und Upgrade-Button |
| `UpgradeDialog` | Paywall-Dialog mit Tier-Beschreibung und „Jetzt upgraden"-Button |

---

### 1.6 DashboardViewModel (`DashboardViewModel.kt`)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/viewmodel/DashboardViewModel.kt`

**Änderungen:**
- `DashboardState` erhält `isLoading: Boolean` und `error: String?` Felder
- Alle Datenbankabfragen wrapped in `try/catch` mit Fehlerbehandlung
- `clearError()` Methode zum Zurücksetzen des Fehlerzustands
- Wochenabfragen nutzen jetzt `getMealsInRange()` statt mehrfacher Einzelabfragen

---

### 1.7 Dashboard-Fehleranzeige (`Dashboard.kt`)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/Dashboard.kt`

**Änderungen:**
- Zeigt `CircularProgressIndicator` während `isLoading == true`
- Zeigt `ErrorCard` wenn `error != null` mit Retry-Button

---

## TEIL 2 – Monetarisierung

### Abo-Modell

| Plan | Monatlich | Jährlich | Lifetime |
|------|-----------|----------|----------|
| **Free** | 0 € | 0 € | 0 € |
| **Premium** | 4,99 € | 29,99 € | 79,99 € |
| **Pro** (mit KI) | 9,99 € | 59,99 € | 149,99 € |

**Free-Limits:**
- Nur 3 Trainingstypen: BASICS, HOME, OTHER_ACTIVITY
- Max. 50 Rezepte
- Max. 5 Workouts pro Woche
- Kein KI-Coach
- Kein Barcode-Scanner

**Premium-Features:**
- Alle Trainingstypen (inkl. CROSSFIT, STRENGTH)
- Barcode-Scanner
- 500+ Rezepte
- Unbegrenzte Workouts

**Pro-Features (alles aus Premium plus):**
- Vollständiger KI-Coach (GPT-4o-mini)
- Cloud-Backup
- Priority Support

---

### 2.1 SubscriptionManager (`SubscriptionManager.kt`)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/logic/SubscriptionManager.kt`  
**Neu erstellt.**

```kotlin
enum class SubscriptionTier { FREE, PREMIUM, PRO }
```

**Feature-Check-Methoden:**

| Methode | FREE | PREMIUM | PRO |
|---|---|---|---|
| `hasAiCoach(ctx)` | false | false | true |
| `hasBarcodeScanner(ctx)` | false | true | true |
| `hasAllRecipes(ctx)` | false | true | true |
| `hasAllTrainingTypes(ctx)` | false | true | true |
| `hasCloudBackup(ctx)` | false | false | true |
| `hasFullStats(ctx)` | false | true | true |
| `hasUnlimitedWorkouts(ctx)` | false | true | true |
| `getMaxWorkoutsPerWeek(ctx)` | 5 | ∞ | ∞ |
| `getMaxRecipes(ctx)` | 50 | ∞ | ∞ |
| `canSaveWorkout(ctx, db)` | prüft Limit | true | true |
| `getAllowedTrainingTypes(ctx)` | BASICS, HOME, OTHER | alle | alle |

**Tier-Persistenz:** `SharedPreferences("formix_subscription")` → Key `"tier"` als String  
**ReactiveState:** `tierFlow: StateFlow<SubscriptionTier>` für Compose-Recomposition

---

### 2.2 BillingManager (`BillingManager.kt`)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/logic/BillingManager.kt`  
**Neu erstellt.**

**Produkt-IDs für die Play Console:**

| Produkt-ID | Typ | Preis |
|---|---|---|
| `formix_premium_monthly` | Subscription | 4,99 €/Monat |
| `formix_premium_yearly` | Subscription | 29,99 €/Jahr |
| `formix_pro_monthly` | Subscription | 9,99 €/Monat |
| `formix_pro_yearly` | Subscription | 59,99 €/Jahr |
| `formix_premium_lifetime` | In-App (Einmalkauf) | 79,99 € |
| `formix_pro_lifetime` | In-App (Einmalkauf) | 149,99 € |

**Funktionen:**
- `connect()` – Verbindung zu Google Play Billing herstellen
- `disconnect()` – Verbindung trennen (wird in `onDestroy()` aufgerufen)
- `queryProducts()` – Alle Produkte von Google Play laden
- `queryExistingPurchases()` – Beim App-Start aktive Käufe wiederherstellen
- `launchPurchase(activity, productDetails)` – Kaufdialog starten
- `handlePurchase(purchase)` – Kauf verarbeiten + Tier setzen + Kauf bestätigen

**Kauf-Bestätigung:** Pflicht innerhalb von 3 Tagen nach Kauf (sonst automatische Rückerstattung durch Google)

---

### 2.3 Feature-Gates in der App

#### KI-Coach Gate (`Dashboard.kt`)
```
Wenn FREE oder PREMIUM → UpgradeDialog mit PRO-Hinweis
Wenn PRO → KI-Coach öffnet sich
```

#### Trainingstypen-Gate (`TypeSelectionStep.kt`)
```
CROSSFIT, STRENGTH → nur PREMIUM/PRO
BASICS, HOME, OTHER_ACTIVITY → auch FREE
Gesperrte Typen zeigen LockedFeatureCard mit Upgrade-Button
```

#### Barcode-Scanner-Gate (`LiveScannerScreen.kt`)
```
Wenn FREE → UpgradeDialog mit PREMIUM-Hinweis
Wenn PREMIUM/PRO → Scanner öffnet sich
```

#### Workout-Limit (`WorkoutStep.kt`)
```
Nach dem Speichern: wenn FREE und 5 Workouts/Woche erreicht
→ AlertDialog mit Upgrade-Hinweis
```

---

### 2.4 UpgradeScreen (`UpgradeScreen.kt`)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/UpgradeScreen.kt`  
**Neu erstellt.**

**Enthält:**
- 6 Plan-Karten (Premium Monatlich/Jährlich/Lifetime, Pro Monatlich/Jährlich/Lifetime)
- „Beliebteste Wahl"-Badge auf Premium Jährlich
- Feature-Vergleichstabelle (Free / Premium / Pro)
- `PlanType`-Enum für alle 6 Kaufoptionen
- Billing-Integration über `BillingManager`
- Highlight-Parameter für automatisch vorausgewählten Plan

---

### 2.5 MainActivity – Initialisierung

**Datei:** `app/src/main/java/com/fantasyfoodplanner/MainActivity.kt`

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    SubscriptionManager.init(this)    // Tier aus SharedPreferences laden
    billingManager = BillingManager(this)
    billingManager.connect()          // Verbindung zu Google Play herstellen
    setContent { App() }
}

override fun onDestroy() {
    super.onDestroy()
    billingManager.disconnect()       // Verbindung sauber trennen
}
```

---

### 2.6 build.gradle.kts – Neue Dependencies

```kotlin
// Google Play Billing
implementation("com.android.billingclient:billing-ktx:6.2.0")

// Encrypted SharedPreferences (API-Key Sicherheit)
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

**Version:** `versionCode = 17` / `versionName = "3.1.0"`

---

## TEIL 3 – Play Store Fertigstellung

### 3.1 Upgrade-Button & Abo-Status im Profil-Screen (`Profile.kt`)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/Profile.kt`

**Neue Parameter:**
```kotlin
fun FullProfileScreen(
    onBack: () -> Unit,
    onGoSystemLog: () -> Unit,
    onGoUpgrade: () -> Unit = {},   // NEU
    onGoLegal: () -> Unit = {}      // NEU
)
```

**Neue Sektion „ABO & PREMIUM":**
- Zeigt aktuellen Plan farblich an:
  - FREE → Grau
  - PREMIUM → Lila (#8B5CF6)
  - PRO → Gold (#FFD700)
- FREE-User sehen: „Jetzt upgraden – PREMIUM / PRO" Button
- PREMIUM-User sehen: „Auf PRO upgraden (mit KI-Coach)" Button
- PRO-User sehen: „Du geniesst alle PRO-Features." (grüner Text)

**Neue Sektion „RECHTLICHES":**
- Button „Datenschutzerklärung & Impressum" → navigiert zu `LegalScreen`

---

### 3.2 LegalScreen (`LegalScreen.kt`)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/LegalScreen.kt`  
**Neu erstellt.**

**Tab 1 – Datenschutzerklärung (DSGVO-konform):**

| Abschnitt | Inhalt |
|---|---|
| 1. Verantwortlicher | Kim Stefan Schäfer, Hauptstraße 57, 24994 Medelby |
| 2. Welche Daten | Körperdaten, Training, Ernährung – nur lokal gespeichert |
| 3. KI-Coach (GPT) | Anfragen direkt an OpenAI, API-Key verschlüsselt lokal |
| 4. Google Play Billing | Zahlungsabwicklung über Google, nur anonyme Bestätigung |
| 5. Berechtigungen | Kamera (Scanner), Internet (KI/Billing), Speicher (Backup) |
| 6. Deine Rechte | Art. 15–21 DSGVO (Auskunft, Berichtigung, Löschung usw.) |
| 7. Datensicherheit | EncryptedSharedPreferences, kein Tracking, kein Analytics |
| 8. Kinder | Nicht für unter 16 Jahre |
| 9. Änderungen | Stand April 2026 |

**Tab 2 – Impressum (§ 5 TMG):**

| Abschnitt | Inhalt |
|---|---|
| Anbieter | Kim Stefan Schäfer, Hauptstraße 57, 24994 Medelby, boosterlaser@gmail.com |
| Verantwortlich für den Inhalt | Kim Stefan Schäfer, Hauptstraße 57, 24994 Medelby |
| Haftungsausschluss | Keine medizinische Beratung, keine Gewähr für Inhalte |
| Urheberrecht | Deutsches Urheberrecht |
| Streitschlichtung (EU) | Plattform-Link + keine Pflicht zur Teilnahme |

---

### 3.3 Neue Routen (`MainActivity.kt`)

```kotlin
// Upgrade-Screen (bereits vorher vorhanden, jetzt mit onGoUpgrade verknüpft)
composable("upgrade/{tier}") { ... }

// Legal-Screen (NEU)
composable("legal") {
    LegalScreen(onBack = { nav.popBackStack() })
}

// Profil-Screen (erweitert mit neuen Callbacks)
composable("profile") {
    FullProfileScreen(
        onBack = { nav.popBackStack() },
        onGoSystemLog = { nav.navigate("system_log") },
        onGoUpgrade = { nav.navigate("upgrade/PREMIUM") },  // NEU
        onGoLegal = { nav.navigate("legal") }               // NEU
    )
}
```

---

## Vollständige Dateiliste

### Neu erstellte Dateien

| Datei | Beschreibung |
|---|---|
| `logic/SubscriptionManager.kt` | SubscriptionTier enum + alle Feature-Checks |
| `logic/BillingManager.kt` | Google Play Billing Integration |
| `features/UpgradeScreen.kt` | Paywall-Screen mit allen Plänen |
| `features/LegalScreen.kt` | Datenschutzerklärung & Impressum |

### Bearbeitete Dateien

| Datei | Was wurde geändert |
|---|---|
| `data/Entities.kt` | DB-Indizes für alle Entities |
| `data/AppDb.kt` | Version 7→8, MIGRATION_7_8 |
| `data/Dao.kt` | Date-Range-Queries + countWorkoutsSince |
| `logic/SettingsManager.kt` | EncryptedSharedPreferences + Key-Migration |
| `ui/FantasyKit.kt` | Accessibility + Feature-Gates + ErrorCard + Typography + Spacing |
| `viewmodel/DashboardViewModel.kt` | isLoading/error State, Range-Queries, clearError() |
| `features/Dashboard.kt` | Error/Loading-Anzeige, KI-Coach Gate |
| `features/LiveScannerScreen.kt` | Barcode-Scanner Gate |
| `features/Profile.kt` | Neue Parameter + Abo-Status + Rechtliches Sektion |
| `features/fitness/training/TypeSelectionStep.kt` | Trainingstypen-Gate |
| `features/fitness/training/WorkoutStep.kt` | Upgrade-Hinweis bei 5 Workouts/Woche |
| `MainActivity.kt` | BillingManager init, SubscriptionManager init, neue Routen |
| `app/build.gradle.kts` | Billing + Security-Crypto Dependencies, Version 3.1.0 |

---

## Build-Status

```
Version:     3.1.0
Build-Code:  17
Build:       BUILD SUCCESSFUL
Errors:      0
Warnings:    4 (alle harmlos, kein Handlungsbedarf)
```

**Harmlose Warnings (zur Info):**
1. `onGoCrossFit` und `onGoAwakened` Parameter in `Dashboard.kt` werden nie genutzt
2. `Divider` deprecated → sollte `HorizontalDivider` sein (in `UpgradeScreen.kt`)
3. `Icons.Default.ArrowForward` deprecated → `Icons.AutoMirrored.Filled.ArrowForward`
4. `ctx` in `UpgradeScreen.kt` unused

---

## Noch ausstehend (manuell durch Entwickler)

### Vor dem Upload zwingend:

1. **Release-APK/AAB signieren**
   - Android Studio → Build → Generate Signed Bundle / APK
   - Keystore sicher aufbewahren (Verlust = kein Update mehr möglich!)

2. **Google Play Console – 6 Produkte anlegen**

   | Typ | Produkt-ID | Preis |
   |---|---|---|
   | Subscription | `formix_premium_monthly` | 4,99 €/Monat |
   | Subscription | `formix_premium_yearly` | 29,99 €/Jahr |
   | Subscription | `formix_pro_monthly` | 9,99 €/Monat |
   | Subscription | `formix_pro_yearly` | 59,99 €/Jahr |
   | In-App (Einmalkauf) | `formix_premium_lifetime` | 79,99 € |
   | In-App (Einmalkauf) | `formix_pro_lifetime` | 149,99 € |

3. **Datenschutz-URL in der Play Console**
   - Play Console verlangt eine externe URL (keine In-App-URL reicht)
   - Empfehlung: kostenlose GitHub Pages Seite mit dem Inhalt aus `LegalScreen.kt`

4. **Store-Listing vorbereiten**
   - App-Icon (512×512 px)
   - Feature-Grafik (1024×500 px)
   - Mind. 2 Screenshots pro Geräteklasse (Smartphone + Tablet)
   - Kurzbeschreibung (max. 80 Zeichen)
   - Vollständige Beschreibung (max. 4000 Zeichen)

5. **Content-Rating-Fragebogen** in der Play Console ausfüllen

6. **App auf echtem Gerät testen**
   - Nicht deinstallieren! (DB-Migration von Version 7→8 wird nur einmal ausgeführt)
   - Alle drei Tiers manuell testen (FREE / PREMIUM / PRO)
   - Billing im Testmodus über Play Console License Testing

---

---

## TEIL 4 – Billing-Simulationssystem (08.04.2026)

### Übersicht

Vollständiges Unit-Test-System zur Validierung der Zahlungslogik, ohne den echten produktiven Code zu verändern.

### Erstellte Dateien

| Datei | Zweck |
|---|---|
| `app/src/test/java/com/fantasyfoodplanner/logic/FakeBillingLayer.kt` | Simulationsebene: spiegelt BillingManager + SubscriptionManager ohne Google Play Kontext |
| `app/src/test/java/com/fantasyfoodplanner/logic/BillingSimulationTest.kt` | Vollständige Testmatrix: 69 Tests in 15 Gruppen |

### Testmatrix – Gruppen (69 Tests total)

| Gruppe | Beschreibung | Tests |
|---|---|---|
| A | Initialzustand und Reset | 2 |
| B | PREMIUM-Käufe (alle 3 Produkte × Zustände) | 6 |
| C | PRO-Käufe (alle 3 Produkte × Zustände) | 6 |
| D | Priorität: PRO > PREMIUM | 3 |
| E | Ablauf / Kündigung → Tier-Downgrade | 4 |
| F | Fehlerszenarien (FAILED, Disconnect, unbekannte ID) | 7 |
| G | App-Neustart (simulateAppRestart) | 4 |
| H | Feature-Gates FREE | 6 |
| I | Feature-Gates PREMIUM | 7 |
| J | Feature-Gates PRO | 6 |
| K | Workout-Limit (FREE: max 5/Woche) | 4 |
| L | Rezept-Limit (FREE: max 50) | 3 |
| M | Erlaubte Trainingstypen | 4 |
| N | PENDING-Kauf | 3 |
| O | PURCHASED_UNACKNOWLEDGED | 4 |

### Testergebnis

```
161 Tests gesamt – 0 Fehler – BUILD SUCCESSFUL
├── BillingSimulationTest:  69/69 bestanden ✅
├── OcrEngineTest:          23/23 bestanden ✅  (Bug behoben, s.u.)
├── ExerciseLogicTest:      alle bestanden ✅
├── AiLogicTest:            alle bestanden ✅
├── CheckInAnalyzerTest:    alle bestanden ✅
├── NutrientCalculatorTest: alle bestanden ✅
└── PerformanceTagEngineTest: alle bestanden ✅
```

### Entdeckter und behobener Bug: OcrEngine – Ziffern in Schlüsselwörtern

**Problem:** `OcrEngine.extractNumbers()` nutzte `.replace("g", " ")` um die Gramm-Einheit zu entfernen. Dadurch wurde `"eiwei6"` (OCR-Fehler für `"eiweiß"`) nach dem Lowercase-Cleaning zu `"eiwei "` + extrahierten Zahlen `[6, 8.0]`. Da die Regex `\d+` auch Ziffern innerhalb von Wörtern matchte, wurde `6` (aus `eiwei6`) als erste Zahl erkannt statt `8.0` (der echte Protein-Wert).

**Auswirkung:** Protein-Wert wurde als `6.0` statt `8.0` gespeichert bei OCR-gescannten Etiketten mit dem Buchstaben-Ziffer-Ersatz `"6"` für `"ß"`.

**Behobene Datei:** `app/src/main/java/com/fantasyfoodplanner/logic/OcrEngine.kt`

**Lösung 1 – Gramm-Einheit kontextbewusst entfernen:**
```kotlin
// Vorher:
.replace("g", " ")

// Nachher:
.replace(Regex("""(?<=[\d\s])g(?=\s|$)"""), " ")
```

**Lösung 2 – Zahlen-Regex nur außerhalb von Wörtern matchen:**
```kotlin
// Vorher:
val regex = Regex("""(\d{1,3}(?:\.\d{3})+(?:,\d+)?|\d+,\d+|\d+\.\d+|\d+)""")

// Nachher:
val regex = Regex("""(?<![a-zäöüß])(\d{1,3}(?:\.\d{3})+(?:,\d+)?|\d+,\d+|\d+\.\d+|\d+)""")
```

Der Negative Lookbehind `(?<![a-zäöüß])` verhindert, dass Ziffern am Ende von Wörtern (wie das `6` in `eiwei6`) als eigenständige Zahlen erkannt werden.

### Bekannter pre-existierender Bug (nicht behoben – UpgradeScreen)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/UpgradeScreen.kt:135-138`

Der Kaufen-Button ruft `launchBillingFlow` nicht auf – die Zeile ist auskommentiert:
```kotlin
// billingManager.launchPurchase(...)
```
**Status:** Bewusst offen gelassen – muss vor dem Production-Release im echten Billing-Flow-Test validiert werden.

---

## Kontakt / Verantwortlicher

**Kim Stefan Schäfer**  
Hauptstraße 57  
24994 Medelby  
boosterlaser@gmail.com

---

## TEIL 4 – Bug-Fixes & UX (13.05.2026)

**Session:** 13.05.2026  
**Build:** assembleDebug – BUILD SUCCESSFUL (1m 15s)  
**Installiert auf:** Samsung SM-S908B (R3CT203W38T), Android 16

---

### 4.1 Coach Fly-In Animation entfernt (`MainActivity.kt`)

**Problem:** Beim App-Start wurde eine Coach-Fly-In-Animation abgespielt, die als störend empfunden wurde.

**Lösung:**
- `showCoachFlyIn` State und zugehörige Logik aus `MainActivity.kt` entfernt
- Splash-Screen geht nun direkt zur App ohne Animation
- `CoachFlyInAnimation.kt` ist noch vorhanden, wird aber nicht mehr aufgerufen

---

### 4.2 Bug-Fix: Leeres Training wird als Trainingseinheit gezählt (`WorkoutStep.kt`)

**Problem:** `saveWorkoutToDb` speicherte einen `WorkoutEntry` in die Datenbank, egal ob der Nutzer irgendeinen Satz abgehakt hatte oder nicht. Dadurch wurde jedes gestartete Training als abgeschlossene Einheit gezählt.

**Analyse:**
- Funktion `saveWorkoutToDb` in `WorkoutStep.kt` (Zeile 623+)
- `isDone` auf `SetState`-Objekten zeigt ob ein Set abgehakt wurde
- `plan.savedSetStates[index]` enthält die Set-Zustände je Übungs-Index

**Lösung – `saveWorkoutToDb` (Zeile 623):**

Rückgabetyp von `Unit` auf `Boolean` geändert. Vor dem Insert wird geprüft, ob mindestens ein Set `isDone == true` ist:

```kotlin
internal suspend fun saveWorkoutToDb(db: AppDb, plan: WorkoutPlan, type: String): Boolean {
    val anySetDone = plan.exercises.indices.any { index ->
        val block = plan.exercises[index]
        if (block.type != "ex") return@any false
        val states = plan.savedSetStates[index] ?: emptyList()
        states.any { it.isDone }
    }
    if (!anySetDone) return false   // Nichts abgehakt → nicht speichern

    val today = LocalDate.now().toEpochDay()
    db.workoutDao().insert(WorkoutEntry(dateEpochDay = today, type = type))
    // ... Rest unverändert ...
    return true
}
```

**Beide Aufrufer** (Zeile ~205 und ~228) wurden bereits in einer früheren Session angepasst:
```kotlin
val saved = saveWorkoutToDb(db, plan, selectedType.name)
if (!saved) {
    launch(Dispatchers.Main) { showNothingDoneDialog = true }
    return@launch
}
```

**Dialog:** `showNothingDoneDialog` AlertDialog (bereits in früherer Session hinzugefügt, Zeile ~248) zeigt dem Nutzer einen Hinweis, dass kein Set abgehakt wurde.

**Ergebnis:** Training wird nur gespeichert wenn mindestens ein Satz wirklich abgehakt wurde.

---

### 4.3 LegalScreen vereinfacht (`LegalScreen.kt`)

**Vorher:** LegalScreen zeigte Datenschutz + Impressum als In-App-Text (230 Zeilen, Tabs).  
**Nachher:** LegalScreen zeigt nur noch einen Button, der `https://formix-app.netlify.app` im externen Browser öffnet.  
**Grund:** Kein doppelter Inhalt in App und Webseite. Webseite ist die Single Source of Truth.

---

---

## TEIL 5 – Marketing-Webseite & Webseiten-Verbesserungen (13.05.2026)

**Session:** 13.05.2026  
**Netlify-URL:** https://formix-app.netlify.app  
**Deploy:** netlify deploy --prod – alle Deploys erfolgreich

---

### 5.1 Vollständige Marketing-Webseite erstellt (`docs/index.html`)

Einzelne HTML-Datei ohne Framework. Enthält:

| Section | Inhalt |
|---|---|
| Navigation | Logo, Links (Features, Preise, Download), Hamburger-Menü (Mobile) |
| Hero | Headline, Subtext, Download-Button (deaktiviert – bald verfügbar) |
| Features | 9 Feature-Karten |
| How it Works | 3-Schritte-Erklärung |
| Preise | 3 Pläne (Free / Premium / Pro) mit Jahres/Monats-Toggle |
| Download | "Bald verfügbar"-Badge + Google Play Button (deaktiviert) |
| Footer | Navigation, Rechtliches-Links |
| Modals | Datenschutzerklärung + Impressum als eingebettete Modals |

---

### 5.2 Hamburger-Menü gefixt

- Dropdown öffnet sich von oben nach unten
- 3-Striche → X Animation beim Öffnen/Schließen
- Menü schließt sich automatisch nach Klick auf einen Link

---

### 5.3 Feature-Karten: Emojis durch Unsplash-Bilder ersetzt

**Vorher:** 9 Karten mit großen Emoji-Icons (🏋️📊🥗🤖📷⚡🎯🏃🔒)  
**Nachher:** 9 Karten mit echten Fotos von Unsplash

Neue Kartenstruktur:
```html
<div class="feature-card">
  <div class="feature-img-wrap">
    <img class="feature-img" src="https://images.unsplash.com/photo-XXXXX?w=600&q=80&fit=crop" alt="..." loading="lazy" />
  </div>
  <div class="feature-body">
    <h3>Titel</h3>
    <p>Beschreibung</p>
    <span class="feature-tag tag-free">FREE</span>
  </div>
</div>
```

Neue CSS-Klassen (bereits im `<style>`-Block vorhanden):
- `.feature-img` – Bild füllt Karte, Hover-Zoom-Effekt
- `.feature-img-wrap` – Bild-Container mit Overlay
- `.feature-body` – Text-Bereich unterhalb des Bildes

| Feature | Unsplash-ID |
|---|---|
| Strukturierte Trainingspläne | `photo-1534438327276-14e5300c3a48` |
| Fortschritts-Tracking | `photo-1571019613454-1cb2f99b2d8b` |
| Kalorientracker | `photo-1490645935967-10de6ba17061` |
| KI-Coach | `photo-1677442135703-1787eea5ce01` |
| Barcode-Scanner | `photo-1607082348824-0a96f2a4b9da` |
| Offline-First | `photo-1512941937669-90a1b58e7e9c` |
| Intelligente Kalibrierung | `photo-1517836357463-d25dfeac3438` |
| Andere Sportarten | `photo-1571008887538-b36bb32f4571` |
| Datenschutz pur | `photo-1614064641938-3bbee52942c7` |

---

### 5.4 "Bald verfügbar" Badge – Rakete durch SVG-Icon ersetzt

**Vorher:** `🚀 Bald verfügbar` (Emoji-Platzhalter)  
**Nachher:** SVG-Uhren-Icon + Text `Bald verfügbar`

```html
<div class="coming-soon-badge">
  <svg ...Uhr-SVG...>...</svg>
  Bald verfügbar
</div>
```

---

## TEIL 6 – Upgrade-Navigation aus Limit-Dialog

### 6.1 Upgrade-Button verdrahtet (`TrainingFlowScreen.kt`, `MainActivity.kt`)

**Problem:** Der „Auf Premium upgraden"-Button im `showWorkoutLimitReachedDialog` schloss nur den Dialog, navigierte aber nicht zum UpgradeScreen.

**Lösung:**
- `TrainingFlowScreen` erhält neuen Parameter `onGoUpgrade: () -> Unit = {}`
- Confirm-Button ruft jetzt `onGoUpgrade()` nach `showWorkoutLimitReachedDialog = false`
- `MainActivity.kt`: `onGoUpgrade = { nav.navigate("upgrade/PREMIUM") }` übergeben

**Betroffene Dateien:**
- `features/fitness/TrainingFlowScreen.kt`
- `MainActivity.kt`

**Build:** BUILD SUCCESSFUL – installiert auf Samsung SM-S908B ✅

---

## Aktueller Gesamt-Stand (13.05.2026)

```
App-Version:    3.3.0 / Build 19
Build-Status:   BUILD SUCCESSFUL
Installiert:    Samsung SM-S908B (Android 16) via ADB
Webseite:       https://formix-app.netlify.app – live ✅
Backup:         D:\Backups\FORMIX_Backup_2026_05_13_KOMPLETT ✅
```
