## 🚀 QUICK START: LOTTIE ANIMATIONEN HINZUFÜGEN

---

## 1️⃣ ASSETS ORDNER ERSTELLEN

```
app/src/main/assets/animations/
```

**Schritte in Android Studio:**
1. Öffne `Project` Panel (links)
2. Navigiere zu: `app → src → main → assets`
3. Erstelle neuen Ordner: `animations`
4. Fertig!

---

## 2️⃣ LOTTIE JSON DATEIEN HINZUFÜGEN

### Option A: Drag & Drop in Android Studio

```
1. Öffne animations/ Ordner in Android Studio
2. Ziehe deine .json Dateien rein
3. Oder: Rechtsklick → Paste JSON-Datei
4. Android Studio kompiliert automatisch
```

### Option B: Manuell in Explorer

```
1. Öffne Explorer: Y:\Desktop\Benutzer Entwickler\FantasyNutritionPlanner_FIX_ONLY\app\src\main\assets\
2. Erstelle Ordner "animations" (falls nicht vorhanden)
3. Kopiere .json Dateien rein
4. Führe ./gradlew build durch
```

---

## 3️⃣ NAMING-CONVENTION (WICHTIG!)

### Regel: Dateiname muss mit Übung matchen (normalisiert)

**Normalisierung:**
- Lowercase: `Bankdrücken` → `bankdruecken`
- Umlaute: `ä→ae`, `ö→oe`, `ü→ue`, `ß→ss`
- Leerzeichen/Bindestriche: `push up` → `push_up`

### Beispiel-Mapping:

| Übung | ✅ Dateiname funktioniert | ❌ Nicht erkannt |
|-------|----------------------|----------------|
| **Dips** | `dips.json` | `dip.json` |
| **Liegestütze** | `liegestuetze.json`<br/>`liegestütze.json`<br/>`push_up.json` | `push-up.json`<br/>`Pushup.json` |
| **Bankdrücken** | `bankdruecken.json`<br/>`bank_druck.json` | `bench_press.json` |
| **Kniebeugen** | `kniebeugen.json`<br/>`squat.json` | `knee_bend.json` |
| **Klimmzug** | `klimmzug.json`<br/>`pullup.json` | `chin_up.json` |
| **Plank** | `plank.json`<br/>`unterarmstütz.json` | `planks.json` |

---

## 4️⃣ JSON QUELLEN

### LottieFiles (empfohlen)
- Website: https://lottiefiles.com
- Suchbeispiel: "push up animation"
- Download: JSON format
- Format: Lottie JSON

### Figma
- Design Datei exportieren
- Lottie Plugin nutzen
- Export: Lottie JSON

### Nach Effects
- Animation erstellen
- BodyMovin Plugin exportieren
- Format: .json

---

## 5️⃣ JSON VALIDIEREN

### Vor dem Hinzufügen überprüfen:

```bash
# Option 1: Online Validator
https://lottiefiles.com/preview

# Option 2: Android Studio
- Datei öffnen
- Sollte valid JSON sein
- { ... } struktur
```

### Dateigröße prüfen:

```
✓ < 100KB: Optimal
✓ 100-500KB: OK
⚠ > 500KB: Langsam, überlegen ob optimierbar
```

---

## 6️⃣ BUILD UND TEST

### Build durchführen:

```bash
cd "Y:\Desktop\Benutzer Entwickler\FantasyNutritionPlanner_FIX_ONLY"
./gradlew.bat clean build
```

### App auf Device/Emulator testen:

```bash
./gradlew.bat installDebug
```

### In App testen:

1. App öffnen
2. Trainingsbereich → Übung anklicken (z.B. "Dips")
3. **Oben** sollte schwarzer Container mit Animation sein
4. Falls Animation fehlt → Fallback Text zeigt sich

---

## 7️⃣ DEBUG - LOGS PRÜFEN

### LogCat öffnen in Android Studio:

```
View → Tool Windows → Logcat
```

### Filtern nach "LottieAnimation":

```
D/LottieAnimation: Found X JSON animation files:
D/LottieAnimation:   - animations/dips.json
D/LottieAnimation:   - animations/liegestuetze.json
...
D/LottieAnimation: Matched exercise 'Dips' to animation: animations/dips.json
```

### Fehler-Logs prüfen:

```
E/LottieAnimation: Error loading animation: animations/dips.json
```

Falls Fehler → Siehe LOTTIE_IMPLEMENTATION_GUIDE.md Kapitel 7 "Häufige Fehler"

---

## 8️⃣ VOLLSTÄNDIGES BEISPIEL

### Szenario: "Dips" Animation hinzufügen

**Schritt 1: JSON Download**
- LottieFiles.com besuchen
- "dips" suchen
- JSON herunterladen: `dips.json`

**Schritt 2: In Assets legen**
```
Y:\Desktop\Benutzer Entwickler\FantasyNutritionPlanner_FIX_ONLY\
  → app\src\main\assets\animations\dips.json
```

**Schritt 3: Build**
```bash
./gradlew.bat clean build
```

**Schritt 4: Testen**
- App starten
- Trainingsbereich
- "Dips" anklicken
- Animation lädt & spielt ab 🎬

---

## 9️⃣ STRUKTUR NACH SETUP

```
app/src/main/assets/
├── animations/
│   ├── dips.json                    ← ✅ Wird erkannt für "Dips"
│   ├── liegestuetze.json            ← ✅ Wird erkannt für "Liegestütze"
│   ├── bankdruecken.json            ← ✅ Wird erkannt für "Bankdrücken"
│   ├── kniebeugen.json              ← ✅ Wird erkannt für "Kniebeugen"
│   ├── klimmzug.json                ← ✅ Wird erkannt für "Klimmzug"
│   ├── plank.json                   ← ✅ Wird erkannt für "Plank"
│   ├── kreuzheben.json              ← ✅ Wird erkannt für "Kreuzheben"
│   ├── schulterpresse.json          ← ✅ Wird erkannt für "Schulterpresse"
│   ├── latzug.json                  ← ✅ Wird erkannt für "Latzug"
│   ├── bizeps_curls.json            ← ✅ Wird erkannt für "Bizeps-Curls"
│   ├── trizepsdrücken.json          ← ✅ Wird erkannt für "Trizepsdrücken"
│   ├── beinpresse.json              ← ✅ Wird erkannt für "Beinpresse"
│   ├── ausfallschritte.json         ← ✅ Wird erkannt für "Ausfallschritte"
│   ├── seitheben.json               ← ✅ Wird erkannt für "Seitheben"
│   ├── burpees.json                 ← ✅ Wird erkannt für "Burpees"
│   ├── mountain_climbers.json       ← ✅ Wird erkannt für "Mountain Climbers"
│   ├── crunch.json                  ← ✅ Wird erkannt für "Crunch"
│   ├── glute_bridge.json            ← ✅ Wird erkannt für "Glute Bridge"
│   ├── thruster.json                ← ✅ Wird erkannt für "Thruster"
│   ├── beinstrecker.json            ← ✅ Wird erkannt für "Beinstrecker"
│   ├── beinbeuger.json              ← ✅ Wird erkannt für "Beinbeuger"
│   └── wadenheben.json              ← ✅ Wird erkannt für "Wadenheben"
│
└── models/
    ├── male.glb
    └── female.glb
```

---

## 🔟 TROUBLESHOOTING

### Problem: "Animation wird vorbereitet" Text zeigt sich

**Ursache:** JSON nicht gefunden oder Dateiname passt nicht

**Lösung:**
1. Überprüfe Dateiname (case insensitive, aber Umlaute wichtig!)
2. Datei liegt in `app/src/main/assets/animations/`?
3. `./gradlew clean build` erneut durchführen
4. Logs in Logcat prüfen

### Problem: Animation lädt ewig / Loading-Spinner dreht sich

**Ursache:** JSON zu groß oder Format invalid

**Lösung:**
1. JSON-Dateigröße < 100KB?
2. JSON valid? (JSON Validator online prüfen)
3. Andere Animation probieren
4. Device hat genug Memory?

### Problem: Animation spielt nicht / schwarzer Bildschirm

**Ursache:** Composition laden fehlgeschlagen

**Lösung:**
1. Logs prüfen: `E/LottieAnimation: Error loading...`
2. JSON-File erneut von LottieFiles laden
3. Ggf. andere Animation probieren
4. Device restart

---

## ✅ ERFOLGS-CHECKLIST

Nach dem Setup sollte:

```
☑ App kompiliert ohne Fehler
☑ Trainingsbereich öffnet ohne Crash
☑ Dips/Bankdrücken/etc. zeigen Animation oder Fallback
☑ Logcat zeigt: "Matched exercise 'XXX' to animation: animations/xxx.json"
☑ Schwarzer Container sichtbar (16:9 Aspect Ratio)
☑ Animation spielt in Loop ab (wenn vorhanden)
☑ Beim Übungswechsel wechselt Animation
☑ Keine Memory Leaks
☑ Kein Flackern/Stottern
```

---

**Status:** Ready for JSON imports ✅
**Next Step:** JSON Dateien in `app/src/main/assets/animations/` legen

