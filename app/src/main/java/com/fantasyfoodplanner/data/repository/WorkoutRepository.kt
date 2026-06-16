package com.fantasyfoodplanner.data.repository

import com.fantasyfoodplanner.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Repository für Workout- und Übungsdaten.
 * Kapselt den Datenbankzugriff für alle fitnessbezogenen Daten.
 */
class WorkoutRepository(private val db: AppDb) {

    fun workoutsForDayFlow(dateEpochDay: Long): Flow<List<WorkoutEntry>> =
        db.workoutDao().getForDay(dateEpochDay)

    fun allWorkoutsFlow(): Flow<List<WorkoutEntry>> =
        db.workoutDao().getAllWorkouts()

    fun allExercisesWithSetsFlow(): Flow<List<ExerciseWithSets>> =
        db.workoutDao().getAllWithSets()

    fun exerciseHistoryFlow(exerciseName: String): Flow<List<ExerciseWithSets>> =
        db.workoutDao().getHistoryForExercise(exerciseName)

    suspend fun getExerciseHistory(exerciseName: String): List<ExerciseWithSets> =
        withContext(Dispatchers.IO) {
            db.workoutDao().getHistoryForExercise(exerciseName).first()
        }

    suspend fun insertWorkout(entry: WorkoutEntry) = withContext(Dispatchers.IO) {
        db.workoutDao().insert(entry)
    }

    suspend fun insertExerciseLog(log: ExerciseLog) = withContext(Dispatchers.IO) {
        db.workoutDao().insertLog(log)
    }

    suspend fun insertSets(sets: List<SetLog>) = withContext(Dispatchers.IO) {
        db.workoutDao().insertSets(sets)
    }

    suspend fun getAllWorkouts(): List<WorkoutEntry> = withContext(Dispatchers.IO) {
        db.workoutDao().getAllWorkouts().first()
    }

    suspend fun getAllWithSets(): List<ExerciseWithSets> = withContext(Dispatchers.IO) {
        db.workoutDao().getAllWithSets().first()
    }
}

