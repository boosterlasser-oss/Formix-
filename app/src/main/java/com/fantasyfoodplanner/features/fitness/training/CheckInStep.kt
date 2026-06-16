package com.fantasyfoodplanner.features.fitness.training

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fantasyfoodplanner.data.*
import com.fantasyfoodplanner.features.fitness.*
import com.fantasyfoodplanner.logic.*
import com.fantasyfoodplanner.ui.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CheckInStep(
    selectedType: TrainingType,
    checkInText: String,
    onCheckInTextChange: (String) -> Unit,
    selectedFocusGroups: Set<String>,
    onFocusGroupsChanged: (Set<String>) -> Unit,
    onPlanGenerated: (WorkoutPlan, SessionMode, FocusMode, DailyModifier, String, Boolean) -> Unit,
    onBack: () -> Unit,
    showPlanPreview: Boolean,
    workoutPlan: WorkoutPlan?,
    onStartTraining: () -> Unit,
    onDismissPreview: () -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { AppDb.get(ctx) }
    val scope = rememberCoroutineScope()

    var yesterdayActivityFocusKey by remember { mutableStateOf<String?>(null) }
    var yesterdayActivityCategory by remember { mutableStateOf<Double?>(null) }
    LaunchedEffect(Unit) {
        try {
            val yesterdayEpoch = java.time.LocalDate.now().toEpochDay() - 1
            val allWithSets = db.workoutDao().getAllWithSets().first()
            val yesterdayOtherLogs = allWithSets
                .filter { it.log.dateEpochDay == yesterdayEpoch && it.log.exerciseType == "OTHER" }
            if (yesterdayOtherLogs.isNotEmpty()) {
                val bestLog = yesterdayOtherLogs.minByOrNull { it.log.weightKg }!!
                yesterdayActivityCategory = bestLog.log.weightKg
                val distinctFocusKeys = yesterdayOtherLogs.map { it.log.workoutType }.distinct()
                yesterdayActivityFocusKey = if (distinctFocusKeys.size > 1 && bestLog.log.weightKg <= 1.0) {
                    "FULL_BODY"
                } else {
                    bestLog.log.workoutType
                }
            }
        } catch (_: Exception) { }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        MainAppBar("Daily Check-in", onBack = onBack)

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AIHeadIcon(Modifier.size(60.dp))
                Spacer(Modifier.height(8.dp))
                FText("Wie fühlst du dich heute?", sizeSp = 16, bold = true)
            }

            BodyButtonWithPulse(
                enabled = selectedFocusGroups.isNotEmpty(),
                onClick = {
                    if (selectedFocusGroups.isEmpty()) return@BodyButtonWithPulse
                    scope.launch {
                        val learningProfile = SettingsManager.getLearningProfile(ctx)
                        val signals = CheckInAnalyzer.analyze(checkInText, learningProfile, yesterdayActivityFocusKey, yesterdayActivityCategory)
                        val dailyModifier = signals.modifier
                        val sessionMode = signals.mode
                        val focusMode = signals.focus
                        val modeExplanation = signals.explanation

                        val profile = db.userDao().profile().first()
                        if (profile != null) {
                            val fitProfile = FitnessProfile(
                                mainGoal = profile.goal,
                                experience = profile.experience,
                                weightKg = profile.weightKg,
                                gender = profile.sex
                            )

                            val primaryFocus = if (selectedFocusGroups.isNotEmpty()) {
                                selectedFocusGroups.sorted().first()
                            } else {
                                null
                            }

                            val basePlan = PlanGenerator.buildPlan(
                                fitProfile,
                                selectedType,
                                dailyModifier,
                                dailyFocus = primaryFocus,
                                sorenessFocus = signals.sorenessFocus
                            )

                            var filteredPlan = basePlan

                            if (!profile.availableEquipment.isNullOrBlank()) {
                                filteredPlan = PlanGenerator.filterByEquipment(filteredPlan, profile.availableEquipment)
                            }

                            if (!profile.healthRestrictions.isNullOrBlank()) {
                                filteredPlan = PlanGenerator.filterByHealthRestrictions(filteredPlan, profile.healthRestrictions)
                            }

                            if (profile.timePerSession > 0) {
                                filteredPlan = PlanGenerator.adjustTimeTarget(filteredPlan, profile.timePerSession)
                            }

                            var limitTriggered = false

                            val allowedCategories = BodyFocus.toCategoryStrings(selectedFocusGroups)

                            val filteredByFocus = if (allowedCategories.isNotEmpty()) {
                                filteredPlan.exercises.filter { block ->
                                    when (block.type) {
                                        "warmup", "cooldown" -> true
                                        "ex" -> {
                                            val def = ExerciseDefinitions.get(block.ex)
                                            allowedCategories.contains(def.category)
                                        }
                                        else -> true
                                    }
                                }
                            } else {
                                filteredPlan.exercises
                            }

                            val filteredExercises = filteredByFocus.map { block ->
                                if (block.type == "ex") {
                                    val history = db.workoutDao().getHistoryForExercise(block.ex).first()
                                    val next = ExerciseLogic.calculateNextProgression(history, block.ex)

                                    if (next.isLimitDetected) limitTriggered = true

                                    var finalSets = next.plannedSets
                                    val finalReps: Int
                                    var finalWeight = if (next.weightKg > 0) next.weightKg else block.weight
                                    var finalDuration = if (next.durationTargetSeconds > 0) next.durationTargetSeconds else block.durationSeconds
                                    val baseReps = if (next.repTarget > 0) next.repTarget else block.reps

                                    when (sessionMode) {
                                        SessionMode.PUSH -> {
                                            if (finalSets >= 2 && finalSets < 5) finalSets += 1
                                            finalReps = (baseReps * 1.1f).roundToInt().coerceIn(baseReps, baseReps + 2)
                                            if (finalWeight > 0) {
                                                finalWeight = (finalWeight * 1.05).coerceAtMost(finalWeight + 2.5)
                                                finalWeight = (finalWeight * 4).toInt() / 4.0
                                            }
                                            if (finalDuration > 0) finalDuration = (finalDuration * 1.1f).roundToInt()
                                        }
                                        SessionMode.RECOVERY -> {
                                            if (finalSets >= 3) finalSets = 2
                                            finalReps = (baseReps * 0.7f).roundToInt().coerceAtLeast(5)
                                            if (finalWeight > 0) finalWeight *= 0.7
                                            if (finalDuration > 0) finalDuration = (finalDuration * 0.8f).roundToInt()
                                        }
                                        SessionMode.SHORT -> {
                                            if (finalSets >= 3) finalSets -= 1
                                            finalReps = if (focusMode == FocusMode.TECHNIQUE) {
                                                if (finalWeight > 0) finalWeight *= 0.95
                                                (baseReps * 0.85f).roundToInt().coerceAtLeast(6)
                                            } else {
                                                baseReps
                                            }
                                        }
                                        else -> {
                                            finalReps = baseReps
                                        }
                                    }

                                    block.copy(
                                        weight = finalWeight,
                                        sets = finalSets.coerceIn(1, 5),
                                        reps = finalReps.coerceIn(1, 30),
                                        durationSeconds = finalDuration
                                    )
                                } else block
                            }

                            val warmups = filteredExercises.filter { it.type == "warmup" }
                            val exercisesOnly = filteredExercises.filter { it.type == "ex" }
                            val cooldowns = filteredExercises.filter { it.type == "cooldown" }

                            val finalExList = when (sessionMode) {
                                SessionMode.RECOVERY -> exercisesOnly.take(2)
                                SessionMode.SHORT -> exercisesOnly.take(3)
                                SessionMode.PUSH -> exercisesOnly
                                else -> exercisesOnly.take(exercisesOnly.size.coerceAtMost(5))
                            }

                            if (finalExList.isEmpty()) {
                                android.widget.Toast.makeText(ctx, "Plan konnte nicht erstellt werden. Bitte andere Fokusgruppen wählen.", android.widget.Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            val plan = WorkoutPlan(
                                targetMinutes = basePlan.targetMinutes,
                                estTotalMinutes = basePlan.estTotalMinutes,
                                restSeconds = basePlan.restSeconds,
                                exercises = warmups + finalExList + cooldowns,
                                currentExerciseIndex = 0
                            )

                            FitnessPersistence.savePlan(ctx, selectedType.name.lowercase(), plan)
                            onPlanGenerated(plan, sessionMode, focusMode, dailyModifier, modeExplanation, limitTriggered)
                        }
                    }
                },
                modifier = Modifier.size(80.dp)
            )
        }

        FText("Dein Coach passt das Training an.", sizeSp = 12, color = Color.Gray)
        Spacer(Modifier.height(16.dp))

        FantasyTextField(
            value = checkInText,
            onValueChange = onCheckInTextChange,
            label = "z.B. Fit, müde, wenig Zeit...",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        BodySelector3D(
            selectedFocusGroups = selectedFocusGroups,
            onFocusGroupsChanged = onFocusGroupsChanged
        )

        Spacer(Modifier.weight(1f))

        if (showPlanPreview && workoutPlan != null) {
            PlanPreviewDialog(
                plan = workoutPlan,
                onStartTraining = onStartTraining,
                onDismiss = onDismissPreview
            )
        }
    }
}
