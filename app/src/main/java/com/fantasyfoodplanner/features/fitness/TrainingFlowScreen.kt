package com.fantasyfoodplanner.features.fitness

import android.content.*
import android.os.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fantasyfoodplanner.data.*
import com.fantasyfoodplanner.features.fitness.training.*
import com.fantasyfoodplanner.logic.*
import com.fantasyfoodplanner.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

enum class SessionMode { RECOVERY, SHORT, NORMAL, PUSH }
enum class FocusMode { PERFORMANCE, TECHNIQUE, MOBILITY, EFFICIENCY, NORMAL }
enum class TrainingStep { TYPE_SELECTION, CHECK_IN, WORKOUT, OTHER_ACTIVITY }

@Composable
fun TrainingFlowScreen(
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit,
    onGoStats: () -> Unit,
    onGoUpgrade: () -> Unit = {}
) {
    val ctx = LocalContext.current

    var step by remember { mutableStateOf(TrainingStep.TYPE_SELECTION) }
    var selectedType by remember { mutableStateOf(SettingsManager.getTrainingType(ctx)) }
    var checkInText by remember { mutableStateOf("") }
    var selectedFocusGroups by remember { mutableStateOf(setOf<String>()) }
    var workoutPlan by remember { mutableStateOf<WorkoutPlan?>(null) }
    var currentExerciseIndex by remember { mutableStateOf(0) }
    var dailyModifier by remember { mutableStateOf(DailyModifier()) }
    var sessionMode by remember { mutableStateOf(SessionMode.NORMAL) }
    var focusMode by remember { mutableStateOf(FocusMode.NORMAL) }
    var modeExplanation by remember { mutableStateOf("") }
    var showLimitDialog by remember { mutableStateOf(false) }
    var showPlanPreview by remember { mutableStateOf(false) }
    var showWorkoutLimitReachedDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val db = remember { AppDb.get(ctx) }

    val skippedExercises = remember { mutableStateListOf<String>().apply { addAll(SettingsManager.getSkippedExercises(ctx)) } }

    var timerService by remember { mutableStateOf<TimerService?>(null) }
    DisposableEffect(ctx) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                timerService = (binder as? TimerService.TimerBinder)?.getService()
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                timerService = null
            }
        }
        ctx.bindService(Intent(ctx, TimerService::class.java), connection, Context.BIND_AUTO_CREATE)
        onDispose { ctx.unbindService(connection) }
    }

    val handleExit = {
        timerService?.stopTimer()
        onBack()
    }

    BackHandler { handleExit() }

    LaunchedEffect(Unit) {
        val existingPlan = FitnessPersistence.loadPlan(ctx, selectedType.name.lowercase())
        if (existingPlan != null) {
            workoutPlan = existingPlan
            currentExerciseIndex = existingPlan.currentExerciseIndex
            step = TrainingStep.WORKOUT
        }
    }

    LaunchedEffect(step) {
        if (step == TrainingStep.CHECK_IN && selectedFocusGroups.isEmpty()) {
            try {
                val profile = db.userDao().profile().first()
                val lastWorkout = db.workoutDao().getAllWorkouts().first()
                    .filter { it.type != "OTHER" }
                    .maxByOrNull { it.dateEpochDay }
                if (lastWorkout != null && !lastWorkout.focusAreas.isNullOrBlank()) {
                    val lastFocusSet = BodyFocusMapper.fromEnumString(lastWorkout.focusAreas)
                    selectedFocusGroups = lastFocusSet.map { it.name }.toSet()
                } else if (profile != null && !profile.focusAreas.isNullOrBlank()) {
                    val profileFocusSet = BodyFocusMapper.fromProfileString(profile.focusAreas)
                    selectedFocusGroups = profileFocusSet.map { it.name }.toSet()
                }
            } catch (e: Exception) { }
        }
    }

    FantasySurface {
        when (step) {
            TrainingStep.TYPE_SELECTION -> {
                TypeSelectionStep(
                    selectedType = selectedType,
                    onTypeSelected = { type ->
                        selectedType = type
                        SettingsManager.setTrainingType(ctx, type)
                    },
                    onConfirm = {
                        scope.launch(Dispatchers.IO) {
                            val canStart = SubscriptionManager.canSaveWorkout(ctx, db)
                            withContext(Dispatchers.Main) {
                                if (canStart) {
                                    if (selectedType == TrainingType.OTHER_ACTIVITY) {
                                        step = TrainingStep.OTHER_ACTIVITY
                                    } else {
                                        step = TrainingStep.CHECK_IN
                                    }
                                } else {
                                    showWorkoutLimitReachedDialog = true
                                }
                            }
                        }
                    },
                    onBack = handleExit
                )
            }

            TrainingStep.CHECK_IN -> {
                CheckInStep(
                    selectedType = selectedType,
                    checkInText = checkInText,
                    onCheckInTextChange = { checkInText = it },
                    selectedFocusGroups = selectedFocusGroups,
                    onFocusGroupsChanged = { selectedFocusGroups = it },
                    onPlanGenerated = { plan, mode, focus, modifier, explanation, limitTriggered ->
                        workoutPlan = plan
                        sessionMode = mode
                        focusMode = focus
                        dailyModifier = modifier
                        modeExplanation = explanation
                        if (limitTriggered) {
                            showLimitDialog = true
                        } else {
                            showPlanPreview = true
                        }
                    },
                    onBack = { step = TrainingStep.TYPE_SELECTION },
                    showPlanPreview = showPlanPreview,
                    workoutPlan = workoutPlan,
                    onStartTraining = {
                        showPlanPreview = false
                        FitnessPersistence.savePlan(ctx, selectedType.name.lowercase(), workoutPlan)
                        step = TrainingStep.WORKOUT
                    },
                    onDismissPreview = {
                        showPlanPreview = false
                        FitnessPersistence.savePlan(ctx, selectedType.name.lowercase(), null)
                        workoutPlan = null
                    }
                )
            }

            TrainingStep.WORKOUT -> {
                workoutPlan?.let { plan ->
                    WorkoutStep(
                        plan = plan,
                        currentExerciseIndex = currentExerciseIndex,
                        sessionMode = sessionMode,
                        focusMode = focusMode,
                        modeExplanation = modeExplanation,
                        selectedType = selectedType,
                        skippedExercises = skippedExercises,
                        timerService = timerService,
                        onPlanUpdate = { updatedPlan ->
                            workoutPlan = updatedPlan
                        },
                        onExerciseIndexChange = { newIndex ->
                            currentExerciseIndex = newIndex
                        },
                        onOpenExercise = onOpenExercise,
                        onFinish = onBack,
                        onBack = handleExit
                    )
                }
            }

            TrainingStep.OTHER_ACTIVITY -> {
                OtherActivityStep(
                    onActivitySaved = { onBack() },
                    onBack = { step = TrainingStep.TYPE_SELECTION }
                )
            }
        }

        if (showLimitDialog && workoutPlan != null) {
            LimitDialog(
                plan = workoutPlan!!,
                selectedType = selectedType,
                onKeep = { showLimitDialog = false; step = TrainingStep.WORKOUT },
                onReduce = { reducedPlan ->
                    workoutPlan = reducedPlan; showLimitDialog = false; step = TrainingStep.WORKOUT
                },
                onDismiss = { showLimitDialog = false; step = TrainingStep.WORKOUT }
            )
        }

        if (showWorkoutLimitReachedDialog) {
            AlertDialog(
                onDismissRequest = { showWorkoutLimitReachedDialog = false },
                containerColor = FantasyColors.CardBg,
                title = { FText("Wochenlimit erreicht", bold = true, sizeSp = 18, color = FantasyColors.Accent) },
                text = {
                    Column {
                        FText("Du hast 5 von 5 Trainingseinheiten diese Woche absolviert – das ist das Maximum im Free-Plan.")
                        Spacer(Modifier.height(8.dp))
                        FText("Mit Premium trainierst du unbegrenzt und nutzt alle Trainingstypen!", color = FantasyColors.GrayText, sizeSp = 13)
                    }
                },
                confirmButton = { FantasyButton("Auf Premium upgraden") { showWorkoutLimitReachedDialog = false; onGoUpgrade() } },
                dismissButton = { TextButton(onClick = { showWorkoutLimitReachedDialog = false }) { FText("Schliessen", color = FantasyColors.GrayText) } }
            )
        }
    }
}

@Composable
fun BodyButtonWithPulse(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    val alpha = remember { androidx.compose.animation.core.Animatable(0.85f) }
    LaunchedEffect(enabled) {
        if (enabled) {
            while (true) {
                scale.animateTo(1.12f, androidx.compose.animation.core.tween(700))
                scale.animateTo(1f, androidx.compose.animation.core.tween(700))
                alpha.animateTo(1f, androidx.compose.animation.core.tween(700))
                alpha.animateTo(0.85f, androidx.compose.animation.core.tween(700))
            }
        } else {
            scale.snapTo(1f); alpha.snapTo(0.4f)
        }
    }
    Box(
        modifier = modifier
            .background(Color(0xFF1A1A1A).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .border(3.dp, if (enabled) Color(0xFF2196F3) else Color(0xFF666666), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { if (enabled) onClick() }
            .graphicsLayer(scaleX = scale.value, scaleY = scale.value, alpha = alpha.value)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        val composition by com.airbnb.lottie.compose.rememberLottieComposition(
            com.airbnb.lottie.compose.LottieCompositionSpec.Asset("animations/Körper/man-chest.json")
        )
        val progress by com.airbnb.lottie.compose.animateLottieCompositionAsState(
            composition = composition, iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever
        )
        com.airbnb.lottie.compose.LottieAnimation(composition = composition, progress = { progress }, modifier = Modifier.fillMaxSize())
        FText("START", sizeSp = 14, bold = true, color = Color.White, modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
    }
}
