package com.fantasyfoodplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fantasyfoodplanner.data.*
import com.fantasyfoodplanner.data.repository.UserRepository
import com.fantasyfoodplanner.data.repository.WorkoutRepository
import com.fantasyfoodplanner.features.fitness.*
import com.fantasyfoodplanner.logic.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel für den Training-Flow.
 * Verwaltet den gesamten Trainingsablauf-State.
 */
class TrainingFlowViewModel(
    application: Application,
    private val userRepo: UserRepository,
    private val workoutRepo: WorkoutRepository
) : AndroidViewModel(application) {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    init {
        viewModelScope.launch {
            _userProfile.value = userRepo.getProfile()
        }
    }

    suspend fun getExerciseHistory(exerciseName: String): List<ExerciseWithSets> {
        return workoutRepo.getExerciseHistory(exerciseName)
    }

    suspend fun saveWorkout(plan: WorkoutPlan, type: String) = withContext(Dispatchers.IO) {
        val today = java.time.LocalDate.now().toEpochDay()
        workoutRepo.insertWorkout(WorkoutEntry(dateEpochDay = today, type = type))
        plan.exercises.forEachIndexed { index, block ->
            if (block.type == "ex") {
                val states = plan.savedSetStates[index] ?: emptyList()
                if (plan.checkedIndices.contains(index) || states.any { it.isDone }) {
                    val exType = ExerciseLogic.getExerciseType(block.ex)
                    val totalReps = states.sumOf { it.reps }
                    val setsDone = states.count { it.isDone }
                    val log = ExerciseLog(
                        dateEpochDay = today,
                        workoutType = type,
                        exerciseName = block.ex,
                        exerciseType = exType.name,
                        plannedSets = block.sets,
                        actualSetsDone = setsDone,
                        totalRepsDone = totalReps,
                        weightKg = block.weight,
                        wasSuccessful = states.all { it.isDone }
                    )
                    workoutRepo.insertExerciseLog(log)
                    val setLogs = states.mapIndexed { sIdx, s ->
                        SetLog(
                            exerciseLogId = log.id,
                            setIndex = sIdx + 1,
                            repsDone = s.reps,
                            timeDoneSeconds = s.duration,
                            setSuccess = s.isDone
                        )
                    }
                    workoutRepo.insertSets(setLogs)
                }
            }
        }
    }

    class Factory(
        private val application: Application,
        private val db: AppDb
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TrainingFlowViewModel(
                application = application,
                userRepo = UserRepository(db),
                workoutRepo = WorkoutRepository(db)
            ) as T
        }
    }
}


