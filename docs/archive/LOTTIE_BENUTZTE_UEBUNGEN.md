# 🎯 FORMIX - Tatsächlich BENUTZTE Übungen & Animation-Status

**Datum:** 01.05.2026  
**Zweck:** Analyse welche Übungen WIRKLICH in Trainingsplänen vorkommen  
**Status:** NUR ANALYSE - KEINE ÄNDERUNGEN

---

## 📋 METHODIK

Diese Analyse untersucht:
1. ✅ **PlanGenerator.kt** - Automatische Trainingspläne (4 Trainingstypen)
2. ✅ **AiPlanGenerator.kt** - KI-generierte Pläne (41 bekannte Übungen)
3. ✅ **ExerciseDefinitions.kt** - Alle definierten Übungen (59 Rules)
4. ✅ **ExerciseProData.kt** - Detaillierte Anleitungen (23 Übungen)

---

## 🎯 TATSÄCHLICH BENUTZTE ÜBUNGEN

### ⚡ CROSSFIT Trainingspläne (PlanGenerator)

| Nr | Übung | Animation | Status |
|----|-------|-----------|---------|
| 1 | Burpees | ✅ `Burpees.json` | ✅ PERFEKT |
| 2 | Thruster | ❌ Keine | ⚠️ FEHLT |
| 3 | Box Jumps | ❌ Keine | ⚠️ FEHLT |
| 4 | Plank | ✅ `Plank.json` | ✅ PERFEKT |
| 5 | Russian Twist | ✅ `russian-twist.json` | ✅ PERFEKT |

**Beine-Pool (CROSSFIT):** Kniebeugen, Ausfallschritte, Glute Bridges, Step-ups  
**Ziehen-Pool (CROSSFIT):** Klimmzüge, Handtuch-Latzug, Superman  
**Drücken-Pool (CROSSFIT):** Liegestütze, Dips am Stuhl

---

### 💪 STRENGTH Trainingspläne (PlanGenerator)

#### Hauptübungen (immer dabei):
| Nr | Übung | Animation | Status |
|----|-------|-----------|---------|
| 1 | Kniebeugen LH | ✅ `Kniebeugen LH.json` | ✅ PERFEKT |
| 2 | Beinpresse | ✅ `Beinpresse.json` | ✅ PERFEKT |
| 3 | Kreuzheben | ✅ `Kreuzheben .json` + `Hantel Kreuzheben.json` | ✅ PERFEKT |
| 4 | Bankdrücken | ✅ `Bankdrücken.json` | ✅ PERFEKT |
| 5 | Schulterpresse | ✅ `Schulterpresse.json` + `Schulterpresse KH.json` | ✅ PERFEKT |
| 6 | Dips | ✅ `dips.json` | ✅ PERFEKT |
| 7 | Latzug | ✅ `Latzug.json` | ✅ PERFEKT |
| 8 | Klimmzüge | ✅ `Klimmzüge.json` | ✅ PERFEKT |
| 9 | Bizeps Curls | ✅ `Bizeps Curls.json` | ✅ PERFEKT |

#### Zusatzübungen (Beine):
| Nr | Übung | Animation | Status |
|----|-------|-----------|---------|
| 10 | Beinstrecker | ❌ Keine | ⚠️ FEHLT |
| 11 | Beinbeuger | ✅ `Beinbeuger.json` | ✅ PERFEKT |

#### Zusatzübungen (Oberkörper):
| Nr | Übung | Animation | Status |
|----|-------|-----------|---------|
| 12 | Butterfly | ✅ `cable-chest-fly-exercise-for-chest.json` | ✅ PERFEKT |
| 13 | Seitheben | ❌ Keine | ⚠️ FEHLT |
| 14 | Trizepsdrücken | ✅ `Trizepsdrücken.json` | ✅ PERFEKT |

#### Core:
| Nr | Übung | Animation | Status |
|----|-------|-----------|---------|
| 15 | Beinheben | ⚠️ `man-doing-low-plank-leg-raise-exercise-for-legs.json` NICHT GEMAPPT! | 🔧 QUICK WIN! |
| 16 | Ab-Wheel | ❌ Keine | ⚠️ FEHLT |

---

### 🏠 BASICS Trainingspläne (PlanGenerator)

| Nr | Übung | Animation | Status |
|----|-------|-----------|---------|
| 1 | Kniebeugen | ✅ `Kniebeugen.json` | ✅ PERFEKT |
| 2 | Ausfallschritte | ✅ `Ausfallschritt.json` | ✅ PERFEKT |
| 3 | Liegestütze | ✅ `Liegestütz.json` | ✅ PERFEKT |
| 4 | Schulterpresse KH | ✅ `Schulterpresse KH.json` | ✅ PERFEKT |
| 5 | Latzug | ✅ `Latzug.json` | ✅ PERFEKT |
| 6 | Superman | ✅ `superman-exercise.json` | ✅ PERFEKT |
| 7 | Plank | ✅ `Plank.json` | ✅ PERFEKT |
| 8 | Sit-ups | ⚠️ `woman-doing-sit-ups-exercise.json` NICHT GEMAPPT! | 🔧 QUICK WIN! |

---

### 🏡 HOME Trainingspläne (PlanGenerator)

| Nr | Übung | Animation | Status |
|----|-------|-----------|---------|
| 1 | Bulgarian Split Squats | ✅ `bulgarian-split-squat-exercise.json` | ✅ PERFEKT |
| 2 | Ausfallschritte | ✅ `Ausfallschritt.json` | ✅ PERFEKT |
| 3 | Liegestütze | ✅ `Liegestütz.json` | ✅ PERFEKT |
| 4 | Pike Pushups | ✅ `Pike Pushups.json` | ✅ PERFEKT |
| 5 | Superman | ✅ `superman-exercise.json` | ✅ PERFEKT |
| 6 | Handtuch-Latzug | ❌ Keine | ⚠️ FEHLT |
| 7 | Plank | ✅ `Plank.json` | ✅ PERFEKT |
| 8 | Mountain Climbers | ✅ `mountain-climber-exercise.json` | ✅ PERFEKT |

---

### 🤖 KI-GENERIERTE Pläne (AiPlanGenerator.kt)

**41 bekannte Übungen** die der KI-Coach verwenden kann:

#### WEIGHTED (13 Übungen):
| Nr | Übung | Animation | Status |
|----|-------|-----------|---------|
| 1 | Bankdrücken | ✅ | ✅ |
| 2 | Schulterpresse | ✅ | ✅ |
| 3 | Latzug | ✅ | ✅ |
| 4 | Beinpresse | ✅ | ✅ |
| 5 | Beinstrecker | ❌ | ⚠️ FEHLT |
| 6 | Beinbeuger | ✅ | ✅ |
| 7 | Kreuzheben | ✅ | ✅ |
| 8 | Bizeps Curls | ✅ | ✅ |
| 9 | Trizepsdrücken | ✅ | ✅ |
| 10 | Seitheben | ❌ | ⚠️ FEHLT |
| 11 | Butterfly | ✅ | ✅ |
| 12 | Flys | ❌ | ⚠️ FEHLT |
| 13 | Wadenheben | ❌ | ⚠️ FEHLT |

#### WEIGHTED MIT HANTEL-ZUSATZ (3 Übungen):
| Nr | Übung | Animation | Status |
|----|-------|-----------|---------|
| 14 | Kniebeugen LH | ✅ | ✅ |
| 15 | Ausfallschritte KH | ✅ | ✅ |
| 16 | Thruster | ❌ | ⚠️ FEHLT (aber `woman-doing-dumbbell-squat-overhead-press-exercise-for-legs.json` vorhanden!) |

#### BODYWEIGHT Push/Pull (8 Übungen):
| Nr | Übung | Animation | Status |
|----|-------|-----------|---------|
| 17 | Liegestütze | ✅ | ✅ |
| 18 | Pike Pushups | ✅ | ✅ |
| 19 | Diamond Pushups | ❌ | ⚠️ FEHLT |
| 20 | Dips am Stuhl | ✅ | ✅ |
| 21 | Klimmzüge | ✅ | ✅ |
| 22 | Superman | ✅ | ✅ |
| 23 | Handtuch-Latzug | ❌ | ⚠️ FEHLT |
| 24 | Floor Slides | ❌ | ⚠️ FEHLT |

#### BODYWEIGHT Beine (4 Übungen):
| Nr | Übung | Animation | Status |
|----|-------|-----------|---------|
| 25 | Kniebeugen | ✅ | ✅ |
| 26 | Ausfallschritte | ✅ | ✅ |
| 27 | Glute Bridges | ✅ | ✅ |
| 28 | Step-ups | ❌ | ⚠️ FEHLT |

#### BODYWEIGHT Explosiv (4 Übungen):
| Nr | Übung | Animation | Status |
|----|-------|-----------|---------|
| 29 | Burpees | ✅ | ✅ |
| 30 | Skaters | ❌ | ⚠️ FEHLT |
| 31 | Box Jumps | ❌ | ⚠️ FEHLT |

#### TIME BASED (4 Übungen):
| Nr | Übung | Animation | Status |
|----|-------|-----------|---------|
| 32 | Plank | ✅ | ✅ |
| 33 | Mountain Climbers | ✅ | ✅ |
| 34 | Wandsitzen | ❌ | ⚠️ FEHLT |
| 35 | Hampelmänner | ❌ | ⚠️ FEHLT |
| 36 | Plank Jacks | ❌ | ⚠️ FEHLT |

#### CORE REPS (6 Übungen):
| Nr | Übung | Animation | Status |
|----|-------|-----------|---------|
| 37 | Crunches | ⚠️ | 🔧 QUICK WIN! (`woman-doing-sit-ups-exercise.json`) |
| 38 | Beinheben | ⚠️ | 🔧 QUICK WIN! (`man-doing-low-plank-leg-raise-exercise-for-legs.json`) |
| 39 | Russian Twist | ✅ | ✅ |
| 40 | Dead Bug | ❌ | ⚠️ FEHLT |
| 41 | Bird Dog | ❌ | ⚠️ FEHLT |

---

## 🔥 ALTERNATIVE ÜBUNGEN (Equipment-Filtersystem)

Diese Übungen werden als Alternativen eingesetzt wenn Equipment fehlt:

### Bodyweight-Alternativen:
| Übung | Animation | Status |
|-------|-----------|---------|
| Liegestütze | ✅ | ✅ |
| Pike Pushups | ✅ | ✅ |
| Diamond Pushups | ❌ | ⚠️ FEHLT |
| Dips am Stuhl | ✅ | ✅ |
| Superman | ✅ | ✅ |
| Handtuch-Latzug | ❌ | ⚠️ FEHLT |
| Floor Slides | ❌ | ⚠️ FEHLT |
| Kniebeugen | ✅ | ✅ |
| Ausfallschritte | ✅ | ✅ |
| Glute Bridges | ✅ | ✅ |
| Step-ups | ❌ | ⚠️ FEHLT |
| Plank | ✅ | ✅ |
| Crunches | ⚠️ | 🔧 QUICK WIN! |
| Beinheben | ⚠️ | 🔧 QUICK WIN! |
| Dead Bug | ❌ | ⚠️ FEHLT |
| Bird Dog | ❌ | ⚠️ FEHLT |

### Minimales Equipment (Hanteln):
| Übung | Animation | Status |
|-------|-----------|---------|
| Schulterpresse KH | ✅ | ✅ |
| Fliegende KH | ❌ | ⚠️ FEHLT |
| Kniebeugen KH | ✅ | ✅ |
| Ausfallschritte KH | ✅ | ✅ |
| Bizeps Curls KH | ✅ | ✅ |
| Thruster KH | ❌ | ⚠️ FEHLT |

---

## 🏥 ÜBUNGEN BEI GESUNDHEITS-EINSCHRÄNKUNGEN

### Bei Knieproblemen (Alternativen):
| Übung | Animation | Status |
|-------|-----------|---------|
| Glute Bridges | ✅ | ✅ |
| Step-ups | ❌ | ⚠️ FEHLT |
| Wandsitzen | ❌ | ⚠️ FEHLT |

### Bei Rückenproblemen:
| Übung | Animation | Status |
|-------|-----------|---------|
| Superman | ✅ | ✅ |
| Floor Slides | ❌ | ⚠️ FEHLT |
| Handtuch-Latzug | ❌ | ⚠️ FEHLT |

### Bei Schulterproblemen:
| Übung | Animation | Status |
|-------|-----------|---------|
| Liegestütze | ✅ | ✅ |
| Dips am Stuhl | ✅ | ✅ |

### Bei Herzproblemen (Low Intensity):
| Übung | Animation | Status |
|-------|-----------|---------|
| Hampelmänner | ❌ | ⚠️ FEHLT |
| Skaters | ❌ | ⚠️ FEHLT |

### Safe Core-Übungen:
| Übung | Animation | Status |
|-------|-----------|---------|
| Dead Bug | ❌ | ⚠️ FEHLT |
| Bird Dog | ❌ | ⚠️ FEHLT |
| Plank | ✅ | ✅ |

---

## 📊 STATISTIK - BENUTZTE ÜBUNGEN

### Gesamt-Übersicht:
| Kategorie | Total | Mit Animation | Ohne Animation | Abdeckung |
|-----------|-------|---------------|----------------|-----------|
| **CROSSFIT** | 5 | 3 (60%) | 2 (40%) | 🟡 Mittel |
| **STRENGTH Haupt** | 9 | 9 (100%) | 0 (0%) | 🟢 PERFEKT! |
| **STRENGTH Zusatz** | 5 | 3 (60%) | 2 (40%) | 🟡 Mittel |
| **BASICS** | 8 | 7 (88%) | 1 (12%) | 🟢 Sehr gut |
| **HOME** | 8 | 7 (88%) | 1 (12%) | 🟢 Sehr gut |
| **KI (41 Übungen)** | 41 | 24 (59%) | 17 (41%) | 🟡 Mittel |

### Nach Übungstyp:
| Typ | Total | Mit Animation | Abdeckung |
|-----|-------|---------------|-----------|
| **WEIGHTED** | 16 | 12 (75%) | 🟢 Gut |
| **BODYWEIGHT Push/Pull** | 8 | 5 (63%) | 🟡 Mittel |
| **BODYWEIGHT Beine** | 4 | 3 (75%) | 🟢 Gut |
| **BODYWEIGHT Explosiv** | 4 | 1 (25%) | 🔴 Schlecht |
| **TIME BASED** | 5 | 2 (40%) | 🔴 Schlecht |
| **CORE REPS** | 6 | 1 (17%) | 🔴 SEHR SCHLECHT |

---

## 🚨 KRITISCHE LÜCKEN

### ⚠️ HOHE PRIORITÄT (in vielen Plänen):
1. **Beinstrecker** - STRENGTH Zusatz (immer dabei)
2. **Seitheben** - STRENGTH Zusatz (immer dabei)
3. **Thruster** - CROSSFIT Haupt + KI-Coach
4. **Box Jumps** - CROSSFIT Haupt
5. **Handtuch-Latzug** - HOME + Rücken-Alternative

### ⚠️ MITTLERE PRIORITÄT (Alternativen/Filter):
6. **Step-ups** - Bodyweight Alternative + Knie-Probleme
7. **Wandsitzen** - Knie-Probleme Alternative
8. **Diamond Pushups** - Bodyweight Alternative
9. **Floor Slides** - Rücken-Probleme Alternative
10. **Skaters** - Herz-Probleme Alternative
11. **Hampelmänner** - Herz-Probleme Alternative

### ⚠️ NIEDRIGE PRIORITÄT (selten genutzt):
12. **Flys** - Nur bei Equipment-Filter
13. **Wadenheben** - KI-Coach (selten)
14. **Ab-Wheel** - STRENGTH Core (selten)
15. **Plank Jacks** - TIME BASED (selten)
16. **Dead Bug** - Core Alternative
17. **Bird Dog** - Core Alternative

---

## 🔧 QUICK WINS - 3 SOFORT LÖSBAR!

Diese Animationen sind VORHANDEN, aber nicht gemappt:

### 1. Crunches / Sit-ups
**Problem:** In BASICS-Plänen als "Sit-ups", in KI als "Crunches"  
**Lösung:** Animation vorhanden: `woman-doing-sit-ups-exercise.json`  
**Aktion:** Synonym-Map erweitern:  
```kotlin
"Crunch" to listOf("crunch", "situp", "sit", "ups")
"Sit-ups" to listOf("situp", "sit", "ups", "crunch")
```

### 2. Beinheben
**Problem:** STRENGTH Core (immer dabei)  
**Lösung:** Animation vorhanden: `man-doing-low-plank-leg-raise-exercise-for-legs.json`  
**Aktion:** Synonym-Map erweitern:  
```kotlin
"Beinheben" to listOf("beinheben", "leg", "raise", "plank")
```

### 3. Thruster
**Problem:** CROSSFIT + KI-Coach  
**Lösung:** Animation vorhanden: `woman-doing-dumbbell-squat-overhead-press-exercise-for-legs.json`  
**Aktion:** Synonym-Map erweitern:  
```kotlin
"Thruster" to listOf("thruster", "overhead", "squat", "dumbbell", "press")
```

---

## 📈 VERBESSERUNGSVORSCHLAG - PRIORITÄTENLISTE

### PHASE 1: Quick Wins (3 Übungen, 0 neue Dateien)
- ✅ Crunches/Sit-ups mappen
- ✅ Beinheben mappen
- ✅ Thruster mappen

**Impact:** +7% Abdeckung (von 59% auf 66%)

### PHASE 2: Englische Varianten besser nutzen (13 Dateien vorhanden!)
- Bulgarian Split Squats ✅ (bereits gemappt)
- Lat Pull Down → Latzug
- Smith Bench Press → Bankdrücken
- Seated Dumbbell Press → Schulterpresse
- Leg Curl → Beinbeuger
- Leg Press → Beinpresse
- Sumo Squat → Kniebeugen

**Impact:** +5% Abdeckung (bessere Varianz)

### PHASE 3: Kritische Lücken füllen (14 neue Dateien benötigt)
1. Beinstrecker (HOHE PRIORITÄT)
2. Seitheben (HOHE PRIORITÄT)
3. Box Jumps (HOHE PRIORITÄT)
4. Handtuch-Latzug (HOHE PRIORITÄT)
5. Step-ups (MITTLERE PRIORITÄT)
6. Wandsitzen (MITTLERE PRIORITÄT)
7. Diamond Pushups (MITTLERE PRIORITÄT)
8. Floor Slides (MITTLERE PRIORITÄT)
9. Skaters (MITTLERE PRIORITÄT)
10. Hampelmänner (MITTLERE PRIORITÄT)
11. Dead Bug (NIEDRIGE PRIORITÄT)
12. Bird Dog (NIEDRIGE PRIORITÄT)
13. Flys (NIEDRIGE PRIORITÄT)
14. Wadenheben (NIEDRIGE PRIORITÄT)

**Impact:** +34% Abdeckung (von 66% auf 100%)

---

## 💡 EMPFEHLUNG

### Sofort umsetzen (HEUTE):
1. ✅ **Quick Win #1**: Crunches/Sit-ups mappen → `woman-doing-sit-ups-exercise.json`
2. ✅ **Quick Win #2**: Beinheben mappen → `man-doing-low-plank-leg-raise-exercise-for-legs.json`
3. ✅ **Quick Win #3**: Thruster mappen → `woman-doing-dumbbell-squat-overhead-press-exercise-for-legs.json`

**Aufwand:** ~5 Zeilen Code in `LottieAnimationProvider.kt`  
**Nutzen:** 3 wichtige Übungen sofort verfügbar

### Mittelfristig (DIESE WOCHE):
4. Englische Varianten besser nutzen (Synonym-Map erweitern)

### Langfristig (BEI BEDARF):
5. Fehlende Animationen beschaffen (14 Dateien)

---

## ✅ ZUSAMMENFASSUNG

### Was funktioniert GUT:
- ✅ **STRENGTH Hauptübungen:** 100% Abdeckung!
- ✅ **BASICS & HOME:** 88% Abdeckung
- ✅ **WEIGHTED Übungen:** 75% Abdeckung

### Was FEHLT:
- ❌ **CORE Übungen:** Nur 17% Abdeckung (1 von 6)
- ❌ **TIME BASED:** Nur 40% Abdeckung (2 von 5)
- ❌ **BODYWEIGHT Explosiv:** Nur 25% Abdeckung (1 von 4)

### Sofort-Maßnahme:
**3 Quick Wins** können die Gesamt-Abdeckung von **59% auf 66%** steigern!

---

**Status:** ✅ ANALYSE ABGESCHLOSSEN - BEREIT FÜR QUICK WINS
