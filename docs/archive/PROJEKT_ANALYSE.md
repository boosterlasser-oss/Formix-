# 🏋️ Projekt-Analyse: Performance Planner App

**Analysedatum:** 23. Februar 2026  
**Analysiert von:** GitHub Copilot

---

## ✅ DURCHGEFÜHRTE REFACTORINGS

| # | Änderung | Status | Datei |
|---|----------|--------|-------|
| 1 | `enum class TrainingStep` | ✅ Fertig | TrainingFlowScreen.kt |
| 2 | `CheckInAnalyzer` ausgelagert | ✅ Fertig | logic/CheckInAnalyzer.kt |
| 3 | `enum class BodyFocus` + `ExerciseCategory` | ✅ Fertig | logic/BodyFocus.kt |
| 4 | Unbenutzte Funktionen entfernt | ✅ Fertig | TrainingFlowScreen.kt |
| 5 | **Cooldown im Workout-Flow** | ✅ Fertig | TrainingFlowScreen.kt |
| 6 | **Erklärbarkeit (warum welcher Modus)** | ✅ Fertig | TrainingFlowScreen.kt |

**TrainingFlowScreen.kt:** 833 Zeilen → **732 Zeilen** (~12% weniger, aber mehr Features!)

### Neue Features:
- 🧘 **Cooldown-Karten** werden jetzt im Workout-Flow angezeigt
- 💬 **Erklärung** warum ein Modus gewählt wurde (z.B. "⚡ Push-Modus aktiv! Du bist fit und motiviert.")

---

## 📁 PROJEKT-STRUKTUR

```
com.fantasyfoodplanner/
├── MainActivity.kt              → Navigation & App-Start
├── data/                        → Datenbank & Entities
│   ├── AppDb.kt                → Room Database
│   ├── Dao.kt                  → Data Access Objects
│   ├── Entities.kt             → Datenmodelle (UserProfile, WorkoutEntry, etc.)
│   ├── FoodDatabase.kt         → Ernährungs-Daten
│   └── Seeder.kt               → Initialdaten
├── features/                    → Feature-Screens
│   ├── fitness/                → 🏋️ HAUPT-FITNESS-MODUL
│   │   ├── TrainingFlowScreen.kt      → Trainingsablauf (833 Zeilen!)
│   │   ├── BodySelector3D.kt          → 3D-Körperteil-Auswahl
│   │   ├── PlanGenerator.kt           → Trainingsplan-Generierung
│   │   ├── ExerciseDefinitions.kt     → Übungsdefinitionen
│   │   ├── ExerciseDetailScreen.kt    → Übungsdetails
│   │   ├── FitnessModels.kt           → Datenmodelle
│   │   ├── StatsOverviewScreen.kt     → Statistik-Übersicht
│   │   └── WorkoutStatsScreen.kt      → Workout-Statistiken
│   ├── Dashboard.kt            → Hauptbildschirm
│   ├── Planner.kt              → Ernährungsplaner
│   └── Profile.kt              → Benutzerprofil
├── logic/                       → Business-Logik
│   ├── ExerciseLogic.kt        → Progressions-Berechnung
│   ├── SettingsManager.kt      → Einstellungen & Persistenz
│   └── TrainingValidator.kt    → Validierung
└── ui/                          → UI-Komponenten
    ├── FantasyKit.kt           → Design-System
    ├── WorkoutComponents.kt    → Workout-UI-Elemente
    └── TimerService.kt         → Timer-Hintergrund-Service
```

---

## 🔄 HAUPT-DATENFLUSS: TrainingFlowScreen

### 3-Stufen-Flow:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ TYPE_SELECTION  │ → │   CHECK_IN      │ → │    WORKOUT      │
│                 │    │                 │    │                 │
│ • CrossFit      │    │ • Stimmung      │    │ • Warmup-Karte  │
│ • Strength      │    │ • BodySelector  │    │ • Übungs-Karten │
│ • Basics        │    │ • AI-Analyse    │    │ • Cooldown      │
│ • Home          │    │ • Plan-Erzeugung│    │ • DB-Speicherung│
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

---

## ⚙️ KERN-LOGIK ANALYSE

### 1. Check-In Analyse (`analyzeCheckInDetailed`)

**Eingabe:** Freitext + LearningProfile  
**Ausgabe:** SessionMode, FocusMode, DailyModifier, SorenessFocus

| Keyword-Typ | Beispiele | Resultat |
|-------------|-----------|----------|
| Negativ | müde, kaputt, krank | RECOVERY/SHORT |
| Positiv | fit, motiviert, energie | PUSH |
| Zeitdruck | wenig zeit, schnell | SHORT |
| Schmerz | muskelkater, schmerz | RECOVERY + MOBILITY |

### 2. Session-Modi

| Modus | Sets | Reps | Gewicht | Übungen |
|-------|------|------|---------|---------|
| **PUSH** | +1 | +10% | +5% | Alle |
| **NORMAL** | Standard | Standard | Standard | Max 5 |
| **SHORT** | -1 | (-15% bei Technik) | (-5%) | Max 3 |
| **RECOVERY** | Max 2 | -30% | -30% | Max 2 |

### 3. Progressions-Logik (`ExerciseLogic.kt`)

```kotlin
// Limit-Erkennung:
- 3 Fails hintereinander → Limit
- SuccessRate < 65% über 10 Sessions → Limit
- Kein Fortschritt über 5 Sessions → Limit

// Progression:
WEIGHTED:     +2.5 kg bei Erfolg
BODYWEIGHT:   +1 Satz (max 5), dann +1 Schwierigkeit
TIME:         +30 Sekunden bei Erfolg
```

### 4. Body-Fokus → Kategorie-Mapping

```
Brust, Schultern, Trizeps  →  "push"
Rücken, Bizeps             →  "pull"
Beine, Po/Gesäß            →  "legs"
Bauch/Core, Nacken         →  "core"
Arme                       →  "push" + "pull"
```

---

## ✅ STÄRKEN DES PROJEKTS

1. **Adaptive AI-Logik**
   - Check-In-Text wird analysiert (deutsch + Emojis)
   - SessionMode passt Intensität automatisch an
   - LearningProfile kalibriert sich über Zeit

2. **3D BodySelector**
   - Lottie-Animationen für Körperteile
   - Multi-Auswahl möglich
   - Mapping zu Übungskategorien

3. **Persistenz-System**
   - Workout kann unterbrochen und fortgesetzt werden
   - FitnessPersistence speichert Tagesplan
   - Room-DB für langfristige Historie

4. **Timer-Service**
   - Hintergrund-Service mit Notification
   - Alarm bei Timer-Ende
   - Pro Übung gebunden

5. **Progressions-System**
   - Automatische Gewichts-/Sets-Steigerung
   - Limit-Erkennung bei Plateau
   - Drei Übungstypen: WEIGHTED, BODYWEIGHT, TIME

---

## ⚠️ SCHWÄCHEN & RISIKEN

### 1. **TrainingFlowScreen ist zu groß (833 Zeilen)**
   - UI, Logik und DB-Zugriffe gemischt
   - Schwer testbar
   - String-basierte Step-Maschine ("TYPE_SELECTION" etc.)

### 2. **Index-Inkonsistenz**
   - `originalIndexInPlan` vs. `index` in verschiedenen Kontexten
   - Potenzielle Bugs bei Set-State-Speicherung

### 3. **Cooldown wird übersprungen**
   - `workflowBlocks` enthält nur `warmup` + `ex`
   - Cooldowns sind im Plan, aber nicht im Kartenflow

### 4. **Fehlendes Fehler-Handling**
   - Wenn `profile == null` → kein Plan, keine Meldung
   - Stilles Versagen möglich

### 5. **Hardcoded Strings**
   - Keine String-Resources für Lokalisierung
   - Übungsnamen als Magic Strings

---

## 🔧 VERBESSERUNGSVORSCHLÄGE

### Hohe Priorität

| # | Problem | Lösung |
|---|---------|--------|
| 1 | Step als String | `enum class TrainingStep { TYPE_SELECTION, CHECK_IN, WORKOUT }` |
| 2 | 833 Zeilen | Aufteilen in ViewModel + UseCase + Screen |
| 3 | Cooldown fehlt | `workflowBlocks` um Cooldown erweitern |
| 4 | Kein Fehler-UI | Loading/Error States mit Feedback |

### Mittlere Priorität

| # | Problem | Lösung |
|---|---------|--------|
| 5 | analyzeCheckInDetailed im UI | In eigene Klasse `CheckInAnalyzer` auslagern |
| 6 | Index-Mapping | Klare Datenstruktur mit expliziten Indizes |
| 7 | Body-Fokus als Strings | `enum class BodyFocus { CHEST, BACK, LEGS, ... }` |

### Nice-to-Have

| # | Feature | Beschreibung |
|---|---------|--------------|
| 8 | Erklärbarkeit | Dem User zeigen WARUM ein Modus gewählt wurde |
| 9 | Plan-Varianten | A/B-Auswahl vor Workout-Start |
| 10 | Gamification | Streaks, Badges für konstantes Training |

---

## 📊 DATENBANK-STRUKTUR

```
┌─────────────────┐     ┌─────────────────┐
│   UserProfile   │     │  WorkoutEntry   │
├─────────────────┤     ├─────────────────┤
│ id (PK)         │     │ id (PK)         │
│ name            │     │ dateEpochDay    │
│ weightKg        │     │ type            │
│ experience      │     │ completed       │
│ goal            │     └─────────────────┘
└─────────────────┘              │
                                 │ 1:n
                                 ▼
┌─────────────────┐     ┌─────────────────┐
│  ExerciseLog    │────▶│    SetLog       │
├─────────────────┤     ├─────────────────┤
│ id (PK)         │     │ id (PK)         │
│ exerciseName    │     │ exerciseLogId   │
│ plannedSets     │     │ setIndex        │
│ actualSetsDone  │     │ repsDone        │
│ weightKg        │     │ setSuccess      │
│ wasSuccessful   │     └─────────────────┘
└─────────────────┘
```

---

## 🎯 ZUSAMMENFASSUNG

**Was die App gut macht:**
- Intelligente Trainingsplanung mit AI-Check-In
- 3D-Körperteil-Auswahl als USP
- Automatische Progression mit Limit-Erkennung
- Persistentes Workout-Resuming

**Was verbessert werden sollte:**
- Code-Architektur (MVVM/Clean Architecture)
- Typsicherheit (Enums statt Strings)
- Cooldown-Integration
- Error-Handling & UX-Feedback

**Empfehlung:**
Das Projekt hat eine solide Grundlage. Die Hauptarbeit sollte in **Refactoring** fließen, um die Wartbarkeit zu erhöhen, bevor neue Features hinzugefügt werden.

---

*Dieses Dokument wurde automatisch generiert und dient als Grundlage für weitere Diskussionen.*



