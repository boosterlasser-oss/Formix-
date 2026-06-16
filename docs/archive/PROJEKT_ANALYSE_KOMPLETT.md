# 🏋️ VOLLSTÄNDIGE PROJEKT-ANALYSE
## Performance Planner App - Stand nach Refactoring

**Datum:** 23. Februar 2026  
**Analysiert von:** GitHub Copilot

---

## 📁 AKTUELLE PROJEKTSTRUKTUR

```
com.fantasyfoodplanner/
├── MainActivity.kt                    → Navigation & App-Start
│
├── data/                              → 📦 DATENBANK & ENTITIES
│   ├── AppDb.kt                      → Room Database Singleton
│   ├── Dao.kt                        → Data Access Objects
│   ├── Entities.kt                   → UserProfile, WorkoutEntry, ExerciseLog, SetLog
│   ├── FoodDatabase.kt               → Ernährungs-Daten
│   └── Seeder.kt                     → Initialdaten
│
├── features/                          → 🖥️ FEATURE-SCREENS
│   ├── fitness/                      → HAUPT-FITNESS-MODUL
│   │   ├── TrainingFlowScreen.kt     → Trainingsablauf (732 Zeilen) ✅ REFACTORED
│   │   ├── BodySelector3D.kt         → 3D-Körperteil-Auswahl mit Lottie
│   │   ├── PlanGenerator.kt          → Trainingsplan-Generierung
│   │   ├── ExerciseDefinitions.kt    → Übungsdefinitionen mit Regex-Matching
│   │   ├── ExerciseDetailScreen.kt   → Übungsdetails + YouTube
│   │   ├── FitnessModels.kt          → WorkoutPlan, WorkoutBlock, FitnessProfile
│   │   ├── StatsOverviewScreen.kt    → Statistik-Übersicht
│   │   └── WorkoutStatsScreen.kt     → Workout-Historie
│   ├── Dashboard.kt                  → Hauptbildschirm
│   ├── Planner.kt                    → Ernährungsplaner
│   └── Profile.kt                    → Benutzerprofil
│
├── logic/                             → 🧠 BUSINESS-LOGIK
│   ├── ExerciseLogic.kt              → Progressions-Berechnung & Limit-Erkennung
│   ├── SettingsManager.kt            → Einstellungen, LearningProfile, Persistenz
│   ├── CheckInAnalyzer.kt            → ✅ NEU: Check-In Text-Analyse
│   ├── BodyFocus.kt                  → ✅ NEU: Typsichere Body-Focus Enums
│   └── TrainingValidator.kt          → Validierung
│
└── ui/                                → 🎨 UI-KOMPONENTEN
    ├── FantasyKit.kt                 → Design-System (Colors, Buttons, Cards)
    ├── WorkoutComponents.kt          → UnifiedExerciseCard, CompactTimer
    └── TimerService.kt               → Hintergrund-Timer mit Notification
```

---

## 🔄 HAUPT-DATENFLUSS (TrainingFlowScreen)

```
┌──────────────────────┐
│   TrainingStep       │  ← enum class (typsicher!)
│   TYPE_SELECTION     │
└──────────┬───────────┘
           │ Bestätigen
           ▼
┌──────────────────────┐
│   TrainingStep       │
│   CHECK_IN           │
│                      │
│  ┌────────────────┐  │
│  │ CheckInAnalyzer│  │  ← Analysiert Freitext
│  │ .analyze()     │  │
│  └───────┬────────┘  │
│          ▼           │
│  ┌────────────────┐  │
│  │ BodyFocus      │  │  ← Mappt Körperteile → Kategorien
│  │ .toCategory    │  │
│  │  Strings()     │  │
│  └───────┬────────┘  │
│          ▼           │
│  ┌────────────────┐  │
│  │ PlanGenerator  │  │  ← Erstellt Basis-Plan
│  │ .buildPlan()   │  │
│  └───────┬────────┘  │
│          ▼           │
│  ┌────────────────┐  │
│  │ ExerciseLogic  │  │  ← Personalisiert nach Historie
│  │ .calculate     │  │
│  │  NextProg...() │  │
│  └────────────────┘  │
└──────────┬───────────┘
           │ START Button
           ▼
┌──────────────────────┐
│   TrainingStep       │
│   WORKOUT            │
│                      │
│  • Warmup-Karte      │
│  • Übungs-Karten     │
│  • Cooldown-Karte    │  ← ✅ NEU im Flow!
│  • DB-Speicherung    │
└──────────────────────┘
```

---

## ⚙️ KERN-KOMPONENTEN DETAIL

### 1. CheckInAnalyzer (NEU)
**Datei:** `logic/CheckInAnalyzer.kt` (188 Zeilen)

```kotlin
// Eingabe
CheckInAnalyzer.analyze(text: String, learning: LearningProfile)

// Ausgabe
AnalysisResult(
    mode: SessionMode,        // RECOVERY, SHORT, NORMAL, PUSH
    focus: FocusMode,         // MOBILITY, TECHNIQUE, EFFICIENCY, PERFORMANCE, NORMAL
    modifier: DailyModifier,  // intensityFactor, volumeFactor, timeFactor
    sorenessFocus: String?,   // z.B. "Schultern"
    explanation: String       // z.B. "⚡ Push-Modus aktiv!"
)
```

**Keyword-Kategorien (erweitert):**
| Kategorie | Keywords (Beispiele) | Resultat |
|-----------|----------|----------|
| LOW | müde, kaputt, schlapp, fertig, hundemüde, exhausted | SHORT + TECHNIQUE |
| HIGH | fit, motiviert, energie, topfit, power, hyped, pumped | PUSH + PERFORMANCE |
| TIME_PRESSURE | wenig zeit, schnell, 30 min, nur kurz | SHORT + EFFICIENCY |
| PAIN | schmerz, muskelkater, verspannt, verletzt, zieht | RECOVERY + MOBILITY |
| STRESS | stress, arbeit, druck, gestresst, hektisch | SHORT + TECHNIQUE |

**Soreness-Focus Erkennung:**
- Schultern, Rücken, Beine, Brust, Arme, Bauch/Core, Nacken, Po/Gesäß
- Unterstützt auch Englisch (shoulder, back, leg, chest, etc.)

---

### 2. BodyFocus + ExerciseCategory (NEU)
**Datei:** `logic/BodyFocus.kt` (69 Zeilen)

```kotlin
enum class BodyFocus(val displayName: String, val categories: List<ExerciseCategory>) {
    CHEST("Brust", listOf(PUSH)),
    SHOULDERS("Schultern", listOf(PUSH)),
    TRICEPS("Trizeps", listOf(PUSH)),
    BACK("Rücken", listOf(PULL)),
    BICEPS("Bizeps", listOf(PULL)),
    LEGS("Beine", listOf(LEGS)),
    GLUTES("Po / Gesäß", listOf(LEGS)),
    CORE("Bauch / Core", listOf(CORE)),
    NECK("Nacken", listOf(CORE)),
    ARMS("Arme", listOf(PUSH, PULL))
}

enum class ExerciseCategory(val categoryId: String) {
    PUSH("push"), PULL("pull"), LEGS("legs"), CORE("core"), CROSS("cross")
}

// Verwendung:
val categories = BodyFocus.toCategoryStrings(setOf("Brust", "Rücken"))
// → setOf("push", "pull")
```

---

### 3. SessionMode Anpassungen
**Datei:** `TrainingFlowScreen.kt`

| Modus | Sets | Reps | Gewicht | Übungen | Erklärung |
|-------|------|------|---------|---------|-----------|
| **PUSH** | +1 (max 5) | +10% | +5% (max +2.5kg) | Alle | "⚡ Push-Modus aktiv!" |
| **NORMAL** | Standard | Standard | Standard | Max 5 | "✅ Normal-Modus" |
| **SHORT** | -1 | -15% (Technik) | -5% (Technik) | Max 3 | "⏱️ Short-Modus aktiv" |
| **RECOVERY** | Max 2 | -30% | -30% | Max 2 | "🛡️ Recovery-Modus aktiv" |

---

### 4. ExerciseLogic - Progressions-System
**Datei:** `logic/ExerciseLogic.kt` (106 Zeilen)

```kotlin
// Drei Übungstypen:
enum class ExerciseType { WEIGHTED, BODYWEIGHT, TIME }

// Progression bei Erfolg:
WEIGHTED:     +2.5 kg
BODYWEIGHT:   +1 Satz (bis 5), dann +1 Schwierigkeit
TIME:         +30 Sekunden

// Limit-Erkennung (isStructuralLimitReached):
- 3 Fails hintereinander → Limit
- SuccessRate < 65% über 10 Sessions → Limit
- Kein Fortschritt über 5 Sessions → Limit
```

---

### 5. PlanGenerator
**Datei:** `features/fitness/PlanGenerator.kt` (234 Zeilen)

**Trainingstypen:**
| Typ | Übungen | Fokus |
|-----|---------|-------|
| CROSSFIT | Burpees, Thruster, Box Jumps + Basics | Explosiv + Kondition |
| STRENGTH | Bankdrücken, Kniebeugen LH, Latzug | Kraft mit Gewicht |
| BASICS | Liegestütze, Ausfallschritte, Plank | Grundübungen |
| HOME | Bulgarian Split Squats, Pike Pushups | Ohne Equipment |

**Warmup-Pool (9 Cardio + 8 Mobility + 6 Activation + 5 Dynamic Stretch)**

---

## 📊 DATENBANK-STRUKTUR

```
┌─────────────────┐
│   UserProfile   │
├─────────────────┤
│ name, age       │
│ weightKg        │
│ experience      │  ← "new", "some", "pro"
│ goal            │  ← "build", "lose", "fit"
│ dailyKcalTarget │
└─────────────────┘
         │
         │ 1:n
         ▼
┌─────────────────┐     ┌─────────────────┐
│  WorkoutEntry   │     │   ExerciseLog   │
├─────────────────┤     ├─────────────────┤
│ dateEpochDay    │     │ exerciseName    │
│ type            │ 1:n │ plannedSets     │
│ completed       │────▶│ actualSetsDone  │
└─────────────────┘     │ weightKg        │
                        │ wasSuccessful   │
                        └────────┬────────┘
                                 │ 1:n
                                 ▼
                        ┌─────────────────┐
                        │     SetLog      │
                        ├─────────────────┤
                        │ setIndex        │
                        │ repsDone        │
                        │ setSuccess      │
                        └─────────────────┘
```

---

## ✅ DURCHGEFÜHRTE REFACTORINGS

| # | Änderung | Vorher | Nachher |
|---|----------|--------|---------|
| 1 | Step-Variable | `String` ("TYPE_SELECTION") | `enum class TrainingStep` |
| 2 | Check-In Analyse | 70 Zeilen in UI | Eigene Klasse `CheckInAnalyzer.kt` |
| 3 | Body-Focus Mapping | when-Block mit Strings | `enum class BodyFocus` |
| 4 | Cooldown | Nicht im Flow | ✅ Jetzt als Karte angezeigt |
| 5 | Erklärbarkeit | Keine | ✅ User sieht WARUM Modus gewählt |
| 6 | Unbenutzte Funktionen | FocusDropdownField, normalizeFocus | Entfernt |
| 7 | **Entwicklungskurven** | Einfacher KombiChart | ✅ NEU: Lottie + Tabs (Training/Ernährung) |

**Neue Features in StatsOverviewScreen:**
- 🎬 **Lottie-Animation** "Diagramm.json" im Header
- 📊 **Zwei Tabs**: Training (Volumen, Sets, Reps) & Ernährung (Kalorien, Makros)
- 📈 **Separate Charts**: Balken für Volumen, Linie für Kalorien
- 🎯 **Mini-Statistiken** mit Emojis pro Tab

**Dateigröße TrainingFlowScreen.kt:** 833 → **732 Zeilen** (-12%)

---

## 🧩 KOMPONENTEN-ABHÄNGIGKEITEN

```
TrainingFlowScreen
    │
    ├── CheckInAnalyzer.analyze()
    │       └── LearningProfile (aus SettingsManager)
    │
    ├── BodyFocus.toCategoryStrings()
    │       └── ExerciseCategory
    │
    ├── PlanGenerator.buildPlan()
    │       ├── FitnessProfile
    │       ├── TrainingType
    │       ├── DailyModifier
    │       └── ExerciseDefinitions
    │
    ├── ExerciseLogic.calculateNextProgression()
    │       └── ExerciseWithSets (aus DB)
    │
    ├── FitnessPersistence
    │       └── SharedPreferences (JSON)
    │
    └── TimerService
            └── Foreground Notification
```

---

## 🎯 VERBLEIBENDE VERBESSERUNGSMÖGLICHKEITEN

### Hohe Priorität
| # | Thema | Aufwand | Risiko |
|---|-------|---------|--------|
| 1 | ViewModel einführen | Mittel | 🟡 |
| 2 | Unit Tests für CheckInAnalyzer | Niedrig | 🟢 |
| 3 | Unit Tests für BodyFocus | Niedrig | 🟢 |

### Mittlere Priorität
| # | Thema | Aufwand | Risiko |
|---|-------|---------|--------|
| 4 | ExerciseDefinitions in DB | Hoch | 🟡 |
| 5 | Mehr Keywords in CheckInAnalyzer | Niedrig | 🟢 |
| 6 | Warmup-Timer pro Item | Niedrig | 🟢 |

### Nice-to-Have
| # | Feature | Beschreibung |
|---|---------|--------------|
| 7 | Plan-Varianten | A/B-Auswahl vor Start |
| 8 | Streaks | Gamification für Konsistenz |
| 9 | Kalender-Integration | Zeitfenster aus Kalender |

---

## 📱 APP-FLOW ZUSAMMENFASSUNG

```
App Start
    │
    ▼
┌─────────────┐
│  Splash     │
└──────┬──────┘
       │
       ▼
┌─────────────┐    Kein Profil    ┌─────────────┐
│  Dashboard  │◀─────────────────│  Onboarding │
└──────┬──────┘                  └─────────────┘
       │
       │ "Training starten"
       ▼
┌──────────────────────────────────────────────┐
│            TrainingFlowScreen                │
│                                              │
│  1. TYPE_SELECTION                           │
│     └─ CrossFit / Strength / Basics / Home   │
│                                              │
│  2. CHECK_IN                                 │
│     ├─ Stimmung eingeben (Freitext)          │
│     ├─ Körperteile wählen (BodySelector3D)   │
│     └─ START → Plan wird generiert           │
│                                              │
│  3. WORKOUT                                  │
│     ├─ Warmup-Karte                          │
│     ├─ Übungs-Karten (mit Timer, Sets)       │
│     ├─ Cooldown-Karte                        │
│     └─ "Training abschließen"                │
│         └─ DB speichern + Profil kalibrieren │
└──────────────────────────────────────────────┘
```

---

*Dieses Dokument wurde automatisch generiert und spiegelt den aktuellen Stand nach den Refactorings wider.*


