# Setup-Guide: GitHub + Firebase + Play Store

## 1. GitHub Repository

**Erstellt:** https://github.com/boosterlasser-oss/Formix-

### CI/CD (GitHub Actions)
- Bei jedem Push auf `main` wird automatisch `assembleDebug` gebaut
- Die APK liegt als Download-Artefakt im Actions-Tab
- Workflow-Datei: `.github/workflows/build.yml`

### Wenn du auf einem neuen PC entwickelst:
```bash
git clone https://github.com/boosterlasser-oss/Formix-.git
cd Formix-
# local.properties anpassen (SDK-Pfad, Keystore)
```

---

## 2. Firebase Crashlytics

**Projekt:** https://console.firebase.google.com/project/formix-b1396/overview

### Setup (bereits erledigt im Code):
1. `google-services.json` liegt in `app/google-services.json`
2. Firebase SDK + Crashlytics sind in `build.gradle.kts` eingebaut
3. Der App-Paketname ist `com.fantasyfoodplanner` (Release + Debug)

### Crashlytics prüfen:
1. Release-APK auf Handy installieren
2. App nutzen → bei Abstürzen → nach 1-2 Minuten in der Console sichtbar
3. Console: https://console.firebase.google.com/project/formix-b1396/crashlytics

### Wenn `google-services.json` neu geladen werden muss:
1. Firebase Console → Projekteinstellungen → Allgemein → Deine Apps
2. `google-services.json` downloaden → nach `app/google-services.json` kopieren

---

## 3. Play Store Veröffentlichung

### Benötigte Dateien:
| Datei | Pfad |
|-------|------|
| Release-APK | `app/build/outputs/apk/release/app-release.apk` |
| Feature Graphic | `docs/feature_graphic.png` (1024×500) |
| Screenshots | `docs/screenshot1-3.jpg` |
| Beschreibung DE | `docs/STORE_TEXT_DE.md` |
| Beschreibung EN | `docs/STORE_TEXT_EN.md` |

### Schritte in der Play Console:
1. **App erstellen** → Namen: "FORMIX – Fitness & Ernährung"
2. **Produktion → Release erstellen**
3. APK hochladen (`app-release.apk`)
4. Store-Eintrag ausfüllen (Texte aus `STORE_TEXT_DE.md` + `STORE_TEXT_EN.md`)
5. Grafiken: Feature Graphic + Screenshots hochladen
6. Categorisierung: **Gesundheit & Fitness**
7. Alterseinstufung ausfüllen
8. Privacy Policy: In der App enthalten (Profil → Rechtliches)
9. Veröffentlichen

### Release-Keys:
- Keystore: `formix_release.jks` (im Projekt-Root, NICHT in Git)
- Keystore-Passwort: in `local.properties`
- **Wichtig:** Keystore sicher extern backupen! Ohne ihn kannst du keine Updates veröffentlichen.

---

## 4. Build-Befehle

```bash
# Debug-APK (zum Testen)
./gradlew assembleDebug

# Release-APK (für Store)
./gradlew assembleRelease

# Beide gleichzeitig
./gradlew assembleDebug assembleRelease
```

### APK-Ausgabe:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

---

## 5. Wichtige Hinweise

- **JDK 17** ist Pflicht (in `gradle.properties` konfiguriert)
- **Android SDK** muss in `local.properties` gesetzt sein
- **Keystore** nie in Git committen! (`.gitignore` schützt `*.jks`)
- Bei Problemen: `./gradlew assembleDebug --stacktrace` für Details
