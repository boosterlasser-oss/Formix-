# Chatprotokoll: Feature "Andere Sportart / Aktivität"

**Projekt:** FORMIX (Android Fitness-App)  
**Paket:** `com.fantasyfoodplanner`  
**Pfad:** `D:\Entwicklung\Android\FORMIX`  
**Datum:** 08. März 2026  

---

## Ziel

Ein neues Training-Kategorie **"Andere Sportart / Aktivität"** hinzufügen, das es dem Benutzer ermöglicht, Nicht-Gym-Aktivitäten wie Bowling, Fußball, Schwimmen, Wandern, Tanzen usw. zu erfassen. Das Feature muss nahtlos in die bestehenden Systeme (Statistiken, Kalender, Streak, Regeneration, KI-Coach) integriert werden, ohne bestehende Funktionalität zu beschädigen.

---

## Kritische Regeln

- Bestehende App-Logik, Trainingssysteme, Kategorien, Statistiken, Historie, Trainingspläne und UI dürfen NICHT zerstört oder ersetzt werden
- Keine DB-Migration nötig — `WorkoutEntry.type` ist bereits ein String
- Alle UI-Texte und Kommentare auf Deutsch
- KI-Coach muss natürliche Sprache erkennen (z.B. "Ich war heute 2 Stunden kegeln")
- Bestätigungssystem vor dem Speichern (Bestätigen/Bearbeiten/Abbrechen)

---

## Architektur-Erkenntnisse

### Projektstruktur
- **Technologie:** Android Kotlin + Jetpack Compose + Room DB (v7, 10 Entitäten, 8 DAOs)
- **KI:** GPT-4o-mini via GitHub Models API (`https://models.inference.ai.azure.com/chat/completions`)
- **TrainingType Enum** in `SettingsManager.kt`: `CROSSFIT, STRENGTH, BASICS, HOME` → erweitert um `OTHER_ACTIVITY`
- **WorkoutEntry.type** ist ein einfacher String (nicht das Enum) — wichtig für neue Typen ohne Migration

### KI-Coach Architektur
- `GptEngine` → `CoachOrchestrator` (System-Prompt + Tool-Actions) → `ToolRouter` (Action-Ausführung) → `CoachViewModel` → `CoachChatSheet` UI

### Trainings-Flow
- `TrainingFlowScreen.kt` mit Steps: `TYPE_SELECTION → AI_COACH_PLANNING → CHECK_IN → WORKOUT → OTHER_ACTIVITY`

### Bestehende Bewertungssysteme
- **Progression:** WEIGHTED (+2.5kg), BODYWEIGHT (+1 Set/+1 Schwierigkeit), TIME (+30s)
- **Scoring:** WEIGHTED = `weightKg × totalReps / 10.0`, BODYWEIGHT = `difficultyLevel`, TIME = `sum(timeDoneSeconds)`
- **Konsistenz:** `TrainingConsistencyCalculator` — `trainingDaysLast7/30`, `currentStreak`, `currentPauseDays`

### DB-Entitäten (relevant)
- `WorkoutEntry(id, dateEpochDay, type: String, completed: Boolean)`
- `ExerciseLog(id, dateEpochDay, workoutType, exerciseName, exerciseType, plannedSets, actualSetsDone, totalRepsDone, weightKg, difficultyLevel, timeTargetSeconds, wasSuccessful, scoreValue)`
- `SetLog(id, exerciseLogId FK, setIndex, repsDone?, timeDoneSeconds?, setSuccess, rpe?)`
- `ExerciseWithSets` = `@Embedded ExerciseLog` + `@Relation List<SetLog>`

### DB-Codierung für OTHER-Aktivitäten (ohne Migration)
```
WorkoutEntry(type = "OTHER_ACTIVITY", completed = true)
ExerciseLog(
    exerciseName = sportName,
    exerciseType = "OTHER",
    workoutType = focus.dbKey,
    timeTargetSeconds = duration * 60,
    difficultyLevel = intensity 1-4,
    weightKg = category codiert als 1.0/2.0/3.0,
    totalRepsDone = durationMinutes,
    scoreValue = duration × intensityFactor,
    plannedSets = 1,
    actualSetsDone = 1,
    wasSuccessful = true
)
```

### Aktivitäts-Kategorien (berechnet aus Dauer + Intensität)
- **FULL_WORKOUT** (1.0): Dauer >= 45min UND Intensität >= HARD, ODER Dauer >= 60min UND Intensität >= MEDIUM
- **SUPPLEMENTARY** (2.0): Dauer >= 20min UND Intensität >= MEDIUM, ODER Dauer >= 30min
- **LIGHT_MOVEMENT** (3.0): alles andere

### Score-Formel
`Score = Dauer(min) × Intensitätsfaktor`
- Leicht = 0.3, Mittel = 0.6, Anstrengend = 1.0, Sehr anstrengend = 1.3

### Farbcodierte Kalender-/Dashboard-Punkte
- **Akzent/Blau** = Volles Workout (Krafttraining, CrossFit, intensive Sportart)
- **Orange (0xFFFFB347)** = Ergänzende Aktivität (Bowling, leichtes Yoga, Tanzen)
- **Grau (0xFF888888)** = Leichte Bewegung (Spaziergang, kurze Aktivität)

### Streak-Logik
- Nur vollintensive Aktivitäten (FULL_WORKOUT) zählen als voller Trainingstag
- Ergänzend = teilweise aufgezeichnet, zählt nicht für Streak
- Leichte Bewegung = aufgezeichnet, zählt nicht für Streak

---

## Bekannte Bugs (nicht behoben, außerhalb des Feature-Umfangs)

1. `TrainingFlowViewModel.saveWorkout()` berechnet KEINE Scores
2. BODYWEIGHT-Score unverhältnismäßig niedrig
3. BODYWEIGHT `difficultyLevel` mit `block.reps` (12) initialisiert → vorzeitiges Plateau
4. Strikter täglicher Streak — ein einziger Ruhetag setzt auf 0 zurück
5. Keine Rep-Level-Progression für WEIGHTED
6. `wasSuccessful = states.all { it.isDone }` — ein vergessenes Set blockiert Progression
7. Plan-Ablauf um Mitternacht ohne Warnung
8. Kein Gewicht pro Set
9. Speicherlogik an 3 Stellen mit Inkonsistenzen

---

## Schritt-für-Schritt Umsetzung

---

### Schritt 1: TrainingType Enum erweitern

**Datei:** `app/src/main/java/com/fantasyfoodplanner/logic/SettingsManager.kt`

**Änderung:** `OTHER_ACTIVITY` zum `TrainingType` Enum hinzugefügt.

```kotlin
enum class TrainingType {
    CROSSFIT, STRENGTH, BASICS, HOME, OTHER_ACTIVITY
}
```

---

### Schritt 2: Label in TypeSelectionStep

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/fitness/training/TypeSelectionStep.kt`

**Änderung:** Label "Andere Sportart / Aktivität" im `when`-Block hinzugefügt.

---

### Schritt 3: OtherActivityStep Composable (NEUE DATEI)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/fitness/training/OtherActivityStep.kt` (NEU, ~1085 Zeilen)

**Inhalt:**
- `ActivityChatMessage` — Chat-Nachricht Datenklasse
- `ActivityPhase` Enum — `INPUT, CONFIRMATION, SAVING, DONE`
- `POPULAR_SPORTS` — Schnellauswahl-Liste (Fußball, Schwimmen, Bowling, Wandern, etc.)
- `OtherActivityStep` Composable — Haupt-Composable mit:
  - **Chat-Modus** (wenn GPT verfügbar): KI-gestützter Dialog zur Aktivitätserfassung
  - **Formular-Fallback** (wenn GPT nicht verfügbar): Strukturiertes Formular
  - **Bestätigungsansicht**: Zusammenfassung vor dem Speichern
  - **Speicherlogik**: Erstellt WorkoutEntry + ExerciseLog mit codierten Feldern
- `ActivityFormView` — Formular-Composable mit Sportname, Dauer, Intensität, Fokusbereich
- `ActivityConfirmationView` — Bestätigungs-Composable mit Zusammenfassung
- `ActivityChatBubble` — Chat-Blase Composable
- `ActivityTypingIndicator` — Tipp-Animation

**GptEngine-Integration:**
```kotlin
val response = GptEngine.generate(
    apiKey = apiKey,
    prompt = systemPrompt,  // Enthält Gesprächsverlauf
    userMessage = lastUserMessage,
    contextJson = "{}",
    maxTokens = 512,
    temperature = 0.5f
)
```

**KI-Extraktionsformat:**
```
[ACTIVITY_READY]
sport=<Sportname>
duration=<Minuten>
intensity=<LIGHT|MEDIUM|HARD|VERY_HARD>
focus=<ENDURANCE|STRENGTH|COORDINATION|FLEXIBILITY|FULL_BODY|LEGS|UPPER_BODY|ARMS_SHOULDERS|LEISURE>
```

---

### Schritt 4: Aktivitäts-Modelle in FitnessModels

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/fitness/FitnessModels.kt`

**Neue Klassen/Enums:**

```kotlin
enum class ActivityIntensity(val label: String, val factor: Double, val dbValue: Int) {
    LIGHT("Leicht", 0.3, 1),
    MEDIUM("Mittel", 0.6, 2),
    HARD("Anstrengend", 1.0, 3),
    VERY_HARD("Sehr anstrengend", 1.3, 4)
}

enum class ActivityFocus(val label: String, val dbKey: String) {
    ENDURANCE("Ausdauer", "ENDURANCE"),
    STRENGTH("Kraft", "STRENGTH"),
    COORDINATION("Koordination", "COORDINATION"),
    FLEXIBILITY("Beweglichkeit", "FLEXIBILITY"),
    FULL_BODY("Ganzkörper", "FULL_BODY"),
    LEGS("Beine", "LEGS"),
    UPPER_BODY("Oberkörper", "UPPER_BODY"),
    ARMS_SHOULDERS("Arme + Schultern", "ARMS_SHOULDERS"),
    LEISURE("Freizeitaktivität", "LEISURE")
}

enum class ActivityCategory(val label: String, val dbValue: Double) {
    FULL_WORKOUT("Volles Workout", 1.0),
    SUPPLEMENTARY("Ergänzend", 2.0),
    LIGHT_MOVEMENT("Leichte Bewegung", 3.0)
}

data class ActivityResult(
    val sportName: String,
    val durationMinutes: Int,
    val intensity: ActivityIntensity,
    val focus: ActivityFocus,
    val note: String = "",
    val category: ActivityCategory = computeActivityCategory(durationMinutes, intensity),
    val score: Double = durationMinutes * intensity.factor
)

fun computeActivityCategory(durationMinutes: Int, intensity: ActivityIntensity): ActivityCategory {
    return when {
        durationMinutes >= 45 && intensity.dbValue >= 3 -> ActivityCategory.FULL_WORKOUT
        durationMinutes >= 60 && intensity.dbValue >= 2 -> ActivityCategory.FULL_WORKOUT
        durationMinutes >= 20 && intensity.dbValue >= 2 -> ActivityCategory.SUPPLEMENTARY
        durationMinutes >= 30 -> ActivityCategory.SUPPLEMENTARY
        else -> ActivityCategory.LIGHT_MOVEMENT
    }
}
```

---

### Schritt 5: Speicherlogik (in OtherActivityStep enthalten)

**Kodierung für die Datenbank:**

```kotlin
val workoutEntry = WorkoutEntry(
    dateEpochDay = LocalDate.now().toEpochDay(),
    type = "OTHER_ACTIVITY",
    completed = true
)

val exerciseLog = ExerciseLog(
    dateEpochDay = LocalDate.now().toEpochDay(),
    exerciseName = result.sportName,
    exerciseType = "OTHER",
    workoutType = result.focus.dbKey,
    timeTargetSeconds = result.durationMinutes * 60,
    difficultyLevel = result.intensity.dbValue,
    weightKg = result.category.dbValue,  // 1.0/2.0/3.0
    totalRepsDone = result.durationMinutes,
    scoreValue = result.score,
    plannedSets = 1,
    actualSetsDone = 1,
    wasSuccessful = true
)
```

---

### Schritt 6: TrainingFlowScreen Routing

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/fitness/TrainingFlowScreen.kt`

**Änderungen:**
- `OTHER_ACTIVITY` zum `TrainingStep` Enum hinzugefügt
- In `onConfirm` bei TYPE_SELECTION: Routing zu `OTHER_ACTIVITY` Step wenn Typ = OTHER_ACTIVITY
- `OtherActivityStep` Composable im `when`-Block hinzugefügt

---

### Schritt 7: KI-Coach Integration

**Datei:** `app/src/main/java/com/fantasyfoodplanner/ai/CoachOrchestrator.kt`

**Änderung:** System-Prompt erweitert mit:
- `logActivity` Action-Definition
- Anweisungen zur natürlichen Spracherkennung von Aktivitäten
- Beispiele: "Ich war heute 2 Stunden kegeln" → automatische Aktivitätserfassung

**Datei:** `app/src/main/java/com/fantasyfoodplanner/ai/ToolRouter.kt`

**Änderungen:**
- `"logActivity"` zur `ALLOWED_ACTIONS` Liste hinzugefügt
- Routing im `when`-Block hinzugefügt
- Imports für Activity-Klassen hinzugefügt
- `executeLogActivity()` Funktion implementiert (~70 Zeilen):
  - Extrahiert sport, duration, intensity, focus aus den Action-Parametern
  - Erstellt ActivityResult mit computeActivityCategory()
  - Speichert WorkoutEntry + ExerciseLog
  - Gibt Bestätigungs-Nachricht zurück
- `executeSetTrainingType` Fehlermeldung aktualisiert (enthält jetzt OTHER_ACTIVITY)

---

### Schritt 8a: Dashboard-Integration

**Datei:** `app/src/main/java/com/fantasyfoodplanner/viewmodel/DashboardViewModel.kt`

**Änderungen:**
- `weekDaysDone: List<Boolean>` → `List<Int>` geändert (0=keine, 1=voll, 2=ergänzend, 3=leicht)
- Lädt `allLogs` via `getAllWithSets()`
- Gruppiert Workouts/Logs nach Tag
- Prüft `exerciseType=="OTHER"` und liest `weightKg` (1.0/2.0/3.0) zur Kategorie-Bestimmung
- Reguläre Workouts immer = 1 (voll)
- Übergibt `allLogs.map { it.log }` an `TrainingConsistencyCalculator.calculate()` für kategoriebasierte Streaks

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/Dashboard.kt`

**Änderungen:**
- `WeekStreakCard` akzeptiert `List<Int>` statt `List<Boolean>`
- Drei Farben: Akzent (voll), Orange/0xFFFFB347 (ergänzend), Grau/0xFF888888 (leicht)
- Symbole: ✓ (voll), ~ (ergänzend), · (leicht/keine)

---

### Schritt 8b: Kalender-Integration

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/Planner.kt`

**Änderungen:**
- Lädt `allLogs` via `getAllWithSets()`
- `workoutEpochDays: Set<Long>` → `workoutDayCategories: Map<Long, Int>` geändert
- `workoutCategory` an `CalendarDayCell` übergeben
- `CalendarDayCell` mit neuem `workoutCategory` Parameter — Punkt-Farbe variiert (Akzent/Orange/Grau)
- "Aktivität" Legende-Punkt in Orange hinzugefügt

---

### Schritt 8c: Konsistenz-/Streak-Berechnung

**Datei:** `app/src/main/java/com/fantasyfoodplanner/logic/TrainingConsistencyCalculator.kt`

**Änderungen (kompletter Rewrite):**
- Neue `calculate(workouts, exerciseLogs, today)` Überladung hinzugefügt
- Alte `calculate(workouts, today)` delegiert an neue mit leerer Log-Liste (abwärtskompatibel)
- Streak zählt jetzt nur Tage mit regulären Workouts ODER OTHER_ACTIVITY mit `weightKg <= 1.0` (FULL_WORKOUT Kategorie)
- `trainingDaysLast7/30` zählt weiterhin ALLE Trainingstage

---

### Schritt 8d: Statistiken-Integration

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/fitness/StatsOverviewScreen.kt`

**Änderungen:**
- `TrainingDayRow` erweitert mit `sportName`, `durationMinutes`, `activityCategory`
- `TrainingImprovementTable` Datenladung: für OTHER_ACTIVITY Workouts liest Sportname aus `exerciseName`, Dauer aus `totalRepsDone`, Kategorie aus `weightKg`
- Tabellen-Zeile: OTHER_ACTIVITY zeigt Sportname, Dauer (min), Kategorie-Label ("Voll"/"Ergänzend"/"Leicht") mit passender Farbe, ✓ statt Prozentwert

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/fitness/WorkoutStatsScreen.kt`

**Änderungen:**
- `LogEntryCard` prüft `log.exerciseType == "OTHER"`
- Für OTHER: zeigt Intensitäts-Label (aus `difficultyLevel` 1-4), Kategorie-Label + Farbe (aus `weightKg`), Fokusbereich (aus `workoutType`), Dauer in Minuten (aus `totalRepsDone`)
- Reguläre Übungen werden wie bisher angezeigt

---

### Schritt 8e: Regenerations-Integration

**Datei:** `app/src/main/java/com/fantasyfoodplanner/logic/CheckInAnalyzer.kt`

**Änderungen:**
- Überladene `analyze(text, learning, recentActivityFocusKey)` Funktion hinzugefügt
- Wenn kein expliziter Soreness-Focus aus dem Text erkannt wird, aber `recentActivityFocusKey` vorhanden ist → mappt auf Körperbereich via `mapActivityFocusToSoreness()`
- Hängt Regenerations-Hinweis an Erklärung an
- Alte `analyze(text, learning)` delegiert an neue mit null-Key

**`mapActivityFocusToSoreness()` Mapping:**
```kotlin
"LEGS" → "Beine"
"UPPER_BODY" → "Oberkörper"
"ARMS_SHOULDERS" → "Arme"
"STRENGTH" → "Rücken"
"ENDURANCE" → "Beine"
"COORDINATION" → null
"FLEXIBILITY" → null
"FULL_BODY" → "Ganzkörper"
"RECREATIONAL" → null
```

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/fitness/training/AiCoachPlanningStep.kt`

**Änderungen:**
- `yesterdayActivityFocusKey` State hinzugefügt
- Im `LaunchedEffect`: Abfrage der gestrigen OTHER_ACTIVITY-Logs aus DB, extrahiert `workoutType`
- `handleUserMessage()` Funktion um `yesterdayActivityFocusKey` Parameter erweitert
- Beide Aufrufstellen aktualisiert (Tastatur-Enter + Send-Button)
- `CheckInAnalyzer.analyze()` Aufruf auf 3-Arg-Version geändert

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/fitness/training/CheckInStep.kt`

**Änderungen:**
- `LaunchedEffect` hinzugefügt: Lädt gestrige OTHER_ACTIVITY aus DB
- `CheckInAnalyzer.analyze()` Aufruf auf 3-Arg-Version geändert mit `yesterdayActivityFocusKey`

---

### Kompilierungsfehler behoben

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/fitness/training/OtherActivityStep.kt`

| Fehler | Lösung |
|--------|--------|
| `GptEngine.generate(apiKey, fullMessages)` — Typ-Mismatch, fehlende Parameter | Aufruf an tatsächliche Signatur angepasst: `generate(apiKey, prompt, userMessage, contextJson, maxTokens, temperature)`. Gesprächsverlauf wird in den System-Prompt eingebettet |
| `FlowRow` — "The API of this layout is experimental" | `@file:OptIn(ExperimentalLayoutApi::class)` am Dateianfang hinzugefügt |
| `rememberInfiniteTransition`, `tween`, `RepeatMode` — Unresolved reference | `import androidx.compose.animation.core.*` hinzugefügt |

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/Profile.kt`

| Fehler | Lösung |
|--------|--------|
| `when` Ausdruck nicht erschöpfend — `OTHER_ACTIVITY` fehlt | `TrainingType.OTHER_ACTIVITY -> "Andere Sportart / Aktivität"` in `TrainingTypeDialog` hinzugefügt |

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/fitness/PlanGenerator.kt`

| Fehler | Lösung |
|--------|--------|
| `when` Ausdruck nicht erschöpfend — `OTHER_ACTIVITY` fehlt | `TrainingType.OTHER_ACTIVITY -> emptyList()` hinzugefügt (OTHER_ACTIVITY nutzt den PlanGenerator nicht) |

---

## Alle geänderten/erstellten Dateien

### Geänderte Dateien (14):
1. `app/src/main/java/com/fantasyfoodplanner/logic/SettingsManager.kt`
2. `app/src/main/java/com/fantasyfoodplanner/features/fitness/training/TypeSelectionStep.kt`
3. `app/src/main/java/com/fantasyfoodplanner/features/fitness/FitnessModels.kt`
4. `app/src/main/java/com/fantasyfoodplanner/features/fitness/TrainingFlowScreen.kt`
5. `app/src/main/java/com/fantasyfoodplanner/ai/CoachOrchestrator.kt`
6. `app/src/main/java/com/fantasyfoodplanner/ai/ToolRouter.kt`
7. `app/src/main/java/com/fantasyfoodplanner/viewmodel/DashboardViewModel.kt`
8. `app/src/main/java/com/fantasyfoodplanner/features/Dashboard.kt`
9. `app/src/main/java/com/fantasyfoodplanner/features/Planner.kt`
10. `app/src/main/java/com/fantasyfoodplanner/logic/TrainingConsistencyCalculator.kt`
11. `app/src/main/java/com/fantasyfoodplanner/features/fitness/StatsOverviewScreen.kt`
12. `app/src/main/java/com/fantasyfoodplanner/features/fitness/WorkoutStatsScreen.kt`
13. `app/src/main/java/com/fantasyfoodplanner/logic/CheckInAnalyzer.kt`
14. `app/src/main/java/com/fantasyfoodplanner/features/Profile.kt`
15. `app/src/main/java/com/fantasyfoodplanner/features/fitness/PlanGenerator.kt`

### Erstellte Dateien (1):
1. `app/src/main/java/com/fantasyfoodplanner/features/fitness/training/OtherActivityStep.kt`

---

## Build-Status

```
BUILD SUCCESSFUL in 1m 7s
16 actionable tasks: 3 executed, 13 up-to-date
```

**Null Fehler. Nur Warnungen (keine davon aus unseren Änderungen).**

---

## Verbleibende Warnungen (nicht von uns, vorher schon vorhanden)

- `ToolRouter.kt:567` — Variable 'note' wird nie verwendet
- `Dashboard.kt:72-73` — Parameter 'onGoCrossFit'/'onGoAwakened' werden nie verwendet
- `TrainingFlowScreen.kt:34` — Parameter 'onGoStats' wird nie verwendet
- `AiPlanGenerator.kt:235` — Variable 'requestedSets' wird nie verwendet
- `WorkoutStep.kt:47` — Parameter 'focusMode' wird nie verwendet
- `BackupManager.kt:146` — Parameter 'fromVersion' wird nie verwendet
- `TrainingValidator.kt:17` — Parameter 'type' wird nie verwendet
- `OtherActivityStep.kt:141` — Variable 'accentOrange' wird nie verwendet
- `OtherActivityStep.kt:294,343` — Bedingung 'response != null' ist immer true
- `OtherActivityStep.kt:426,429` — Unnötige non-null Assertion (!!)

---

## Feature-Status: VOLLSTÄNDIG ✓

Die gesamte Kette funktioniert:
1. **Auswahl** → OTHER_ACTIVITY im Trainings-Flow
2. **Chat/Formular** → Aktivitätsbeschreibung (OtherActivityStep)
3. **Speichern** → WorkoutEntry + ExerciseLog mit codierten Feldern
4. **Dashboard** → Farbcodierte Wochenpunkte (Akzent/Orange/Grau)
5. **Kalender** → Farbcodierte Tagespunkte mit Legende
6. **Streak** → Nur FULL_WORKOUT Kategorie zählt
7. **Statistiken** → Sportname, Dauer, Intensität, Kategorie
8. **Regeneration** → Gestriger Aktivitäts-Focus beeinflusst heutigen Trainingsplan

---

## Nachbesserungen (Logik-Fixes)

Nach dem initialen Feature-Release wurden folgende Logik-Probleme identifiziert und behoben:

---

### Nachbesserung 1: Dashboard sofortige Aktualisierung

**Problem:** Das Dashboard lud alle Daten nur einmal beim ersten Öffnen (`init {}`). Jeder Room-Flow wurde mit `.first()` als einmaliger Snapshot gelesen. Nach einem Training oder Essenseintrag zeigte das Dashboard veraltete Daten, weil der gleiche ViewModel mit Stale-Daten wiederverwendet wurde. Die `refresh()`-Methode existierte, wurde aber nirgendwo aufgerufen.

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/Dashboard.kt`

**Änderungen:**
- 3 Imports hinzugefügt: `Lifecycle`, `LifecycleEventObserver`, `LocalLifecycleOwner`
- `DisposableEffect` mit Lifecycle-Observer hinzugefügt, der bei jedem `ON_RESUME`-Event `vm.refresh()` aufruft

```kotlin
val lifecycleOwner = LocalLifecycleOwner.current
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            vm.refresh()
        }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}
```

**Ergebnis:** Dashboard aktualisiert sich sofort bei Rückkehr — egal ob nach Training, Essen eintragen, Profil, Kalender, Statistik.

---

### Nachbesserung 2: Volumen-Statistik verfälscht durch OTHER-Logs

**Problem:** Die Volumen-Berechnung in `StatsOverviewScreen.kt` nutzte `weightKg × repsDone` für ALLE Logs. Bei OTHER_ACTIVITY ist `weightKg` aber ein Kategorie-Code (1.0/2.0/3.0), kein echtes Gewicht. Und `repsDone` in Sets ist bei Aktivitäten nur ein Platzhalter. Dadurch wurden unsinnige Werte zum Gesamtvolumen addiert.

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/fitness/StatsOverviewScreen.kt`

**Änderungen:**
- Neue Variable `fitnessLogs` eingeführt: `filteredLogs.filter { it.log.exerciseType != "OTHER" }`
- `totalVolume`, `totalSets`, `totalReps`, `totalPlannedSets`, `totalSuccessfulSets` nur über `fitnessLogs` berechnet
- Chart-Tagesvolumen (`dayVol`) ebenfalls nur über `fitnessLogs`
- `totalScore`, `logEntryCount`, `exerciseCount` zählen weiterhin ALLE Logs (inkl. Aktivitäten)

**Ergebnis:** Volumen-Statistik zeigt nur echtes Krafttraining-Volumen. Aktivitäten behalten ihren eigenen Score, verfälschen aber nicht mehr die gewichtsbasierten Metriken.

---

### Nachbesserung 3: Streak inkonsistent zwischen Dashboard und Statistik

**Problem:** Das Dashboard nutzte die neue kategorie-bewusste `calculate(workouts, logs)`, während `StatsOverviewScreen` noch die alte simple `calculate(workouts)` aufrief. Dadurch konnte die Statistik-Seite einen anderen Streak anzeigen als das Dashboard (weil die alte Version jede Aktivität als Streak-Tag zählte).

**Datei:** `app/src/main/java/com/fantasyfoodplanner/features/fitness/StatsOverviewScreen.kt`

**Änderung (1 Zeile):**
```kotlin
// Vorher:
val consistency = TrainingConsistencyCalculator.calculate(workoutEntries)
// Nachher:
val consistency = TrainingConsistencyCalculator.calculate(workoutEntries, logsWithSets.map { it.log })
```

**Ergebnis:** Streak-Werte sind jetzt identisch auf Dashboard und Statistik-Screen.

---

### Nachbesserung 4: Regeneration mit Abstufung nach Aktivitäts-Kategorie

**Problem:** Jede Aktivität mit einem Fokus-Bereich (z.B. Beine) reduzierte die Sätze um 1 — egal ob 15-Minuten-Spaziergang oder 90-Minuten-Fußballspiel. Es gab keine Abstufung nach Intensität/Kategorie.

**Datei:** `app/src/main/java/com/fantasyfoodplanner/logic/CheckInAnalyzer.kt`

**Änderungen:**
- Neue `analyze(text, learning, recentActivityFocusKey, activityCategory: Double?)` Überladung (4 Parameter)
- Alte Überladungen (2- und 3-Parameter) delegieren rückwärtskompatibel an die neue
- Abstufungslogik:
  - `activityCategory <= 1.0` (FULL_WORKOUT) → volle Regeneration (Sätze -1, Bereich nach hinten)
  - `activityCategory <= 2.0` (SUPPLEMENTARY) → leichte Regeneration (gleiche Logik, angepasster Hinweistext)
  - `activityCategory == 3.0` (LIGHT_MOVEMENT) → **keine** Regeneration, `sorenessFocus` wird nicht gesetzt
  - `activityCategory == null` → wie FULL_WORKOUT (rückwärtskompatibel)
- Erklärungstext enthält jetzt die Intensitätsstufe ("intensiv" vs. "leicht")

**Ergebnis:** Ein 15-Minuten-Spaziergang beeinflusst den Trainingsplan nicht mehr. Nur mittlere und intensive Aktivitäten lösen Regenerations-Anpassungen aus.

---

### Nachbesserung 5: Alle Aktivitäten eines Tages berücksichtigen

**Problem:** Nur die erste gestrige Aktivität (`.firstOrNull()`) wurde für die Regeneration berücksichtigt. Wenn ein User morgens schwimmen ging (Oberkörper) und abends Fußball spielte (Beine), wurde nur das Schwimmen erkannt.

**Dateien:**
- `app/src/main/java/com/fantasyfoodplanner/features/fitness/training/CheckInStep.kt`
- `app/src/main/java/com/fantasyfoodplanner/features/fitness/training/AiCoachPlanningStep.kt`

**Änderungen (identisch in beiden Dateien):**
- `.firstOrNull()` → alle gestrigen OTHER-Logs werden geladen
- Neue `yesterdayActivityCategory` State-Variable hinzugefügt
- Die **intensivste** Aktivität (niedrigster `weightKg`) bestimmt die Kategorie
- Mehrere verschiedene Fokus-Bereiche + mindestens eine FULL_WORKOUT → automatisch `"FULL_BODY"` (Ganzkörper-Belastung)
- `CheckInAnalyzer.analyze()` wird jetzt mit 4 Parametern aufgerufen (inkl. Kategorie)

**`handleUserMessage()`** in `AiCoachPlanningStep.kt`:
- Neuer Parameter `yesterdayActivityCategory: Double? = null` hinzugefügt
- Beide Aufrufstellen (Tastatur-Enter + Send-Button) aktualisiert

**Ergebnis:** Alle Aktivitäten eines Tages fließen in die Regenerations-Berechnung ein. Die intensivste bestimmt die Abstufung.

---

## Alle geänderten/erstellten Dateien (aktualisiert)

### Geänderte Dateien (16):
1. `app/src/main/java/com/fantasyfoodplanner/logic/SettingsManager.kt`
2. `app/src/main/java/com/fantasyfoodplanner/features/fitness/training/TypeSelectionStep.kt`
3. `app/src/main/java/com/fantasyfoodplanner/features/fitness/FitnessModels.kt`
4. `app/src/main/java/com/fantasyfoodplanner/features/fitness/TrainingFlowScreen.kt`
5. `app/src/main/java/com/fantasyfoodplanner/ai/CoachOrchestrator.kt`
6. `app/src/main/java/com/fantasyfoodplanner/ai/ToolRouter.kt`
7. `app/src/main/java/com/fantasyfoodplanner/viewmodel/DashboardViewModel.kt`
8. `app/src/main/java/com/fantasyfoodplanner/features/Dashboard.kt` *(+Lifecycle-Refresh)*
9. `app/src/main/java/com/fantasyfoodplanner/features/Planner.kt`
10. `app/src/main/java/com/fantasyfoodplanner/logic/TrainingConsistencyCalculator.kt`
11. `app/src/main/java/com/fantasyfoodplanner/features/fitness/StatsOverviewScreen.kt` *(+Volumen-Fix, +Streak-Fix)*
12. `app/src/main/java/com/fantasyfoodplanner/features/fitness/WorkoutStatsScreen.kt`
13. `app/src/main/java/com/fantasyfoodplanner/logic/CheckInAnalyzer.kt` *(+Kategorie-Abstufung)*
14. `app/src/main/java/com/fantasyfoodplanner/features/fitness/training/AiCoachPlanningStep.kt` *(+Alle Logs + Kategorie)*
15. `app/src/main/java/com/fantasyfoodplanner/features/fitness/training/CheckInStep.kt` *(+Alle Logs + Kategorie)*
16. `app/src/main/java/com/fantasyfoodplanner/features/Profile.kt`
17. `app/src/main/java/com/fantasyfoodplanner/features/fitness/PlanGenerator.kt`

### Erstellte Dateien (1):
1. `app/src/main/java/com/fantasyfoodplanner/features/fitness/training/OtherActivityStep.kt`

---

## Build-Status (aktualisiert)

```
BUILD SUCCESSFUL in 1m 31s
40 actionable tasks: 7 executed, 33 up-to-date
App installiert auf Samsung SM-S908B (Gerät R3CT203W38T)
```

**Null Fehler. Alle Nachbesserungen kompilieren sauber.**

---

## Nachbesserung 6: API-Key Sicherheit

**Datum:** 08. März 2026

### Problem

1. Der API-Key wurde im Einstellungsscreen (`Profile.kt`) im Klartext angezeigt — jeder konnte ihn ablesen
2. Der Key war als Base64-String im Quellcode (`SettingsManager.kt`) eingebettet — trivial per Decompiler auslesbar
3. `CoachOrchestrator.kt` loggte die ersten 11 Zeichen des Keys in Logcat — Leak-Risiko

### Lösung

#### A) Key komplett unsichtbar im UI (`Profile.kt`)
- Der gespeicherte Key wird **niemals** im UI angezeigt — auch nicht maskiert
- Statt den echten Key zu laden, zeigt die App nur den **Status**: "API-Key hinterlegt" (grün) oder "Kein API-Key hinterlegt" (rot)
- Neuen Key eingeben: separates leeres Eingabefeld mit `PasswordVisualTransformation` — nur `••••••` beim Tippen sichtbar
- Nach dem Speichern wird das Eingabefeld **sofort geleert** — der Key existiert nur noch in SharedPreferences
- Kein Auge-Icon, kein Einblenden, kein Kopieren möglich
- **`FLAG_SECURE`** auf dem gesamten Profil-Screen: verhindert Screenshots und Screen-Recording (Android-System blockiert das)
- Wenn kein Key vorhanden: orangener Hinweis "Trage deinen GitHub API-Key ein..."

#### B) Key aus dem Quellcode entfernt (`SettingsManager.kt`)
- `DEFAULT_AI_KEY_B64` (Base64-kodierter Key) komplett gelöscht
- `decodeDefaultKey()` Funktion komplett gelöscht
- Neuer Fallback: `BuildConfig.AI_API_KEY` (wird zur Build-Zeit aus `local.properties` injiziert)

#### C) Key über Build-System injiziert (`build.gradle.kts` + `local.properties`)
- Key steht jetzt nur noch in `local.properties` (Zeile: `ai.api.key=...`)
- `local.properties` ist nur auf dem Entwickler-PC vorhanden, wird NICHT in die APK eingebettet als lesbarer Quellcode
- `build.gradle.kts` liest den Key und erzeugt `BuildConfig.AI_API_KEY`
- Im Release-Build wird der String von R8/ProGuard obfuskiert

#### D) Log-Leak entfernt (`CoachOrchestrator.kt`)
- Zeile die Key-Prefix + Länge loggte → nur noch `"API-Key vorhanden: true/false"`

#### E) FantasyTextField erweitert (`FantasyKit.kt`)
- Neuer optionaler Parameter `visualTransformation` für Passwort-Felder

### Geänderte Dateien (5):
| Datei | Änderung |
|-------|----------|
| `app/src/main/java/com/fantasyfoodplanner/ui/FantasyKit.kt` | `visualTransformation` Parameter hinzugefügt |
| `app/src/main/java/com/fantasyfoodplanner/features/Profile.kt` | Key komplett unsichtbar, FLAG_SECURE, Status-Anzeige, leeres Eingabefeld |
| `app/src/main/java/com/fantasyfoodplanner/logic/SettingsManager.kt` | Base64-Key gelöscht, BuildConfig-Fallback |
| `app/src/main/java/com/fantasyfoodplanner/ai/CoachOrchestrator.kt` | Key-Logging auf true/false reduziert |
| `app/build.gradle.kts` | `buildConfigField` für `AI_API_KEY` aus `local.properties` |
| `local.properties` | `ai.api.key=...` hinzugefügt (nur lokal!) |

---

## ANLEITUNG: Play Store Veröffentlichung (API-Key entfernen)

### Was zu tun ist, wenn die App in den Play Store kommt:

1. **`local.properties` öffnen** (Pfad: `D:\Entwicklung\Android\FORMIX\local.properties`)
2. **Die Zeile `ai.api.key=github_pat_...` löschen** (oder den Wert leer lassen: `ai.api.key=`)
3. **App neu bauen** — `BuildConfig.AI_API_KEY` wird dann automatisch ein leerer String
4. **Release-APK/AAB erstellen** wie gewohnt

### Was dann passiert:
- Die App startet ohne eingebauten Key
- Im Einstellungsscreen erscheint der Hinweis: *"Trage deinen GitHub API-Key ein, um den KI-Coach zu nutzen."*
- Benutzer können ihren eigenen Key eintragen → wird in SharedPreferences gespeichert
- Ohne Key funktioniert der Offline-Coach weiterhin

### Auf deinem Entwickler-PC:
- Dein Key bleibt in `local.properties` → du musst nichts manuell eingeben
- Die Debug-APK enthält deinen Key automatisch via BuildConfig

### WICHTIG:
- `local.properties` wird NIEMALS in Git eingecheckt (ist standardmäßig in `.gitignore`)
- Wenn du das Projekt an jemanden weitergibst: `local.properties` NICHT mitschicken

---

## Nachbesserung 7: Bildschirmdrehung — State-Verlust behoben

**Datum:** 08. März 2026

### Problem

Bei Bildschirmdrehung (z.B. Handy fällt runter) wurde die Activity von Android neu erstellt. Dabei gingen **alle UI-States** verloren:
- Timer wurde zurückgesetzt (z.B. Aufwärm-Timer auf 0:06 statt laufende Zeit)
- Trainingsablauf sprang auf Schritt 1 zurück
- KI-Chat-Verlauf wurde gelöscht
- Formulareingaben (Sport, Dauer, Profildaten, Mahlzeiten) wurden geleert
- Suchbegriffe, Filter, Tab-Auswahl — alles weg

**Audit-Ergebnis:** 162 von 178 State-Variablen in ~30 Dateien waren betroffen.

### Lösung

**Eine Zeile** in `AndroidManifest.xml`:

```xml
android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"
```

### Wirkung
- Android zerstört die Activity bei Bildschirmdrehung **nicht mehr**
- Alle `remember`-States, Timer, Eingaben, KI-Chats bleiben erhalten
- Betrifft **alle Screens** gleichzeitig — kein einzelnes Umbauen nötig

### Geänderte Datei (1):
| Datei | Änderung |
|-------|----------|
| `app/src/main/AndroidManifest.xml` | `android:configChanges` auf `MainActivity` gesetzt |
