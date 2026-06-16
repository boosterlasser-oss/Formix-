# Session-Protokoll – 16.06.2026

## FORMIX: Release-Vorbereitung (Hauptsession)

---

### 1. Statistik-Charts verbessert
- **TrainingVolumeChart:** Gradient-Balken, Grid-Linien, horizontal scrollbar (48dp/Punkt)
- **NutritionCalorieChart:** Flächenfüllung unter Kurve, geglättete Line, Punkte mit Outline, scrollbar
- Beide Charts: `horizontalScroll` + separate Label-Leiste

### 2. Statistik für FREE geöffnet
- Dashboard.kt: `onClick`-Gate entfernt → Statistik jetzt ohne Premium-Check

### 3. Release-Vorbereitung

| Aufgabe | Status |
|---------|--------|
| JDK 17 Toolchain-Fix (Gradle findet JDK automatisch) | ✅ `gradle.properties` – `org.gradle.java.installations.paths` |
| Toten Code entfernt (`onGoCrossFit`/`onGoAwakened`) | ✅ Dashboard.kt + MainActivity.kt |
| Alte .md-Dokumente archiviert (58 Dateien) | ✅ → `docs/archive/` |
| Leere States mit CTAs | ✅ Charts + Tabellen mit "Training starten"/"Mahlzeit planen"-Buttons |
| `.gitignore` erstellt | ✅ *.jks, local.properties, build/ |
| `.gitattributes` erstellt | ✅ Line-Endings: *.kt lf, *.bat crlf |
| Adaptive Icons | ✅ mipmap-anydpi-v26 + neues Foreground (grünes F) |
| Privacy Policy | ✅ `assets/privacy_policy.txt` + LegalScreen inline |
| Release-Build (Proguard/R8) | ✅ 63,5 MB (Debug 97 MB) |
| Keystore abgesichert | ✅ .gitignore |

### 4. GitHub Repository
- **Repo:** https://github.com/boosterlasser-oss/Formix-
- **Branch:** `main`
- **Commits:**
  - `5ee9744` – Initial commit: FORMIX v3.3.0 (411 Dateien)
  - `4d4504f` – Add CI/CD + .gitattributes
  - `483ea52` – Add store assets (screenshots, feature graphic, texts)
  - `ca3fdd9` – Add Firebase Crashlytics + fix appId

### 5. CI/CD (GitHub Actions)
- Workflow: `.github/workflows/build.yml`
- Automatischer `assembleDebug` bei jedem Push auf `main`
- APK als Artefakt downloadbar

### 6. Firebase Crashlytics
- **Projekt:** formix-b1396
- google-services.json hinzugefügt
- Firebase SDK + Crashlytics-Gradle-Plugin eingebaut
- App-Paketname: `com.fantasyfoodplanner` (debug-Suffix entfernt)
- Test erfolgreich: 2 Crashes in Console sichtbar

### 7. Store-Assets erstellt
- Feature Graphic (1024×500 PNG)
- 3 Screenshots
- Store-Texte DE + EN
- Generator-Script für Feature Graphic

### 8. Voll-Backup erstellt
- `FORMIX_backup_2026-06-16_163532/` (komplettes Projekt + Gradle-Cache)

---

## Geänderten Dateien (gesamt)

| Datei | Änderung |
|-------|---------|
| `.gitignore` | Neu – Keystore, Build, local.properties |
| `.gitattributes` | Neu – Line-Endings |
| `.github/workflows/build.yml` | Neu – CI/CD Pipeline |
| `app/build.gradle.kts` | Firebase Plugins + Crashlytics, appId fix, debug suffix entfernt |
| `app/google-services.json` | Neu – Firebase Konfiguration |
| `app/proguard-rules.pro` | Unverändert (funktioniert) |
| `app/src/main/AndroidManifest.xml` | Unverändert |
| `app/src/main/assets/privacy_policy.txt` | Neu |
| `app/src/main/java/.../Dashboard.kt` | Dead Code entfernt, Statistik FREE |
| `app/src/main/java/.../MainActivity.kt` | Dead Code + neue Callbacks |
| `app/src/main/java/.../LegalScreen.kt` | Privacy Policy inline |
| `app/src/main/java/.../StatsOverviewScreen.kt` | Charts scrollbar + Gradient, Empty States |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | Neu |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | Neu |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Neues Design (F + Dumbbell) |
| `build.gradle.kts` | Firebase Plugins |
| `gradle.properties` | JDK 17 Pfad |
| `docs/PRIVACY_POLICY.md` | Neu |
| `docs/STORE_TEXT_DE.md` | Neu |
| `docs/STORE_TEXT_EN.md` | Neu |
| `docs/feature_graphic.png` | Neu |
| `docs/generate_feature_graphic.py` | Neu |
| `docs/screenshot1-3.jpg` | Neu |
| `docs/SETUP_GUIDE.md` | Neu |
