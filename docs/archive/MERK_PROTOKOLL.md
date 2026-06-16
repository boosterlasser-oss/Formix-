# FORMIX – Merk-Protokoll (Gedächtnis-Datei)
**Zweck:** Diese Datei am Anfang jeder neuen KI-Session vorzeigen – dann weiß die KI sofort alles.  
**Zuletzt aktualisiert:** 15.05.2026

---

## Wer bin ich?

**Kim Stefan Schäfer**  
Hauptstraße 57, 24994 Medelby  
boosterlaser@gmail.com  
Sprache: **NUR DEUTSCH**

---

## Was ist FORMIX?

Android Fitness & Nutrition App (Kotlin / Jetpack Compose)  
Ziel: **Veröffentlichung im Google Play Store**

---

## Technische Eckdaten – App

| Info | Wert |
|---|---|
| Projekt-Pfad | `D:\Entwicklung\Android\FORMIX` |
| Package (Release) | `com.fantasyfoodplanner.fix.v4` |
| Package (Debug) | `com.fantasyfoodplanner.fix.v4.debug` |
| Version | 3.3.0 / Build 19 |
| Min SDK | 26 |
| Target SDK | 34 |
| Sprache | Kotlin + Jetpack Compose |
| Datenbank | Room DB, Version 8, 10 Entities, 8 DAOs |
| KI | GPT-4o-mini + Local LLM Fallback |
| Gerät | Samsung SM-S908B (Android 16), ADB-ID: R3CT203W38T |

---

## Technische Eckdaten – Webseite

| Info | Wert |
|---|---|
| Netlify Site | `formix-app` |
| URL | **https://formix-app.netlify.app** |
| Site-ID | `48b0fca4-b463-437a-9545-375b1d056589` |
| Netlify Account | boosterlasser |
| Dateipfad | `D:\Entwicklung\Android\FORMIX\docs\index.html` |
| Typ | Einzelne HTML-Datei, kein Framework |
| App-Icon | `docs/icon.png` (aus mipmap-xxxhdpi) |

---

## Backups

| Backup | Pfad | Inhalt |
|---|---|---|
| Alt | `D:\Backups\FORMIX_Backup_20260407_1638` | früher Stand |
| Neu | `D:\Backups\FORMIX_Backup_20260503_1350` | mit Placeholder-Änderungen |
| Vorherig | `D:\Backups\FORMIX_Backup_2026_05_13_KOMPLETT` | Stand 13.05.2026 |
| Aktuell ✅ | `D:\Backups\FORMIX_Backup_2026_05_15_KOMPLETT` | Stand 15.05.2026 – Upgrade-Navigation fertig |

---

## Build & Deploy-Befehle

```powershell
# App bauen
powershell -Command "Set-Location 'D:\Entwicklung\Android\FORMIX'; & '.\gradlew.bat' assembleDebug 2>&1"

# App auf Gerät installieren
adb install -r "D:\Entwicklung\Android\FORMIX\app\build\outputs\apk\debug\app-debug.apk"

# Webseite deployen
netlify deploy --dir="D:\Entwicklung\Android\FORMIX\docs" --prod --site="48b0fca4-b463-437a-9545-375b1d056589"
```

---

## Abo-Modell (KEINE Werbung – nur Subscriptions)

| Plan | Monatlich | Jährlich | Lifetime |
|---|---|---|---|
| Free | 0 € | 0 € | 0 € |
| Premium | 4,99 € | 29,99 € | 79,99 € |
| Pro (mit KI) | 9,99 € | 59,99 € | 149,99 € |

**Free-Limits:** 3 Trainingstypen (BASICS, HOME, OTHER_ACTIVITY), 50 Rezepte, max. 5 Workouts/Woche, kein KI-Coach, kein Barcode-Scanner

---

## Produkt-IDs für Play Console

| Produkt-ID | Typ | Preis |
|---|---|---|
| `formix_premium_monthly` | Subscription | 4,99 €/Monat |
| `formix_premium_yearly` | Subscription | 29,99 €/Jahr |
| `formix_pro_monthly` | Subscription | 9,99 €/Monat |
| `formix_pro_yearly` | Subscription | 59,99 €/Jahr |
| `formix_premium_lifetime` | In-App Einmalkauf | 79,99 € |
| `formix_pro_lifetime` | In-App Einmalkauf | 149,99 € |

---

## Was wurde vollständig implementiert?

### APP – TEIL 1 – Code-Optimierungen ✅
- `Entities.kt` – DB-Indizes für alle 8 Entities
- `AppDb.kt` – Migration Version 7 → 8 (mit allen Index-Statements)
- `Dao.kt` – Date-Range-Queries + `countWorkoutsSince()`
- `SettingsManager.kt` – EncryptedSharedPreferences für API-Key + Migration alter Keys
- `FantasyKit.kt` – Accessibility (48dp Mindestgröße, Semantics), ErrorCard, FantasyTypography, FantasySpacing
- `DashboardViewModel.kt` – isLoading/error State, try/catch, clearError(), Range-Queries

### APP – TEIL 2 – Monetarisierung ✅
- `SubscriptionManager.kt` – NEU: SubscriptionTier enum (FREE/PREMIUM/PRO), alle Feature-Checks
- `BillingManager.kt` – NEU: Google Play Billing 6.2.0, 6 Produkte, queryExistingPurchases(), handlePurchase()
- `FantasyKit.kt` – FeatureGate(), LockedFeatureCard(), UpgradeDialog()
- `build.gradle.kts` – billing-ktx:6.2.0, security-crypto:1.1.0-alpha06
- `Dashboard.kt` – KI-Coach Gate + Error/Loading-Anzeige
- `TypeSelectionStep.kt` – Trainingstypen-Gate (CROSSFIT/STRENGTH gesperrt für FREE)
- `LiveScannerScreen.kt` – Barcode-Scanner Gate
- `WorkoutStep.kt` – Upgrade-Hinweis bei 5 Workouts/Woche (FREE)
- `MainActivity.kt` – SubscriptionManager.init(), BillingManager connect/disconnect
- `UpgradeScreen.kt` – NEU: Vollständiger Paywall-Screen, 6 Plan-Karten, Feature-Vergleich

### APP – TEIL 3 – Play Store Fertigstellung ✅
- `Profile.kt` – Abo-Status-Anzeige (farblich FREE/PREMIUM/PRO) + Upgrade-Button + Rechtliches-Button
- `LegalScreen.kt` – Vereinfacht: öffnet nur noch `https://formix-app.netlify.app` im Browser
- `MainActivity.kt` – Route `"legal"` registriert

### APP – TEIL 4 – Bug-Fixes & UX (13.05.2026) ✅
- `MainActivity.kt` – Coach Fly-In Animation entfernt
- `WorkoutStep.kt` – Bug-Fix: Leeres Training wird nicht mehr gezählt (`saveWorkoutToDb` gibt `Boolean` zurück, prüft `isDone`)
- `TrainingFlowScreen.kt` – Bug-Fix: Free-Limit-Prüfung jetzt VOR Trainingsstart (async via `canSaveWorkout()`)
- `TrainingFlowScreen.kt` – AlertDialog „Wochenlimit erreicht" mit Upgrade-Button

### APP – TEIL 5 – Upgrade-Navigation (15.05.2026) ✅
- `TrainingFlowScreen.kt` – `onGoUpgrade: () -> Unit` Parameter hinzugefügt
- Upgrade-Button im Limit-Dialog navigiert jetzt zu `UpgradeScreen` (`upgrade/PREMIUM`)
- `MainActivity.kt` – `onGoUpgrade = { nav.navigate("upgrade/PREMIUM") }` verdrahtet

### WEBSEITE – Vollständige Marketing-Webseite ✅
- Hero-Section mit Download-Button
- 9 Feature-Karten mit **Unsplash-Bildern** (keine Emojis mehr)
- How-it-Works (3 Schritte)
- Preise (3 Pläne: Free / Premium / Pro)
- Download-Section mit "Bald verfügbar"-Badge (SVG-Uhren-Icon)
- Footer mit Navigation
- Datenschutz + Impressum als Modal
- Hamburger-Menü (Dropdown, 3-Striche → X Animation, schließt nach Klick)
- Live auf Netlify: **https://formix-app.netlify.app**

---

## Webseite – Feature-Karten Unsplash-Bilder

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

## Wichtige Dateien / Struktur

```
D:\Entwicklung\Android\FORMIX\
├── docs/
│   ├── index.html          ✅ Webseite – Feature-Karten mit Unsplash-Bildern, Badge mit SVG-Icon
│   └── icon.png            ✅ App-Icon für Webseite
├── app\src\main\java\com\fantasyfoodplanner\
│   ├── MainActivity.kt                          ✅ Billing + Sub init, alle Routen, Coach-Animation entfernt
│   ├── data/
│   │   ├── Entities.kt                          ✅ DB-Indizes
│   │   ├── Dao.kt                               ✅ Range-Queries
│   │   └── AppDb.kt                             ✅ Version 8, Migration 7→8
│   ├── logic/
│   │   ├── SettingsManager.kt                   ✅ EncryptedSharedPreferences
│   │   ├── SubscriptionManager.kt               ✅ Feature-Checks
│   │   └── BillingManager.kt                    ✅ Google Play Billing
│   ├── ui/
│   │   └── FantasyKit.kt                        ✅ Gates, ErrorCard, Typography, Spacing
│   ├── viewmodel/
│   │   └── DashboardViewModel.kt                ✅ Loading/Error State
│   └── features/
│       ├── Dashboard.kt                         ✅ Gate + Error/Loading
│       ├── LiveScannerScreen.kt                 ✅ Scanner Gate
│       ├── Profile.kt                           ✅ Abo-Status + Upgrade + Legal Button
│       ├── UpgradeScreen.kt                     ✅ Paywall
│       ├── LegalScreen.kt                       ✅ Öffnet Webseite im Browser
│       └── fitness/training/
│           ├── TypeSelectionStep.kt             ✅ Trainingstypen Gate
│           └── WorkoutStep.kt                   ✅ Workout-Limit Gate + Bug-Fix Leer-Training
```

---

## Lottie-Animationen ⚠️ – Für später gemerkt, NICHT eingebaut

- 22 von 52 Übungs-Animationen fehlen oder sind fehlerhaft
- Placeholder aktiv: schwarzer 16:9 Container mit "Animation folgt in Update"
- Geänderte Dateien: `ExerciseDetailScreen.kt`, `LottieAnimationWindow.kt`
- **Fehlend/Fehlerhaft (22):** ab-wheel, bird-dog, box-jumps, dead-bug, floor-slides, skaters, step-ups, wandsitzen, beinstrecker, seitheben, flys, facepulls, arnoldpress, trizepsdips, bizepscurls, hackenschmidt, sumo-squats, pistol-squats, wall-sits, bulgarian-split-squats, goblet-squats, reverse-lunges
- **Quick Wins (3):** Crunches, Beinheben, Thruster (vorhanden, nur Mapping fehlt)
- **Nächster Schritt:** Animationen beschaffen → `LottieAnimationWindow` wieder aktivieren

---

## Bekannte Warnings (harmlos – kein Handlungsbedarf)

1. `onGoCrossFit` / `onGoAwakened` in `Dashboard.kt` – Parameter nie genutzt
2. `Divider` deprecated → sollte `HorizontalDivider` sein (in `UpgradeScreen.kt`)
3. `Icons.Default.ArrowForward` deprecated → `Icons.AutoMirrored.Filled.ArrowForward`
4. `ctx` in `UpgradeScreen.kt` unused

---

## Bekannter pre-existierender Bug (offen – vor Release beheben)

**Datei:** `UpgradeScreen.kt:135-138`  
Der Kaufen-Button ruft `launchBillingFlow` nicht auf – auskommentiert.  
Muss vor dem Production-Release im echten Billing-Flow-Test validiert werden.

---

## Was noch OFFEN ist (manuell durch Kim)

| # | Aufgabe | Wo |
|---|---|---|
| 1 | Release-AAB signieren | Android Studio → Build → Generate Signed Bundle |
| 2 | Keystore sicher aufbewahren! | Verlust = kein Update mehr möglich |
| 3 | 6 Produkte in Play Console anlegen | Produkt-IDs siehe oben |
| 4 | Store-Listing: Icon (512×512), Screenshots, Beschreibung | Play Console |
| 5 | Content-Rating-Fragebogen ausfüllen | Play Console |
| 6 | Billing im Testmodus testen + launchBillingFlow aktivieren | Play Console → License Testing |
| 7 | Lottie-Animationen beschaffen (22 fehlende) | Für späteres Update |

---

## Vollständiges Detailprotokoll

Alle technischen Details sind in:  
`D:\Entwicklung\Android\FORMIX\ENTWICKLUNGS_PROTOKOLL.md`
