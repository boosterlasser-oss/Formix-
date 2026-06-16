package com.fantasyfoodplanner.features.fitness

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fantasyfoodplanner.data.*
import com.fantasyfoodplanner.logic.ExerciseLogic
import com.fantasyfoodplanner.logic.ExerciseType
import com.fantasyfoodplanner.logic.TrainingType
import com.fantasyfoodplanner.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun FitnessDashboardScreen(
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit,
    onGoStats: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDb.get(ctx) }
    val scope = rememberCoroutineScope()
    
    var workoutPlan by remember { mutableStateOf<WorkoutPlan?>(null) }
    var isCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val existingPlan = FitnessPersistence.loadPlan(ctx, "fitness")
        if (existingPlan != null) {
            workoutPlan = existingPlan
        } else {
            val profile = db.userDao().profile().first()
            if (profile != null) {
                val fitProfile = FitnessProfile(mainGoal = profile.goal, experience = profile.experience, weightKg = profile.weightKg)
                val basePlan = PlanGenerator.buildPlan(fitProfile, TrainingType.BASICS)
                
                val adjustedExercises = basePlan.exercises.map { block ->
                    if (block.type == "ex") {
                        val history = db.workoutDao().getHistoryForExercise(block.ex).first()
                        val next = ExerciseLogic.calculateNextProgression(history, block.ex)
                        block.copy(
                            weight = if (next.weightKg > 0) next.weightKg else block.weight,
                            sets = next.plannedSets,
                            reps = if (next.repTarget > 0) next.repTarget else block.reps,
                            durationSeconds = next.durationTargetSeconds
                        )
                    } else block
                }
                
                val initialSetStates = mutableMapOf<Int, List<SetState>>()
                adjustedExercises.forEachIndexed { idx, b ->
                    if (b.type == "ex") {
                        val exType = ExerciseLogic.getExerciseType(b.ex)
                        initialSetStates[idx] = List(b.sets) {
                            SetState(reps = if (exType != ExerciseType.TIME) b.reps else 0, duration = if (exType == ExerciseType.TIME) b.durationSeconds else 0)
                        }
                    }
                }
                
                workoutPlan = basePlan.copy(exercises = adjustedExercises, savedSetStates = initialSetStates)
                FitnessPersistence.savePlan(ctx, "fitness", workoutPlan)
            }
        }
        val workouts = db.workoutDao().getForDay(LocalDate.now().toEpochDay()).first()
        isCompleted = workouts.any { it.type == "Fitness" }
    }

    fun saveProgress(updatedPlan: WorkoutPlan) {
        workoutPlan = updatedPlan
        FitnessPersistence.savePlan(ctx, "fitness", updatedPlan)
    }

    Box(Modifier.fillMaxSize()) {
        FantasySurface {
            Box(
                Modifier
                    .fillMaxSize()
            ) {
                if (workoutPlan == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = FantasyColors.Accent)
                    }
                } else {
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        MainAppBar("Trainingsplan", onBack = onBack, showAI = true)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FText("HEUTIGES TRAINING", color = FantasyColors.Accent, bold = true, modifier = Modifier.weight(1f))
                            FantasyButton(label = "Stats", modifier = Modifier.width(80.dp)) { onGoStats() }
                        }

                        Spacer(Modifier.height(10.dp))

                        Box(Modifier.weight(1f)) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                item { PlanSummary(workoutPlan!!) }
                                item { FText("AKTIVE ÜBUNGEN", bold = true, sizeSp = 16, color = FantasyColors.Accent) }

                                itemsIndexed(workoutPlan!!.exercises) { index, block ->
                                    if (block.type == "ex") {
                                        UnifiedExerciseCard(
                                            name = block.ex,
                                            info = block.title,
                                            sets = block.sets,
                                            reps = block.reps,
                                            weight = block.weight,
                                            durationSeconds = block.durationSeconds,
                                            isChecked = workoutPlan!!.checkedIndices.contains(index),
                                            setStates = workoutPlan!!.savedSetStates[index] ?: emptyList(),
                                            onCheckChange = { checked ->
                                                val newIndices = workoutPlan!!.checkedIndices.toMutableList()
                                                if (checked) newIndices.add(index) else newIndices.remove(index)
                                                saveProgress(workoutPlan!!.copy(checkedIndices = newIndices))
                                            },
                                            onSetStatesChange = { newList ->
                                                val newSetStates = workoutPlan!!.savedSetStates.toMutableMap()
                                                newSetStates[index] = newList
                                                saveProgress(workoutPlan!!.copy(savedSetStates = newSetStates))
                                            },
                                            onWeightChange = { newW ->
                                                val newEx = workoutPlan!!.exercises.toMutableList()
                                                newEx[index] = block.copy(weight = newW)
                                                saveProgress(workoutPlan!!.copy(exercises = newEx))
                                            },
                                            onClick = { onOpenExercise(block.ex) }
                                        )
                                    } else {
                                        WorkoutBlockCard(block.title, block.items, block.minutes)
                                    }
                                }
                                item { Spacer(Modifier.height(80.dp)) }
                            }

                            FantasyButton(
                                label = if (isCompleted) "Training aktualisieren" else "Training speichern",
                                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 16.dp)
                            ) {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val today = LocalDate.now().toEpochDay()
                                        val workoutType = "Fitness"
                                        db.workoutDao().deleteLogsForDay(today, workoutType)
                                        db.workoutDao().insert(WorkoutEntry(dateEpochDay = today, type = workoutType))

                                        workoutPlan?.exercises?.forEachIndexed { index, q ->
                                            if (q.type == "ex") {
                                                val currentSets = workoutPlan!!.savedSetStates[index] ?: emptyList()
                                                if (workoutPlan!!.checkedIndices.contains(index) || currentSets.any { it.isDone }) {
                                                    val exType = ExerciseLogic.getExerciseType(q.ex)

                                                    val totalRepsDone = currentSets.sumOf { it.reps }
                                                    val actualSetsDone = currentSets.count { it.isDone }

                                                    val logToInsert = ExerciseLog(
                                                        dateEpochDay = today,
                                                        workoutType = workoutType,
                                                        exerciseName = q.ex,
                                                        exerciseType = exType.name,
                                                        weightKg = q.weight,
                                                        plannedSets = q.sets,
                                                        actualSetsDone = actualSetsDone,
                                                        totalRepsDone = totalRepsDone,
                                                        timeTargetSeconds = q.durationSeconds,
                                                        wasSuccessful = currentSets.all { it.isDone },
                                                        scoreValue = 0.0
                                                    )

                                                    val tempSets = currentSets.mapIndexed { sIdx, state ->
                                                        SetLog(
                                                            exerciseLogId = logToInsert.id,
                                                            setIndex = sIdx + 1,
                                                            repsDone = if (exType != ExerciseType.TIME) state.reps else null,
                                                            timeDoneSeconds = if (exType == ExerciseType.TIME) state.duration else null,
                                                            setSuccess = state.isDone
                                                        )
                                                    }

                                                    val calculatedScore = ExerciseLogic.calculateScore(logToInsert, tempSets)

                                                    db.workoutDao().insertLog(logToInsert.copy(scoreValue = calculatedScore))
                                                    db.workoutDao().insertSets(tempSets)
                                                }
                                            }
                                        }
                                        isCompleted = true
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                            }
                        }
                    }
                }
            }
        }
        HelpFloatingButton("FitnessDashboard")
    }
}

@Composable
private fun PlanSummary(plan: WorkoutPlan) {
    FantasyCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FText("DAUER", sizeSp = 11)
                FText("${plan.targetMinutes}m", bold = true, color = FantasyColors.Accent)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FText("PAUSEN", sizeSp = 11)
                FText("${plan.restSeconds}s", bold = true, color = FantasyColors.Accent)
            }
        }
    }
}

@Composable
private fun WorkoutBlockCard(title: String, items: List<String>, minutes: Int) {
    FantasyCard {
        FText("${title.uppercase()} ($minutes min)", bold = true, sizeSp = 16, color = FantasyColors.Secondary)
        items.forEach { FText("• $it", sizeSp = 14) }
    }
}
