package com.fantasyfoodplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fantasyfoodplanner.data.*
import com.fantasyfoodplanner.data.repository.MealRepository
import com.fantasyfoodplanner.data.repository.UserRepository
import com.fantasyfoodplanner.logic.TrainingConsistencyCalculator
import com.fantasyfoodplanner.utils.NutrientCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Wochen-Tage Status:
 * 0 = kein Training, 1 = volles Workout, 2 = ergänzende Aktivität, 3 = leichte Bewegung
 */
data class DashboardState(
    val userProfile: UserProfile? = null,
    val todayKcal: Int = 0,
    val avgKcalWeek: Int = 0,
    val trainingDaysThisWeek: Int = 0,
    val currentStreak: Int = 0,
    val lastWorkoutDaysAgo: Int = -1,
    val currentWeight: Double? = null,
    val weightChange: Double = 0.0,
    val weekDaysDone: List<Int> = List(7) { 0 }, // Mo-So: 0=nichts, 1=voll, 2=ergänzend, 3=leicht
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel für den Dashboard-Screen.
 * Verwaltet Benutzerprofil, Kalorienbilanz, Trainings-Streak und Goal-Daten.
 */
class DashboardViewModel(
    application: Application,
    private val userRepo: UserRepository,
    private val mealRepo: MealRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    // Rückwärtskompatibel
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()
    private val _todayKcal = MutableStateFlow(0)
    val todayKcal: StateFlow<Int> = _todayKcal.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val profile = userRepo.getProfile()
                _userProfile.value = profile

                val db = AppDb.get(getApplication())
                val today = LocalDate.now()
                val todayEpoch = today.toEpochDay()
                val weekStart = today.minusDays(6).toEpochDay()

                // Heutige Kalorien
                val meals = withContext(Dispatchers.IO) {
                    mealRepo.mealsOnDayFlow(todayEpoch).first()
                }
                val manualMeals = withContext(Dispatchers.IO) {
                    mealRepo.manualMealsOnDayFlow(todayEpoch).first()
                }
                val recipes = mealRepo.getRecipes()
                val products = mealRepo.getProducts()
                val totals = NutrientCalculator.calculateTotals(meals, manualMeals, recipes, products)
                _todayKcal.value = totals.kcal

                // Ø Kalorien letzte 7 Tage – optimiert: Range-Query statt getAll()
                val weekMeals = withContext(Dispatchers.IO) {
                    db.mealDao().getMealsInRange(weekStart, todayEpoch).first()
                }
                val weekManual = withContext(Dispatchers.IO) {
                    db.manualMealDao().getAll().first()
                        .filter { it.dateEpochDay in weekStart..todayEpoch }
                }
                var weekKcalSum = 0
                var activeFoodDays = 0
                for (i in 0..6) {
                    val d = today.minusDays(i.toLong()).toEpochDay()
                    val dayMeals = weekMeals.filter { it.dateEpochDay == d }
                    val dayManual = weekManual.filter { it.dateEpochDay == d }
                    if (dayMeals.isNotEmpty() || dayManual.isNotEmpty()) {
                        val dayTotals = NutrientCalculator.calculateTotals(dayMeals, dayManual, recipes, products)
                        weekKcalSum += dayTotals.kcal
                        activeFoodDays++
                    }
                }
                val avgKcal = if (activeFoodDays > 0) weekKcalSum / activeFoodDays else 0

                // Trainings-Daten – optimiert: Range-Query statt getAllWorkouts()
                val workouts = withContext(Dispatchers.IO) {
                    db.workoutDao().getAllWorkouts().first()
                }
                val allLogs = withContext(Dispatchers.IO) {
                    db.workoutDao().getAllWithSets().first()
                }
                val consistency = TrainingConsistencyCalculator.calculate(
                    workouts, allLogs.map { it.log }, today
                )

                // Letztes Workout
                val sortedWorkouts = workouts.sortedByDescending { it.dateEpochDay }
                val lastWorkoutEpoch = sortedWorkouts.firstOrNull()?.dateEpochDay
                val lastWorkoutDaysAgo = if (lastWorkoutEpoch != null) {
                    (todayEpoch - lastWorkoutEpoch).toInt()
                } else -1

                // Wochen-Tage (Mo=0, So=6) mit 3-Stufen-Status markieren
                val mondayOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
                val workoutsByDay = workouts.groupBy { it.dateEpochDay }
                val logsByDay = allLogs.groupBy { it.log.dateEpochDay }

                val weekDaysDone = (0..6).map { dayOffset ->
                    val d = mondayOfWeek.plusDays(dayOffset.toLong()).toEpochDay()
                    val dayWorkouts = workoutsByDay[d] ?: emptyList()
                    val dayLogs = logsByDay[d] ?: emptyList()

                    if (dayWorkouts.isEmpty()) {
                        0
                    } else {
                        val hasRegularWorkout = dayWorkouts.any { it.type != "OTHER_ACTIVITY" }
                        if (hasRegularWorkout) {
                            1
                        } else {
                            val otherLogs = dayLogs.filter { it.log.exerciseType == "OTHER" }
                            val bestCategory = otherLogs.minOfOrNull { it.log.weightKg } ?: 3.0
                            when {
                                bestCategory <= 1.0 -> 1
                                bestCategory <= 2.0 -> 2
                                else -> 3
                            }
                        }
                    }
                }

                // Gewicht
                val weights = withContext(Dispatchers.IO) { db.weightDao().getAll().first() }
                val sortedWeights = weights.sortedByDescending { it.dateEpochDay }
                val currentWeight = sortedWeights.firstOrNull()?.weightKg
                val oldWeight = sortedWeights.lastOrNull {
                    it.dateEpochDay >= today.minusDays(30).toEpochDay()
                }?.weightKg
                val wChange = if (currentWeight != null && oldWeight != null && currentWeight != oldWeight) {
                    currentWeight - oldWeight
                } else 0.0

                _state.value = DashboardState(
                    userProfile = profile,
                    todayKcal = totals.kcal,
                    avgKcalWeek = avgKcal,
                    trainingDaysThisWeek = consistency.trainingDaysLast7,
                    currentStreak = consistency.currentStreak,
                    lastWorkoutDaysAgo = lastWorkoutDaysAgo,
                    currentWeight = currentWeight,
                    weightChange = wChange,
                    weekDaysDone = weekDaysDone,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Daten konnten nicht geladen werden: ${e.localizedMessage}"
                )
            }
        }
    }

    fun refresh() {
        loadDashboardData()
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    class Factory(
        private val application: Application,
        private val db: AppDb
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                application = application,
                userRepo = UserRepository(db),
                mealRepo = MealRepository(db)
            ) as T
        }
    }
}
