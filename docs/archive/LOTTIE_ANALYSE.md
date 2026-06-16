# 🎬 Lottie-Animationen Analyse - FORMIX App

**Datum:** 01.05.2026  
**Status:** Bestandsaufnahme - KEINE Änderungen vorgenommen

---

## 📊 Übersicht

### Vorhandene Lottie-Dateien: **70 Animationen**
- 44 Übungs-Animationen (Hauptverzeichnis)
- 26 Körperteil-Animationen (Unterverzeichnis `Körper/`)

### In der App definierte Übungen: **46 Übungen**
- 23 mit DetailedInstruction (ExerciseProData.kt)
- 59 Exercise Rules (ExerciseDefinitions.kt)

---

## ✅ ÜBUNGEN MIT ANIMATIONEN (23 Übungen)

Diese Übungen haben sowohl eine JSON-Animation als auch sind in der App definiert:

### WEIGHTED (Krafttraining mit Gewicht)
| Nr | Übungsname | Animation-Datei | Status |
|----|-----------|-----------------|---------|
| 1  | Bankdrücken | `Bankdrücken.json` | ✅ |
| 2  | Schulterpresse | `Schulterpresse.json` + `Schulterpresse KH.json` | ✅ |
| 3  | Latzug | `Latzug.json` | ✅ |
| 4  | Beinpresse | `Beinpresse.json` | ✅ |
| 5  | Beinbeuger | `Beinbeuger.json` | ✅ |
| 6  | Kreuzheben | `Kreuzheben .json` + `Hantel Kreuzheben.json` | ✅ |
| 7  | Bizeps Curls | `Bizeps Curls.json` | ✅ |
| 8  | Trizepsdrücken | `Trizepsdrücken.json` | ✅ |
| 9  | Butterfly | `cable-chest-fly-exercise-for-chest.json` | ✅ |

### BODYWEIGHT (Eigengewicht)
| Nr | Übungsname | Animation-Datei | Status |
|----|-----------|-----------------|---------|
| 10 | Liegestütze | `Liegestütz.json` + `woman-doing-push-ups.json` | ✅ |
| 11 | Pike Pushups | `Pike Pushups.json` | ✅ |
| 12 | Dips (am Stuhl) | `dips.json` + `Tipps am Stuhl.json` | ✅ |
| 13 | Klimmzüge | `Klimmzüge.json` + `side-hand-pull-up-exercise-for-back.json` | ✅ |
| 14 | Kniebeugen | `Kniebeugen.json` + `Kniebeugen LH.json` | ✅ |
| 15 | Ausfallschritte | `Ausfallschritt.json` + mehrere Bulgarian/Split | ✅ |
| 16 | Glute Bridge | `Glute Bridge.json` | ✅ |
| 17 | Burpees | `Burpees.json` | ✅ |

### TIME BASED / CORE
| Nr | Übungsname | Animation-Datei | Status |
|----|-----------|-----------------|---------|
| 18 | Plank | `Plank.json` | ✅ |
| 19 | Mountain Climbers | `mountain-climber-exercise.json` | ✅ |
| 20 | Russian Twist | `russian-twist.json` | ✅ |

### SONSTIGE
| Nr | Übungsname | Animation-Datei | Status |
|----|-----------|-----------------|---------|
| 21 | Dumbbell Swing | `Dumbbell Swing.json` | ✅ |
| 22 | Crosstrainer | `crosstrainer.json` | ✅ |
| 23 | Superman | `superman-exercise.json` | ✅ |

---

## ❌ ÜBUNGEN OHNE ANIMATIONEN (23 Übungen)

Diese Übungen sind in der App definiert, haben aber KEINE passende Animation:

### WEIGHTED
| Nr | Übungsname | Defined In | Fallback |
|----|-----------|------------|----------|
| 1  | Beinstrecker | ExerciseProData.kt, ExerciseDefinitions.kt | ❌ Keine Animation |
| 2  | Seitheben | ExerciseProData.kt, ExerciseDefinitions.kt | ❌ Keine Animation |
| 3  | Flys | ExerciseDefinitions.kt | ❌ Keine Animation |
| 4  | Wadenheben | ExerciseProData.kt, ExerciseDefinitions.kt | ❌ Keine Animation |
| 5  | Thruster | ExerciseProData.kt, ExerciseDefinitions.kt | ❌ Keine Animation |

### BODYWEIGHT
| Nr | Übungsname | Defined In | Fallback |
|----|-----------|------------|----------|
| 6  | Diamond Pushup | ExerciseDefinitions.kt | ❌ Keine Animation |
| 7  | Handtuch-Latzug | ExerciseDefinitions.kt | ❌ Keine Animation |
| 8  | Floor Slides | ExerciseDefinitions.kt | ❌ Keine Animation |
| 9  | Step-ups | ExerciseDefinitions.kt | ❌ Keine Animation |
| 10 | Skaters | ExerciseDefinitions.kt | ❌ Keine Animation |
| 11 | Box Jump | ExerciseDefinitions.kt | ❌ Keine Animation |

### TIME BASED / CORE
| Nr | Übungsname | Defined In | Fallback |
|----|-----------|------------|----------|
| 12 | Wandsitzen | ExerciseDefinitions.kt | ❌ Keine Animation |
| 13 | Hampelmänner | ExerciseDefinitions.kt | ❌ Keine Animation |
| 14 | Plank Jacks | ExerciseDefinitions.kt | ❌ Keine Animation |
| 15 | Crunch | ExerciseProData.kt, ExerciseDefinitions.kt | ⚠️ `woman-doing-sit-ups-exercise.json` vorhanden! |
| 16 | Beinheben | ExerciseDefinitions.kt | ⚠️ `man-doing-low-plank-leg-raise-exercise-for-legs.json` vorhanden! |
| 17 | Dead Bug | ExerciseDefinitions.kt | ❌ Keine Animation |
| 18 | Bird Dog | ExerciseDefinitions.kt | ❌ Keine Animation |

---

## 🎯 ANIMATIONEN OHNE ZUORDNUNG (21 Dateien)

Diese Animation-Dateien sind vorhanden, aber werden NICHT von Übungen genutzt:

### Englische Varianten (könnten gemappt werden)
| Nr | Animation-Datei | Mögliche Zuordnung |
|----|-----------------|-------------------|
| 1  | `bulgarian-split-squat-exercise.json` | → Ausfallschritte |
| 2  | `woman-doing-barbell-bulgarian-split-squat-exercise-for-legs.json` | → Ausfallschritte |
| 3  | `woman-doing-left-leg-forward-lunge-exercise-for-legs.json` | → Ausfallschritte |
| 4  | `lever-seated-leg-curl-exercise-for-legs.json` | → Beinbeuger |
| 5  | `man-doing-sled-horizontal-leg-press-exercise-for-legs.json` | → Beinpresse |
| 6  | `man-doing-sumo-squat-exercise-for-legs.json` | → Kniebeugen |
| 7  | `woman-doing-barbell-sumo-squat-exercise-for-legs.json` | → Kniebeugen |
| 8  | `woman-doing-dumbbell-squat-overhead-press-exercise-for-legs.json` | → Thruster? |
| 9  | `woman-doing-lat-pull-down-exercise.json` | → Latzug |
| 10 | `woman-doing-smith-bench-press-exercise-for-chest.json` | → Bankdrücken |
| 11 | `man-doing-seated-dumbbell-shoulders-press-exercise-for-shoulders.json` | → Schulterpresse |
| 12 | `woman-doing-sit-ups-exercise.json` | → Crunch/Sit-ups |
| 13 | `man-doing-low-plank-leg-raise-exercise-for-legs.json` | → Beinheben |

### Ohne klare Zuordnung
| Nr | Animation-Datei | Bemerkung |
|----|-----------------|-----------|
| 14 | `Diagramm.json` | ⚠️ Kein Übungsbezug |
| 15 | `trainer.json` | ⚠️ Allgemeine Animation |

### Körperteil-Animationen (26 Dateien in `Körper/`)
Diese werden für die **3D-Body-Selektor** verwendet, nicht für Übungs-Animationen:
- 13x Mann (abs, back, biceps, calves, chest, forearms, glutes, hamstrings, neck, shoulders, thigs, triceps, back-calves)
- 13x Frau (gleiche Bereiche)

---

## 🔧 WIE FUNKTIONIERT DAS MATCHING?

### System: LottieAnimationProvider.kt

**Strategie 1: Direkter Name-Match**
```
"Bankdrücken" → sucht nach "Bankdrücken.json"
```

**Strategie 2: Synonym-basiertes Matching**
```
"Bankdrücken" → Keywords: ["bankdruck", "bench", "press", "smith"]
Durchsucht alle Dateien nach diesen Keywords
Score: 100 (exakt), 80 (enthält), 0 (kein Match)
```

**Minimaler Score für Match: 90 (oder 80 wenn eindeutig)**

### Synonym-Map (46 Übungen definiert)
Das System hat bereits Synonyme für:
- Alle WEIGHTED Übungen (13)
- Alle BODYWEIGHT Übungen (15)
- Alle TIME BASED Übungen (5)
- Alle CORE REPS Übungen (5)
- Plus 8 neue Übungen (Dumbbell Swing, Crosstrainer, etc.)

---

## 📈 STATISTIK

### Abdeckung
- **Übungen MIT Animation:** 23 / 46 = **50%**
- **Übungen OHNE Animation:** 23 / 46 = **50%**
- **Animationen GENUTZT:** ~25 / 70 = **36%**
- **Animationen UNGENUTZT:** ~45 / 70 = **64%**

### Nach Kategorie

| Kategorie | Übungen Total | Mit Animation | Ohne Animation |
|-----------|---------------|---------------|----------------|
| WEIGHTED | 13 | 9 (69%) | 4 (31%) |
| BODYWEIGHT | 17 | 11 (65%) | 6 (35%) |
| TIME/CORE | 16 | 3 (19%) | 13 (81%) |

### Kritische Lücken
**Core-Übungen haben die schlechteste Abdeckung!**
- Crunch ❌ (obwohl `woman-doing-sit-ups-exercise.json` vorhanden!)
- Beinheben ❌ (obwohl `man-doing-low-plank-leg-raise-exercise-for-legs.json` vorhanden!)
- Dead Bug ❌
- Bird Dog ❌
- Wandsitzen ❌
- Hampelmänner ❌
- Plank Jacks ❌

---

## 💡 EMPFEHLUNGEN

### Quick Wins (bestehende Dateien besser mappen)
1. **Crunch** → `woman-doing-sit-ups-exercise.json` zuordnen
2. **Beinheben** → `man-doing-low-plank-leg-raise-exercise-for-legs.json` zuordnen
3. **Thruster** → `woman-doing-dumbbell-squat-overhead-press-exercise-for-legs.json` zuordnen

### Synonym-Map erweitern
Die englischen Varianten in die Synonym-Map aufnehmen:
- `"Beinbeuger"` → "lever", "seated", "curl"
- `"Crunch"` → "situp", "sit", "ups"
- `"Beinheben"` → "leg", "raise", "plank"

### Fehlende Animationen beschaffen
Für diese 20 Übungen gibt es KEINE passende Animation:
- Diamond Pushup, Handtuch-Latzug, Floor Slides, Step-ups, Skaters, Box Jump
- Beinstrecker, Seitheben, Flys, Wadenheben
- Wandsitzen, Hampelmänner, Plank Jacks, Dead Bug, Bird Dog

---

## 🎯 NÄCHSTE SCHRITTE (nicht durchgeführt)

1. ✅ **Analyse durchgeführt** - diese Datei
2. ⏸️ Synonym-Map erweitern (nur auf Anfrage)
3. ⏸️ Fehlende Animationen beschaffen (nur auf Anfrage)
4. ⏸️ Testing der bestehenden Matches (nur auf Anfrage)

---

**Status:** ✅ NUR ANALYSE - KEINE ÄNDERUNGEN VORGENOMMEN
