package com.fantasyfoodplanner.features.fitness

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import com.fantasyfoodplanner.data.*
import com.fantasyfoodplanner.logic.*
import com.fantasyfoodplanner.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun CrossFitScreen(
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit,
    onGoStats: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDb.get(ctx) }
    val scope = rememberCoroutineScope()
    
    var workoutPlan by remember { mutableStateOf<WorkoutPlan?>(null) }
    var isCompleted by remember { mutableStateOf(false) }
    var exerciseToSwap by remember { mutableStateOf<Pair<Int, WorkoutBlock>?>(null) }
    var alternativeName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val existingPlan = FitnessPersistence.loadPlan(ctx, "crossfit")
        if (existingPlan != null) {
            workoutPlan = existingPlan
        } else {
            val profile = db.userDao().profile().first()
            if (profile != null) {
                val fitProfile = FitnessProfile(mainGoal = "crossfit", experience = profile.experience, weightKg = profile.weightKg)
                val basePlan = PlanGenerator.buildPlan(fitProfile, TrainingType.CROSSFIT)
                
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
                            SetState(
                                reps = if (exType != ExerciseType.TIME) b.reps else 0,
                                duration = if (exType == ExerciseType.TIME) b.durationSeconds else 0
                            )
                        }
                    }
                }
                
                workoutPlan = basePlan.copy(
                    exercises = adjustedExercises,
                    savedSetStates = initialSetStates
                )
                FitnessPersistence.savePlan(ctx, "crossfit", workoutPlan)
            }
        }
        val workouts = db.workoutDao().getForDay(LocalDate.now().toEpochDay()).first()
        isCompleted = workouts.any { it.type == "CrossFit" }
    }

    fun saveProgress(updatedPlan: WorkoutPlan) {
        workoutPlan = updatedPlan
        FitnessPersistence.savePlan(ctx, "crossfit", updatedPlan)
    }

    Box(Modifier.fillMaxSize()) {
        FantasySurface {
            if (workoutPlan == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FantasyColors.Accent)
                }
            } else {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    MainAppBar(title = "CrossFit Performance", onBack = onBack, showAI = true)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FText("INTENSIVES ZIRKELTRAINING", color = FantasyColors.Accent, bold = true, modifier = Modifier.weight(1f))
                        FantasyButton(label = "Stats", modifier = Modifier.width(80.dp)) { onGoStats() }
                    }

                    Spacer(Modifier.height(10.dp))

                    Box(Modifier.weight(1f)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                        onClick = { onOpenExercise(block.ex) },
                                        onSwap = { 
                                            exerciseToSwap = index to block
                                            val options = listOf("Burpees", "Box Jumps", "Mountain Climbers", "Kettlebell Swings", "Wall Balls", "Thrusters")
                                            alternativeName = options.filter { it != block.ex }.shuffled().first()
                                        }
                                    )
                                } else {
                                    FantasyCard {
                                        FText(block.title.uppercase(), bold = true, sizeSp = 16, color = FantasyColors.Secondary)
                                        block.items.forEach { FText("• $it", sizeSp = 14) }
                                    }
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
                                    val workoutType = "CrossFit"
                                    db.workoutDao().deleteLogsForDay(today, workoutType)
                                    val session = WorkoutEntry(dateEpochDay = today, type = workoutType)
                                    db.workoutDao().insert(session)

                                    val processedExercises = mutableMapOf<ExerciseWithSets, List<ExerciseWithSets>>()

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
                                                val finalLog = logToInsert.copy(scoreValue = calculatedScore)

                                                db.workoutDao().insertLog(finalLog)
                                                db.workoutDao().insertSets(tempSets)

                                                val history = db.workoutDao().getHistoryForExercise(q.ex).first()
                                                processedExercises[ExerciseWithSets(finalLog, tempSets)] = history
                                            }
                                        }
                                    }

                                    val allWorkouts = db.workoutDao().getAllWorkouts().first()
                                    if (processedExercises.isNotEmpty()) {
                                        TrainingValidator.runSafetyValidation(ctx, session, processedExercises, allWorkouts)
                                    }

                                    isCompleted = true
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        }
                    }
                }
            }

            if (exerciseToSwap != null && alternativeName != null) {
                val (idx, original) = exerciseToSwap!!
                AlertDialog(
                    onDismissRequest = { exerciseToSwap = null },
                    title = { FText("Übung tauschen", color = FantasyColors.Accent, bold = true) },
                    containerColor = Color(0xFF151515),
                    text = {
                        Column {
                            FText("Möchtest du '${original.ex}' durch diese CrossFit-Alternative ersetzen?", sizeSp = 14)
                            Spacer(Modifier.height(12.dp))
                            FText(alternativeName!!, sizeSp = 20, bold = true, color = FantasyColors.Gold)
                        }
                    },
                    confirmButton = {
                        FantasyButton(label = "Tauschen") {
                            val newEx = workoutPlan!!.exercises.toMutableList()
                            val newDef = ExerciseDefinitions.get(alternativeName!!)
                            newEx[idx] = original.copy(ex = alternativeName!!, title = newDef.desc)
                            
                            val exType = ExerciseLogic.getExerciseType(alternativeName!!)
                            val newSetStates = workoutPlan!!.savedSetStates.toMutableMap()
                            newSetStates[idx] = List(original.sets) { SetState(reps = if (exType != ExerciseType.TIME) original.reps else 0, duration = if (exType == ExerciseType.TIME) original.durationSeconds else 0) }
                            
                            saveProgress(workoutPlan!!.copy(exercises = newEx, savedSetStates = newSetStates))
                            exerciseToSwap = null
                        }
                    },
                    dismissButton = { TextButton(onClick = { exerciseToSwap = null }) { FText("Abbrechen", color = Color.Gray) } }
                )
            }
        }
        HelpFloatingButton("CrossFit")
    }
}
