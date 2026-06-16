# 🎯 Universal Lottie Builder - TEIL 5: Umsetzungsplan für 14 Übungen

**Datum:** 01.05.2026  
**Zweck:** Konkrete Strategie & Rezepte für alle 14 fehlenden Übungen  
**Status:** PLANUNGSDOKUMENT - FINAL

---

## 🎯 ZIEL DIESES DOKUMENTS

**Schritt-für-Schritt-Anleitungen** für jede der 14 fehlenden Übungen - welche Transformationen, welche Parameter, welche Erfolgsaussichten.

---

## 📊 ÜBERSICHT: 14 FEHLENDE ÜBUNGEN

### ✅ MIT TOOL MACHBAR (8 Übungen):
1. **Box Jumps** - ⭐⭐⭐ SEHR GUT machbar (90%)
2. **Wandsitzen** - ⭐⭐⭐ SEHR GUT machbar (85%)
3. **Step-ups** - ⭐⭐⭐ GUT machbar (80%)
4. **Ab-Wheel** - ⭐⭐ GUT machbar (75%)
5. **Skaters** - ⭐⭐ MITTEL machbar (60%)
6. **Floor Slides** - ⭐⭐ MITTEL machbar (55%)
7. **Dead Bug** - ⭐ SCHWIERIG (50%)
8. **Bird Dog** - ⭐ SCHWIERIG (50%)

### 🌐 BESSER DOWNLOADEN (6 Übungen):
9. **Beinstrecker** - ❌ Zu komplex (Maschinen-Animation)
10. **Seitheben** - ❌ Arm-Winkel komplett anders
11. **Diamond Pushups** - ❌ Hand-Position präzise ändern schwierig
12. **Hampelmänner** - ❌ Komplexe synchrone Arme+Beine
13. **Handtuch-Latzug** - ❌ Equipment entfernen komplex
14. **Wadenheben** - ❌ Isolierte Fuß-Bewegung schwierig

---

## 🏗️ REZEPTE: 8 MACHBARE ÜBUNGEN

---

### 1️⃣ BOX JUMPS (Erfolg: 90% ⭐⭐⭐)

**Basis-Animation:** `Burpees.json`  
**Grund:** Hat perfekte Jump-Phase (Frames 141-164)

#### Transformations-Schritte:
```bash
# Schritt 1: Jump-Phase extrahieren
python lottie_builder.py extract "Burpees.json" "box-jumps-raw.json" --start 141 --end 164

# Schritt 2: Geschwindigkeit erhöhen (explosiver Sprung)
python lottie_builder.py speed "box-jumps-raw.json" "box-jumps.json" --multiplier 1.2

# Optional: Spiegeln für Variante
python lottie_builder.py mirror "box-jumps.json" "box-jumps-mirrored.json" --axis horizontal
```

#### Parameter-Details:
```python
# Schritt 1: extract_frames
{
    "source": "Burpees.json",
    "start_frame": 141,  # Jump startet
    "end_frame": 164     # Landung endet
}
# Ergebnis: 23 Frames (0.96s bei 24 FPS)

# Schritt 2: speed
{
    "multiplier": 1.2    # 20% schneller = explosiver
}
# Ergebnis: 0.8s Duration
```

#### LottieAnimationProvider.kt Mapping:
```kotlin
"Box Jumps" to listOf("box", "jumps", "jump", "plyometric")
```

#### Qualität: ⭐⭐⭐⭐⭐ (90%)
- ✅ Bewegung ist perfekt (Original Jump aus Burpees)
- ✅ Natürliche Animation
- ⚠️ Evtl. zu kurz (kann mit Loop gelöst werden)

---

### 2️⃣ WANDSITZEN / WALL SIT (Erfolg: 85% ⭐⭐⭐)

**Basis-Animation:** `Kniebeugen.json`  
**Grund:** Hat perfekte Squat-Position in unterer Phase

#### Transformations-Schritte:
```bash
# Schritt 1: Untere Position extrahieren (tiefster Punkt)
python lottie_builder.py extract "Kniebeugen.json" "wandsitzen-raw.json" --start 60 --end 90

# Schritt 2: Sehr langsam machen (fast statisch)
python lottie_builder.py speed "wandsitzen-raw.json" "wandsitzen.json" --multiplier 0.1

# Optional: Rotation (leicht nach hinten = an Wand)
python lottie_builder.py rotate "wandsitzen.json" "wandsitzen-final.json" --degrees -10
```

#### Parameter-Details:
```python
# Schritt 1: extract_frames
{
    "source": "Kniebeugen.json",
    "start_frame": 60,   # Tiefster Punkt
    "end_frame": 90      # Stabilisierte Position
}
# Ergebnis: 30 Frames

# Schritt 2: speed
{
    "multiplier": 0.1    # 10% = extrem langsam, quasi statisch
}
# Ergebnis: ~10s Duration (perfekt für TIME_BASED Übung)

# Schritt 3: rotate (optional)
{
    "degrees": -10       # Leicht zurück = Wand-Anlehnung
}
```

#### LottieAnimationProvider.kt Mapping:
```kotlin
"Wandsitzen" to listOf("wandsitzen", "wall", "sit", "squat", "static", "isometric")
```

#### Qualität: ⭐⭐⭐⭐ (85%)
- ✅ Position ist perfekt (Squat-Haltung)
- ✅ Langzeitanimation möglich
- ⚠️ Minimal beweglich (nicht 100% statisch, aber gut genug)

---

### 3️⃣ STEP-UPS (Erfolg: 80% ⭐⭐⭐)

**Basis-Animation:** `Ausfallschritt.json` ODER `woman-doing-left-leg-forward-lunge-exercise-for-legs.json`  
**Grund:** Bein-Hebe-Bewegung ähnlich

#### Transformations-Schritte:
```bash
# Schritt 1: Ausgangsposition bis höchster Punkt
python lottie_builder.py extract "Ausfallschritt.json" "step-ups-raw.json" --start 0 --end 60

# Schritt 2: Bein höher (Scale Y erhöhen für mehr Höhe)
# ODER: Einfach so lassen (Ausfallschritt ≈ Step-up)

# Schritt 3: Geschwindigkeit anpassen
python lottie_builder.py speed "step-ups-raw.json" "step-ups.json" --multiplier 0.9

# Optional: Beide Beine (mirror für linkes Bein)
python lottie_builder.py mirror "step-ups.json" "step-ups-left.json" --axis horizontal
```

#### Parameter-Details:
```python
# Schritt 1: extract_frames
{
    "source": "Ausfallschritt.json",
    "start_frame": 0,
    "end_frame": 60      # Bis Bein oben
}

# Schritt 2: speed
{
    "multiplier": 0.9    # Etwas langsamer = kontrollierter
}
```

#### LottieAnimationProvider.kt Mapping:
```kotlin
"Step-ups" to listOf("step", "ups", "stepup", "lunge", "leg", "raise")
```

#### Qualität: ⭐⭐⭐⭐ (80%)
- ✅ Bewegung sehr ähnlich
- ⚠️ Keine Box sichtbar (aber Bewegung korrekt)
- ✅ Gut verwendbar

---

### 4️⃣ AB-WHEEL (Erfolg: 75% ⭐⭐)

**Basis-Animation:** `Plank.json` + `Liegestütz.json` kombiniert  
**Grund:** Plank-Position + Vor/Zurück-Bewegung

#### Transformations-Schritte:
```bash
# Schritt 1: Plank als Basis
# Schritt 2: Liegestütz vorwärts/rückwärts Bewegung extrahieren
python lottie_builder.py extract "Liegestütz.json" "pushup-movement.json" --start 0 --end 40

# Schritt 3: Kombinieren (Overlay)
python lottie_builder.py combine "Plank.json" "pushup-movement.json" "ab-wheel.json" --mode overlay

# Optional: Rotation anpassen (Körper mehr horizontal)
python lottie_builder.py rotate "ab-wheel.json" "ab-wheel-final.json" --degrees 5
```

#### Parameter-Details:
```python
# Schritt 1: extract_frames (Liegestütz)
{
    "source": "Liegestütz.json",
    "start_frame": 0,
    "end_frame": 40      # Runter-Bewegung
}

# Schritt 2: combine
{
    "data1": "Plank.json",
    "data2": "pushup-movement.json",
    "mode": "overlay"
}
```

#### LottieAnimationProvider.kt Mapping:
```kotlin
"Ab-Wheel" to listOf("ab", "wheel", "rollout", "plank", "core")
```

#### Qualität: ⭐⭐⭐ (75%)
- ✅ Bewegungsrichtung korrekt (vor/zurück)
- ⚠️ Kein Rad sichtbar (nur Bewegung)
- ✅ Akzeptabel für Core-Übung

---

### 5️⃣ SKATERS (Erfolg: 60% ⭐⭐)

**Basis-Animation:** `Ausfallschritt.json` gespiegelt + kombiniert  
**Grund:** Seitliche Bein-Bewegung

#### Transformations-Schritte:
```bash
# Schritt 1: Ausfallschritt extrahieren
python lottie_builder.py extract "Ausfallschritt.json" "skater-right.json" --start 0 --end 60

# Schritt 2: Spiegeln für linke Seite
python lottie_builder.py mirror "skater-right.json" "skater-left.json" --axis horizontal

# Schritt 3: Sequence kombinieren (rechts → links → rechts)
python lottie_builder.py combine "skater-right.json" "skater-left.json" "skaters-raw.json" --mode sequence

# Schritt 4: Schneller (explosiver)
python lottie_builder.py speed "skaters-raw.json" "skaters.json" --multiplier 1.5

# Optional: Rotation (mehr seitlich)
python lottie_builder.py rotate "skaters.json" "skaters-final.json" --degrees 15
```

#### Parameter-Details:
```python
# Schritt 4: speed
{
    "multiplier": 1.5    # 50% schneller = explosiv
}

# Schritt 5: rotate
{
    "degrees": 15        # Seitliche Perspektive
}
```

#### LottieAnimationProvider.kt Mapping:
```kotlin
"Skaters" to listOf("skater", "skaters", "lateral", "side", "cardio")
```

#### Qualität: ⭐⭐⭐ (60%)
- ⚠️ Bewegung ähnlich, aber nicht perfekt
- ⚠️ Fehlende seitliche Dynamik
- ✅ Als Cardio-Übung erkennbar

---

### 6️⃣ FLOOR SLIDES (Erfolg: 55% ⭐⭐)

**Basis-Animation:** `Superman.json` umgekehrt  
**Grund:** Rücken-Lage + Arm-Bewegung

#### Transformations-Schritte:
```bash
# Schritt 1: Superman umkehren (Rückenlage statt Bauchlage)
python lottie_builder.py mirror "Superman.json" "floor-slides-raw.json" --axis vertical

# Schritt 2: Rotation (auf Rücken drehen)
python lottie_builder.py rotate "floor-slides-raw.json" "floor-slides.json" --degrees 180

# Optional: Geschwindigkeit
python lottie_builder.py speed "floor-slides.json" "floor-slides-final.json" --multiplier 0.8
```

#### Parameter-Details:
```python
# Schritt 1: mirror
{
    "axis": "vertical"   # Oben/Unten spiegeln
}

# Schritt 2: rotate
{
    "degrees": 180       # Komplett umdrehen
}
```

#### LottieAnimationProvider.kt Mapping:
```kotlin
"Floor Slides" to listOf("floor", "slides", "back", "slide", "shoulder")
```

#### Qualität: ⭐⭐ (55%)
- ⚠️ Nicht perfekt, aber verwendbar
- ⚠️ Arm-Winkel nicht ideal
- ✅ Rückenlage korrekt

---

### 7️⃣ DEAD BUG (Erfolg: 50% ⭐)

**Basis-Animation:** `russian-twist.json` modifiziert  
**Grund:** Rückenlage + Bein-Arm-Koordination

#### Transformations-Schritte:
```bash
# Schritt 1: Russian Twist als Basis (Rückenlage)
# Schritt 2: Rotation anpassen (weniger Twist)
python lottie_builder.py rotate "russian-twist.json" "dead-bug-raw.json" --degrees 0

# Schritt 3: Geschwindigkeit verlangsamen
python lottie_builder.py speed "dead-bug-raw.json" "dead-bug.json" --multiplier 0.7

# Optional: Spiegeln für Varianz
python lottie_builder.py mirror "dead-bug.json" "dead-bug-alt.json" --axis horizontal
```

#### LottieAnimationProvider.kt Mapping:
```kotlin
"Dead Bug" to listOf("dead", "bug", "deadbug", "core", "lying")
```

#### Qualität: ⭐⭐ (50%)
- ⚠️ Nicht perfekt (Russian Twist ≠ Dead Bug)
- ⚠️ Bein-Arm-Koordination anders
- ✅ Als Core-Übung erkennbar

**Alternative:** Besser downloaden!

---

### 8️⃣ BIRD DOG (Erfolg: 50% ⭐)

**Basis-Animation:** `Plank.json` + Position-Anpassung  
**Grund:** Plank-ähnliche Position + Arm/Bein heben

#### Transformations-Schritte:
```bash
# Schritt 1: Plank als Basis
# Schritt 2: Layer isolieren (nur relevante Körperteile)
# (Manuell: Arme/Beine identifizieren via analyze)
python lottie_builder.py isolate "Plank.json" "bird-dog.json" --layers 1,3,5,7

# Optional: Geschwindigkeit
python lottie_builder.py speed "bird-dog.json" "bird-dog-final.json" --multiplier 0.8
```

#### LottieAnimationProvider.kt Mapping:
```kotlin
"Bird Dog" to listOf("bird", "dog", "birddog", "balance", "core", "plank")
```

#### Qualität: ⭐⭐ (50%)
- ⚠️ Sehr schwierig umzusetzen
- ⚠️ Arm/Bein-Heben fehlt
- ✅ Position ähnlich

**Alternative:** Besser downloaden!

---

## 🌐 DOWNLOAD-STRATEGIE: 6 SCHWIERIGE ÜBUNGEN

Für diese 6 Übungen ist **Download empfohlen**, da Transformationen zu komplex/unrealistisch:

### 9️⃣ BEINSTRECKER (Leg Extension)
**Problem:** Maschinen-Bewegung, Sitzposition, isolierte Bein-Streckung  
**Download-Quelle:** LottieFiles.com, suche "leg extension exercise"  
**Alternative:** Bestehende `Beinbeuger.json` verwenden (ähnliche Maschine)

---

### 🔟 SEITHEBEN (Lateral Raise)
**Problem:** Arm-Winkel 90° zur Seite (komplett anders als andere Übungen)  
**Download-Quelle:** LottieFiles.com, suche "lateral raise shoulder"  
**Alternative:** `Schulterpresse.json` verwenden (nicht perfekt, aber Schulter-Übung)

---

### 1️⃣1️⃣ DIAMOND PUSHUPS
**Problem:** Hand-Position präzise ändern (Shape-Pfade modifizieren)  
**Download-Quelle:** LottieFiles.com, suche "diamond push up"  
**Alternative:** `Liegestütz.json` verwenden + Mapping anpassen ("diamond" → "push", "ups")

---

### 1️⃣2️⃣ HAMPELMÄNNER (Jumping Jacks)
**Problem:** Synchrone Arm+Bein-Bewegung, komplexe Koordination  
**Download-Quelle:** LottieFiles.com, suche "jumping jacks exercise"  
**Alternative:** `Burpees.json` verwenden (auch Cardio)

---

### 1️⃣3️⃣ HANDTUCH-LATZUG (Towel Pull)
**Problem:** Equipment (Handtuch) hinzufügen, Pull-Bewegung anpassen  
**Download-Quelle:** LottieFiles.com, suche "towel pull exercise"  
**Alternative:** `Latzug.json` verwenden + Synonym-Mapping ("handtuch" → "latzug", "pull")

---

### 1️⃣4️⃣ WADENHEBEN (Calf Raise)
**Problem:** Isolierte Fuß-Bewegung, Rest des Körpers statisch  
**Download-Quelle:** LottieFiles.com, suche "calf raise exercise"  
**Alternative:** `Kniebeugen.json` verwenden (auch Bein-Übung)

---

## 📋 IMPLEMENTIERUNGS-REIHENFOLGE (Priorisierung)

### PHASE 1: Quick Wins (Höchste Erfolgsrate) ✅
1. **Box Jumps** (90%) - CROSSFIT, hohe Priorität
2. **Wandsitzen** (85%) - Knie-Probleme Alternative
3. **Step-ups** (80%) - Bodyweight Alternative

**Aufwand:** ~1-2 Stunden  
**Impact:** 3 wichtige Übungen sofort verfügbar

---

### PHASE 2: Gute Kandidaten ✅
4. **Ab-Wheel** (75%) - STRENGTH Core
5. **Skaters** (60%) - Cardio Alternative

**Aufwand:** ~2-3 Stunden  
**Impact:** 2 weitere Übungen

---

### PHASE 3: Experimental (Niedrige Erfolgsrate) ⚠️
6. **Floor Slides** (55%)
7. **Dead Bug** (50%)
8. **Bird Dog** (50%)

**Aufwand:** ~3-4 Stunden  
**Impact:** 3 Übungen, aber Qualität fraglich  
**Empfehlung:** Erst testen, evtl. Downloads bevorzugen

---

### PHASE 4: Downloads (Schneller & besser) 🌐
9-14. **Alle 6 schwierigen Übungen**

**Aufwand:** ~1-2 Stunden (Suche + Integration)  
**Impact:** 6 Übungen mit hoher Qualität  
**Empfehlung:** PARALLEL zu Phase 1-3 durchführen

---

## 🔧 AUTOMATISIERUNGS-SCRIPT

### Batch-Build für alle 8 machbaren Übungen:

```bash
#!/bin/bash
# build_all_exercises.sh

# Phase 1: Quick Wins
echo "Building Box Jumps..."
python lottie_builder.py extract "Burpees.json" "box-jumps-raw.json" --start 141 --end 164
python lottie_builder.py speed "box-jumps-raw.json" "box-jumps.json" --multiplier 1.2

echo "Building Wandsitzen..."
python lottie_builder.py extract "Kniebeugen.json" "wandsitzen-raw.json" --start 60 --end 90
python lottie_builder.py speed "wandsitzen-raw.json" "wandsitzen.json" --multiplier 0.1

echo "Building Step-ups..."
python lottie_builder.py extract "Ausfallschritt.json" "step-ups-raw.json" --start 0 --end 60
python lottie_builder.py speed "step-ups-raw.json" "step-ups.json" --multiplier 0.9

# Phase 2: Gute Kandidaten
echo "Building Ab-Wheel..."
python lottie_builder.py extract "Liegestütz.json" "pushup-movement.json" --start 0 --end 40
python lottie_builder.py combine "Plank.json" "pushup-movement.json" "ab-wheel.json" --mode overlay

echo "Building Skaters..."
python lottie_builder.py extract "Ausfallschritt.json" "skater-right.json" --start 0 --end 60
python lottie_builder.py mirror "skater-right.json" "skater-left.json" --axis horizontal
python lottie_builder.py combine "skater-right.json" "skater-left.json" "skaters-raw.json" --mode sequence
python lottie_builder.py speed "skaters-raw.json" "skaters.json" --multiplier 1.5

# Phase 3: Experimental
echo "Building Floor Slides..."
python lottie_builder.py mirror "superman-exercise.json" "floor-slides-raw.json" --axis vertical
python lottie_builder.py rotate "floor-slides-raw.json" "floor-slides.json" --degrees 180

echo "Building Dead Bug..."
python lottie_builder.py speed "russian-twist.json" "dead-bug.json" --multiplier 0.7

echo "Building Bird Dog..."
python lottie_builder.py speed "Plank.json" "bird-dog.json" --multiplier 0.8

echo "✅ All exercises built!"
```

### Python Batch-Build Alternative:

```python
# batch_build.py
from lottie_builder import extract_frames, speed, mirror, combine

EXERCISES = [
    {
        "name": "Box Jumps",
        "steps": [
            ("extract", "Burpees.json", {"start": 141, "end": 164}),
            ("speed", None, {"multiplier": 1.2})
        ]
    },
    {
        "name": "Wandsitzen",
        "steps": [
            ("extract", "Kniebeugen.json", {"start": 60, "end": 90}),
            ("speed", None, {"multiplier": 0.1})
        ]
    },
    # ... weitere Übungen
]

for exercise in EXERCISES:
    print(f"Building {exercise['name']}...")
    
    result = None
    for action, source, params in exercise["steps"]:
        if action == "extract":
            result = extract_frames(load_json(source), **params)
        elif action == "speed":
            result = speed(result, **params)
        # ... weitere Aktionen
    
    output_file = f"{exercise['name'].lower().replace(' ', '-')}.json"
    save_json(output_file, result)
    print(f"✅ {exercise['name']} → {output_file}")
```

---

## 📊 ERFOLGS-PROGNOSE

### Gesamt-Übersicht:

| Kategorie | Anzahl | Methode | Qualität | Zeitaufwand |
|-----------|--------|---------|----------|-------------|
| **Quick Wins** | 3 | Tool | 85-90% | 1-2h |
| **Gut machbar** | 2 | Tool | 60-75% | 2-3h |
| **Experimental** | 3 | Tool | 50-55% | 3-4h |
| **Downloads** | 6 | Internet | 90%+ | 1-2h |
| **GESAMT** | 14 | Mixed | 75% Ø | 7-11h |

### Realistische Erwartung:

**Nach Tool-Implementierung:**
- ✅ **8 Übungen selbst gebaut** (mit Tool)
- ✅ **6 Übungen heruntergeladen** (LottieFiles)
- 🎯 **14/14 Übungen verfügbar** (100% Coverage!)

**Coverage-Steigerung:**
- Vorher: 24/41 = **59%**
- Nach Quick Wins (bereits erledigt): 27/41 = **66%**
- Nach diesem Plan: 41/41 = **100%** 🎉

---

## ✅ ZUSAMMENFASSUNG TEIL 5

### Was wir geplant haben:

#### 8 Tool-basierte Rezepte:
1. ✅ **Box Jumps** - extract + speed (90%)
2. ✅ **Wandsitzen** - extract + speed sehr langsam (85%)
3. ✅ **Step-ups** - extract + speed (80%)
4. ✅ **Ab-Wheel** - combine Plank + Liegestütz (75%)
5. ✅ **Skaters** - extract + mirror + combine + speed (60%)
6. ✅ **Floor Slides** - mirror + rotate Superman (55%)
7. ⚠️ **Dead Bug** - speed Russian Twist (50%)
8. ⚠️ **Bird Dog** - isolate Plank (50%)

#### 6 Download-Empfehlungen:
9-14. Beinstrecker, Seitheben, Diamond Pushups, Hampelmänner, Handtuch-Latzug, Wadenheben

### Für jede Übung definiert:
- ✅ Basis-Animation
- ✅ Schritt-für-Schritt-Transformationen
- ✅ CLI-Befehle
- ✅ Parameter-Details
- ✅ Kotlin-Mapping
- ✅ Erfolgswahrscheinlichkeit

### Extras:
- ✅ Automatisierungs-Script (Bash + Python)
- ✅ Priorisierung (Phase 1-4)
- ✅ Erfolgs-Prognose
- ✅ Zeitaufwand-Schätzung

---

## 🎯 NÄCHSTE SCHRITTE

### Jetzt umsetzen:

1. **Tool implementieren** (TEIL 2-4 als Code)
   - Core, Analyzer, Transformer, Builder, CLI
   - ~10-15 Stunden Entwicklung

2. **Phase 1 ausführen** (Quick Wins)
   - Box Jumps, Wandsitzen, Step-ups
   - ~1-2 Stunden

3. **Downloads parallel** (6 Übungen)
   - LottieFiles.com durchsuchen
   - ~1-2 Stunden

4. **Integration in FORMIX**
   - Neue Animationen nach `assets/animations/`
   - `LottieAnimationProvider.kt` erweitern
   - Build + Test
   - ~1 Stunde

**GESAMT:** ~13-20 Stunden bis **100% Coverage!** 🎉

---

**Status:** ✅ TEIL 5 ABGESCHLOSSEN  
**Status:** ✅✅✅✅✅ **ALLE 5 TEILE KOMPLETT!**  
**Bereit für:** TOOL-IMPLEMENTIERUNG! 🚀
