package com.fantasyfoodplanner.logic

import android.content.Context
import com.fantasyfoodplanner.data.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * SICHERHEITSSYSTEM – VALIDIERUNG
 * Prüft, ob die bestehende Logik korrekt angewendet wurde.
 */
object TrainingValidator {

    /**
     * Validiert ein Lebensmittel/Rezept gegen den aktuellen Trainings-Tab (Pre/Post/Regen).
     */
    fun validate(item: Any, tab: String, type: NutritionTrainingType): Boolean {
        val tags = when (item) {
            is FoodItem -> PerformanceTagEngine.computeTags(item.caloriesPer100g, item.proteinPer100g, item.carbsPer100g, item.fatPer100g, item.vitaminC, item.potassium, item.magnesium, item.iron)
            is Recipe -> PerformanceTagEngine.computeTags(item.kcal.toDouble(), item.protein, item.carbs, item.fat)
            is Product -> PerformanceTagEngine.computeTags(item.kcal.toDouble(), item.protein, item.carbs, item.fat)
            else -> emptyList()
        }
        
        return when(tab.lowercase()) {
            "pre" -> tags.contains("pre_workout")
            "post" -> tags.contains("post_workout")
            "regen" -> tags.contains("regeneration") || tags.contains("Elektrolyt")
            else -> true
        }
    }

    fun runSafetyValidation(
        context: Context,
        workout: WorkoutEntry,
        exercisesWithHistory: Map<ExerciseWithSets, List<ExerciseWithSets>>,
        allWorkouts: List<WorkoutEntry>
    ) {
        val sb = StringBuilder()
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
        
        sb.append("------------------------------------------------------------\n")
        sb.append("[SESSION VALIDATION]                         | Bedeutet: Abschluss-Check einer gespeicherten Trainingseinheit.\n")
        sb.append("Timestamp: $timestamp               | Zeitpunkt der Validierung.\n")
        sb.append("Session ID: ${workout.id.take(15)}               | Eindeutige Trainings-ID.\n")

        var allPass = true
        val consistency = TrainingConsistencyCalculator.calculate(allWorkouts)
        val issues = mutableListOf<String>()

        exercisesWithHistory.forEach { (current, history) ->
            val log = current.log
            val sets = current.sets
            val type = ExerciseLogic.getExerciseType(log.exerciseName)
            val success = log.wasSuccessful && sets.isNotEmpty() && sets.all { it.setSuccess }
            
            // Check Plateau Rules
            val limitActive = ExerciseLogic.isStructuralLimitReached(history)
            val progressionEligible = success && !limitActive
            
            // Validate if scoring was done correctly
            val expectedScore = ExerciseLogic.calculateScore(log, sets)
            val scoringCorrect = Math.abs(log.scoreValue - expectedScore) < 0.01

            if (!scoringCorrect) {
                allPass = false
                issues.add("Scoring error in ${log.exerciseName}")
            }

            // Progression check block
            sb.append("\n──────────────── Exercise Info ─────────────\n")
            sb.append("Exercise: ${log.exerciseName.padEnd(35)} | Name der analysierten Übung.\n")
            sb.append("Exercise ID: ${log.id.take(15).padEnd(31)} | Interne Referenz-ID.\n")
            sb.append("Score Type: ${type.name.padEnd(33)} | Bewertungsart (Gewicht/Zeit/Eigengewicht).\n")
            val scoreStr = when(type) {
                ExerciseType.WEIGHTED -> "${log.weightKg}kg"
                ExerciseType.BODYWEIGHT -> "Level ${log.difficultyLevel}"
                ExerciseType.TIME -> "${log.timeTargetSeconds}s"
            }
            sb.append("Current Score: ${scoreStr.padEnd(30)} | Erbrachte Leistung in dieser Einheit.\n")
            sb.append("Sets Planned: ${log.plannedSets.toString().padEnd(31)} | Geplante Sätze laut Trainingsplan.\n")
            sb.append("Sets Successful: ${log.actualSetsDone}/${log.plannedSets}                        | Erfolgsquote dieser Einheit.\n")
            sb.append("Success: ${success.toString().uppercase().padEnd(36)} | Ob Übung als vollständig erfolgreich gilt.\n")

            sb.append("\n──────────────── Consistency Check ──────────\n")
            sb.append("Streak Active: ${if(consistency.currentStreak > 0) "YES" else "NO"}                         | Trainingsserie ohne Unterbrechung.\n")
            sb.append("Pause Phase Detected: ${if(consistency.isPausePhaseActive) "YES" else "NO"}                  | 4+ Tage Unterbrechung erkannt?\n")
            sb.append("Activity Window (7d): ${consistency.trainingDaysLast7.toString().padEnd(20)} | Trainingshäufigkeit letzte 7 Tage.\n")
            sb.append("Activity Window (30d): ${consistency.trainingDaysLast30.toString().padEnd(19)} | Trainingshäufigkeit letzte 30 Tage.\n")

            sb.append("\n──────────────── Plateau Analysis ───────────\n")
            val fail3 = history.take(3).count { h -> !h.log.wasSuccessful || !h.sets.all { it.setSuccess } } >= 3
            val recentTen = history.take(10)
            val rate = if (recentTen.isNotEmpty()) recentTen.count { it.log.wasSuccessful }.toDouble() / recentTen.size else 1.0
            
            sb.append("3-Fail Rule: ${if(fail3) "YES" else "NO".padEnd(32)} | 3 Misserfolge hintereinander?\n")
            sb.append("Success Rate (last 10): ${(rate * 100).toInt()}%".padEnd(37) + " | Erfolgsquote letzte 10 Einheiten.\n")
            sb.append("Stagnation (5 sessions): ${if(limitActive) "YES" else "NO".padEnd(28)} | Keine Leistungssteigerung über 5 Sessions?\n")

            sb.append("\n──────────────── Progression Decision ───────\n")
            sb.append("Eligible for Progression: ${if(progressionEligible) "YES" else "NO".padEnd(27)} | Darf gesteigert werden?\n")
            val adj = when(type) {
                ExerciseType.WEIGHTED -> if(progressionEligible) "+2.5kg" else "FREEZE"
                ExerciseType.BODYWEIGHT -> if(progressionEligible) "+Sets/Level" else "FREEZE"
                ExerciseType.TIME -> if(progressionEligible) "+30s" else "FREEZE"
            }
            sb.append("Applied Adjustment: ${adj.padEnd(31)} | Tatsächlich angewendete Änderung.\n")
        }

        sb.append("\n──────────────── Final Validation ───────────\n")
        if (allPass) {
            sb.append("Validation Result: PASS                      | Gesamtergebnis der Systemprüfung.\n")
            sb.append("Action Taken: Progression Confirmed          | Systemreaktion auf Ergebnis.\n")
        } else {
            sb.append("Validation Result: WARNING                   | Sicherheitsabweichung erkannt.\n")
            sb.append("Issue Detected:\n")
            issues.forEach { sb.append("  - $it | Verstoß gegen Regelwerk.\n") }
            sb.append("Automatic Protection:\n")
            sb.append("  - Progression frozen next session          | Schutzmaßnahme angewendet.\n")
        }
        sb.append("------------------------------------------------------------")

        SystemLogger.log(context, sb.toString())
    }
}
