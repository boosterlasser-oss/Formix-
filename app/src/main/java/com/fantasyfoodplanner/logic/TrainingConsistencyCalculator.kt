package com.fantasyfoodplanner.logic

import com.fantasyfoodplanner.data.ExerciseLog
import com.fantasyfoodplanner.data.WorkoutEntry
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object TrainingConsistencyCalculator {

    data class ConsistencyStats(
        val trainingDaysLast7: Int,
        val trainingDaysLast30: Int,
        val currentStreak: Int,
        val currentPauseDays: Int,
        val pausePhasesThisMonth: Int,
        val isPausePhaseActive: Boolean,
        val recoveryPercent: Float = 0f // Erholungsanteil (Ruhetage / 30 * 100)
    )

    /**
     * Rückwärtskompatible Variante — zählt alle Workouts gleich.
     * Wird weiterhin von Stellen aufgerufen, die keine ExerciseLogs haben.
     */
    fun calculate(workouts: List<WorkoutEntry>, today: LocalDate = LocalDate.now()): ConsistencyStats {
        return calculate(workouts, emptyList(), today)
    }

    /**
     * Kategorie-bewusste Berechnung:
     * - Reguläre Workouts (nicht OTHER_ACTIVITY) zählen immer als volles Training.
     * - OTHER_ACTIVITY zählt nur wenn die Kategorie FULL_WORKOUT ist
     *   (weightKg-Kodierung: 1.0=FULL_WORKOUT, 2.0=SUPPLEMENTARY, 3.0=LIGHT_MOVEMENT).
     * - Für trainingDaysLast7/Last30 werden ALLE Workouts gezählt (auch ergänzend/leicht).
     * - Für den Streak zählen nur volle Trainingstage.
     */
    fun calculate(
        workouts: List<WorkoutEntry>,
        exerciseLogs: List<ExerciseLog>,
        today: LocalDate = LocalDate.now()
    ): ConsistencyStats {
        // Alle Trainingstage (für allgemeine Statistik)
        val allDates = workouts.map { LocalDate.ofEpochDay(it.dateEpochDay) }
            .distinct()
            .sortedDescending()

        val trainingDaysLast7 = allDates.count { it.isAfter(today.minusDays(7)) }
        val trainingDaysLast30 = allDates.count { it.isAfter(today.minusDays(30)) }

        // Letzte Workout-Datum (beliebiger Typ) für Pause
        val lastWorkoutDate = allDates.firstOrNull()
        val currentPauseDays = if (lastWorkoutDate == null) 0 else ChronoUnit.DAYS.between(lastWorkoutDate, today).toInt()

        // Streak: Nur volle Trainingstage zählen
        // Bestimme welche Tage als "voll" gelten
        val workoutsByDay = workouts.groupBy { it.dateEpochDay }
        val logsByDay = exerciseLogs.groupBy { it.dateEpochDay }

        val fullTrainingDates = workoutsByDay.keys.filter { epochDay ->
            val dayWorkouts = workoutsByDay[epochDay] ?: emptyList()
            val hasRegular = dayWorkouts.any { it.type != "OTHER_ACTIVITY" }
            if (hasRegular) {
                true // Reguläres Training = immer voll
            } else {
                // Nur OTHER_ACTIVITY — prüfe Kategorie über weightKg
                val otherLogs = (logsByDay[epochDay] ?: emptyList()).filter { it.exerciseType == "OTHER" }
                val bestCategory = otherLogs.minOfOrNull { it.weightKg } ?: 3.0
                bestCategory <= 1.0 // Nur FULL_WORKOUT zählt
            }
        }.map { LocalDate.ofEpochDay(it) }.toSet()

        // Streak berechnen — nur volle Trainingstage
        var streak = 0
        val lastFullDate = fullTrainingDates.maxOrNull()
        if (lastFullDate != null && (lastFullDate == today || lastFullDate == today.minusDays(1))) {
            var checkDate = if (lastFullDate == today) today else today.minusDays(1)
            while (fullTrainingDates.contains(checkDate)) {
                streak++
                checkDate = checkDate.minusDays(1)
            }
        }

        // Pause Phases (Starts at 4th rest day)
        val isPausePhaseActive = currentPauseDays >= 4

        // Count pause phases this month
        var pausePhasesMonth = 0
        if (allDates.isNotEmpty()) {
            val thisMonth = today.month
            val thisYear = today.year

            // Analyze gaps between workouts
            for (i in 0 until allDates.size - 1) {
                val d1 = allDates[i] // newer
                val d2 = allDates[i + 1] // older

                if (d1.month == thisMonth && d1.year == thisYear) {
                    val gap = ChronoUnit.DAYS.between(d2, d1).toInt() - 1
                    if (gap >= 4) {
                        pausePhasesMonth++
                    }
                }
            }

            // Check if current gap is a phase that started this month
            if (isPausePhaseActive && lastWorkoutDate != null) {
                if (lastWorkoutDate.month == thisMonth && lastWorkoutDate.year == thisYear) {
                    pausePhasesMonth++
                }
            }
        }

        // Erholungsanteil: Ruhetage der letzten 30 Tage
        val restDaysLast30 = 30 - trainingDaysLast30
        val recoveryPct = (restDaysLast30.toFloat() / 30f * 100f).coerceIn(0f, 100f)

        return ConsistencyStats(
            trainingDaysLast7 = trainingDaysLast7,
            trainingDaysLast30 = trainingDaysLast30,
            currentStreak = streak,
            currentPauseDays = currentPauseDays,
            pausePhasesThisMonth = pausePhasesMonth,
            isPausePhaseActive = isPausePhaseActive,
            recoveryPercent = recoveryPct
        )
    }
}
