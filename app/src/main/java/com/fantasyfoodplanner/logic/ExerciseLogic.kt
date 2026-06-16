package com.fantasyfoodplanner.logic

import com.fantasyfoodplanner.data.ExerciseLog
import com.fantasyfoodplanner.data.ExerciseWithSets
import com.fantasyfoodplanner.data.SetLog
import com.fantasyfoodplanner.features.fitness.ExerciseDefinitions

enum class ExerciseType { WEIGHTED, BODYWEIGHT, TIME }

data class ProgressionResult(
    val weightKg: Double,
    val plannedSets: Int,
    val repTarget: Int,
    val difficultyLevel: Int,
    val durationTargetSeconds: Int,
    val isLimitDetected: Boolean = false
)

object ExerciseLogic {

    fun getExerciseType(name: String): ExerciseType {
        return ExerciseDefinitions.get(name).type
    }

    fun calculateScore(log: ExerciseLog, sets: List<SetLog>): Double {
        return when (getExerciseType(log.exerciseName)) {
            ExerciseType.WEIGHTED -> {
                // Volumen-Score: Gewicht × geschaffte Reps (aussagekräftiger als nur Gewicht)
                val totalReps = sets.sumOf { it.repsDone ?: 0 }
                if (totalReps > 0) log.weightKg * totalReps / 10.0 else log.weightKg
            }
            ExerciseType.BODYWEIGHT -> log.difficultyLevel.toDouble()
            ExerciseType.TIME -> sets.sumOf { it.timeDoneSeconds ?: 0 }.toDouble()
        }
    }

    fun isStructuralLimitReached(history: List<ExerciseWithSets>): Boolean {
        if (history.size < 3) return false
        
        // A) 2-3 Fails hintereinander auf erhöhtem Level
        val recentThree = history.take(3)
        val failsHintereinander = recentThree.count { logWithSets -> 
            !logWithSets.log.wasSuccessful || (logWithSets.sets.isNotEmpty() && !logWithSets.sets.all { it.setSuccess })
        }
        if (failsHintereinander >= 3) return true

        // B) SuccessRate < 65% über letzte 5-10 Versuche
        val recentTen = history.take(10)
        if (recentTen.size >= 5) {
            val successCount = recentTen.count { it.log.wasSuccessful }
            val rate = successCount.toFloat() / recentTen.size.toFloat()
            if (rate < 0.65f) return true
        }

        // C) Plateau: 3-5 Sessions ohne Steigerung trotz Versuch
        if (history.size >= 5) {
            val lastFive = history.take(5)
            val firstOfFive = lastFive.last()
            val lastOfFive = lastFive.first()
            
            val type = getExerciseType(lastOfFive.log.exerciseName)
            val noImprovement = when(type) {
                ExerciseType.WEIGHTED -> lastOfFive.log.weightKg <= firstOfFive.log.weightKg
                ExerciseType.BODYWEIGHT -> lastOfFive.log.difficultyLevel <= firstOfFive.log.difficultyLevel && lastOfFive.log.plannedSets <= firstOfFive.log.plannedSets
                ExerciseType.TIME -> lastOfFive.log.timeTargetSeconds <= firstOfFive.log.timeTargetSeconds
            }
            if (noImprovement) return true
        }

        return false
    }

    fun calculateNextProgression(history: List<ExerciseWithSets>, currentExerciseName: String): ProgressionResult {
        if (history.isEmpty()) {
            val type = getExerciseType(currentExerciseName)
            return when(type) {
                ExerciseType.WEIGHTED -> ProgressionResult(20.0, 3, 10, 0, 0)
                ExerciseType.BODYWEIGHT -> ProgressionResult(0.0, 3, 12, 1, 0)
                ExerciseType.TIME -> ProgressionResult(0.0, 3, 0, 0, 30)
            }
        }
        
        val last = history.first()
        val type = getExerciseType(last.log.exerciseName)
        val allSuccessful = last.sets.isNotEmpty() && last.sets.all { it.setSuccess }
        
        val limitActive = isStructuralLimitReached(history)

        return when (type) {
            ExerciseType.WEIGHTED -> {
                val nextWeight = if (allSuccessful && !limitActive) last.log.weightKg + 2.5 else last.log.weightKg
                val lastReps = if (last.log.totalRepsDone > 0 && last.log.plannedSets > 0)
                    (last.log.totalRepsDone / last.log.plannedSets).coerceIn(6, 15)
                else 10
                ProgressionResult(nextWeight, last.log.plannedSets, lastReps, 0, 0, limitActive)
            }
            ExerciseType.BODYWEIGHT -> {
                var nextSets = last.log.plannedSets
                var nextDiff = last.log.difficultyLevel
                if (allSuccessful && !limitActive) {
                    if (last.log.plannedSets < 5) nextSets += 1
                    else nextDiff += 1
                }
                ProgressionResult(0.0, nextSets, 12, nextDiff, 0, limitActive)
            }
            ExerciseType.TIME -> {
                val nextTime = if (allSuccessful && !limitActive) last.log.timeTargetSeconds + 30 else last.log.timeTargetSeconds
                ProgressionResult(0.0, last.log.plannedSets, 0, 0, nextTime, limitActive)
            }
        }
    }
}
