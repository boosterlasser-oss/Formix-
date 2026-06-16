package com.fantasyfoodplanner.features.fitness.training

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fantasyfoodplanner.data.*
import com.fantasyfoodplanner.features.fitness.*
import com.fantasyfoodplanner.logic.*
import com.fantasyfoodplanner.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Schritt 3: Workout-Durchführung (Aufwärmen → Übungen → Cooldown)
 */
@Composable
fun WorkoutStep(
    plan: WorkoutPlan,
    currentExerciseIndex: Int,
    sessionMode: SessionMode,
    focusMode: FocusMode,
    modeExplanation: String,
    selectedType: TrainingType,
    skippedExercises: MutableList<String>,
    timerService: TimerService?,
    onPlanUpdate: (WorkoutPlan) -> Unit,
    onExerciseIndexChange: (Int) -> Unit,
    onOpenExercise: (String) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { AppDb.get(ctx) }
    val app = ctx.applicationContext as Application
    val scope = rememberCoroutineScope()
    var showSkipped by remember { mutableStateOf(false) }
    var showWorkoutLimitUpgrade by remember { mutableStateOf(false) }
    var showNothingDoneDialog by remember { mutableStateOf(false) }

    val workflowBlocks = plan.exercises.filter { it.type == "warmup" || it.type == "ex" || it.type == "cooldown" }
    val glowGreen = Color(0xFF00FF7F)

    Box(Modifier.fillMaxSize()) {

    if (currentExerciseIndex < workflowBlocks.size) {
        val currentBlock = workflowBlocks[currentExerciseIndex]
        val originalIndexInPlan = plan.exercises.indexOf(currentBlock)

        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val title = when (currentBlock.type) {
                    "warmup" -> "Vorbereitung"
                    "cooldown" -> "Cooldown"
                    else -> "Übung ${currentExerciseIndex} / ${workflowBlocks.count { it.type == "ex" }}"
                }
                MainAppBar(title, onBack = onBack)
                Spacer(Modifier.weight(1f))
                if (currentBlock.type == "ex") {
                    IconButton(onClick = {
                        SettingsManager.addSkippedExercise(ctx, currentBlock.ex)
                        skippedExercises.add(currentBlock.ex)
                        val newIndex = currentExerciseIndex + 1
                        onExerciseIndexChange(newIndex)
                        timerService?.stopTimer()
                        val updatedPlan = plan.copy(currentExerciseIndex = newIndex)
                        onPlanUpdate(updatedPlan)
                        FitnessPersistence.savePlan(ctx, selectedType.name.lowercase(), updatedPlan)
                    }) {
                        Icon(Icons.Default.Close, "Überspringen", tint = Color.Red.copy(0.6f))
                    }
                }
            }

            if (currentBlock.type == "ex" && sessionMode != SessionMode.NORMAL) {
                val chipColor = when (sessionMode) {
                    SessionMode.PUSH -> FantasyColors.Accent
                    SessionMode.SHORT -> Color.Cyan
                    SessionMode.RECOVERY -> Color.White
                    else -> Color.Gray
                }
                val modeText = when (sessionMode) {
                    SessionMode.PUSH -> "⚡ MODUS: PUSH (Volle Power)"
                    SessionMode.SHORT -> "⏱️ MODUS: SHORT (Zeitsparend)"
                    SessionMode.RECOVERY -> "🛡️ MODUS: RECOVERY (Schonend)"
                    else -> ""
                }
                Surface(
                    color = chipColor.copy(0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, chipColor.copy(0.5f)),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    FText(
                        text = modeText,
                        sizeSp = 10,
                        bold = true,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = chipColor
                    )
                }
                if (modeExplanation.isNotEmpty() && currentExerciseIndex == 1) {
                    FText(
                        text = modeExplanation,
                        sizeSp = 11,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            when (currentBlock.type) {
                "warmup" -> {
                    WarmupCard(currentBlock)
                }
                "cooldown" -> {
                    CooldownCard(currentBlock)
                }
                else -> {
                    UnifiedExerciseCard(
                        name = currentBlock.ex,
                        info = currentBlock.title,
                        sets = currentBlock.sets,
                        reps = currentBlock.reps,
                        weight = currentBlock.weight,
                        durationSeconds = currentBlock.durationSeconds,
                        isChecked = plan.checkedIndices.contains(originalIndexInPlan),
                        setStates = plan.savedSetStates[originalIndexInPlan] ?: List(currentBlock.sets) { SetState(reps = currentBlock.reps, duration = currentBlock.durationSeconds) },
                        onCheckChange = { checked ->
                            val newIndices = plan.checkedIndices.toMutableList()
                            if (checked) newIndices.add(originalIndexInPlan) else newIndices.remove(originalIndexInPlan)
                            val updatedPlan = plan.copy(checkedIndices = newIndices)
                            onPlanUpdate(updatedPlan)
                            FitnessPersistence.savePlan(ctx, selectedType.name.lowercase(), updatedPlan)
                        },
                        onSetStatesChange = { newList ->
                            val newSetStates = plan.savedSetStates.toMutableMap()
                            newSetStates[originalIndexInPlan] = newList
                            val updatedPlan = plan.copy(savedSetStates = newSetStates)
                            onPlanUpdate(updatedPlan)
                            FitnessPersistence.savePlan(ctx, selectedType.name.lowercase(), updatedPlan)
                        },
                        onWeightChange = { newW ->
                            val newEx = plan.exercises.toMutableList()
                            newEx[originalIndexInPlan] = currentBlock.copy(weight = newW)
                            val updatedPlan = plan.copy(exercises = newEx)
                            onPlanUpdate(updatedPlan)
                            FitnessPersistence.savePlan(ctx, selectedType.name.lowercase(), updatedPlan)
                        },
                        onClick = { onOpenExercise(currentBlock.ex) }
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            if (skippedExercises.isNotEmpty() && currentBlock.type == "ex") {
                FantasyButton("Weggedrückte Übungen (${skippedExercises.size})", Modifier.fillMaxWidth().padding(bottom = 12.dp), alpha = 0.7f) {
                    showSkipped = true
                }
            }
            FantasyButton(
                label = if (currentExerciseIndex == workflowBlocks.size - 1) "Training abschließen" else "Nächste Karte",
                modifier = Modifier.fillMaxWidth()
            ) {
                if (currentExerciseIndex < workflowBlocks.size - 1) {
                    val newIndex = currentExerciseIndex + 1
                    onExerciseIndexChange(newIndex)
                    timerService?.stopTimer()
                    val updatedPlan = plan.copy(currentExerciseIndex = newIndex)
                    onPlanUpdate(updatedPlan)
                    FitnessPersistence.savePlan(ctx, selectedType.name.lowercase(), updatedPlan)
                } else {
                    scope.launch(Dispatchers.IO) {
                        val successRate = calculateSuccessRate(plan)
                        SettingsManager.calibrateProfile(ctx, successRate)
                        val saved = saveWorkoutToDb(db, plan, selectedType.name)
                        FitnessPersistence.savePlan(ctx, selectedType.name.lowercase(), null)
                        timerService?.stopTimer()
                        if (!saved) {
                            launch(Dispatchers.Main) { showNothingDoneDialog = true }
                            return@launch
                        }
                        // Upgrade-Hinweis wenn Free-User 5 Workouts/Woche erreicht
                        val weekStart = java.time.LocalDate.now().minusDays(7).toEpochDay()
                        val weekCount = db.workoutDao().countWorkoutsSince(weekStart)
                        if (weekCount >= 5 && SubscriptionManager.getCurrentTier(ctx) == com.fantasyfoodplanner.logic.SubscriptionTier.FREE) {
                            launch(Dispatchers.Main) { showWorkoutLimitUpgrade = true }
                        } else {
                            launch(Dispatchers.Main) { onFinish() }
                        }
                    }
                }
            }
        }
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            FantasyButton("Speichern & Beenden") {
                scope.launch(Dispatchers.IO) {
                    val saved = saveWorkoutToDb(db, plan, selectedType.name)
                    FitnessPersistence.savePlan(ctx, selectedType.name.lowercase(), null)
                    timerService?.stopTimer()
                    if (!saved) {
                        launch(Dispatchers.Main) { showNothingDoneDialog = true }
                        return@launch
                    }
                    // Upgrade-Hinweis wenn Free-User 5 Workouts/Woche erreicht
                    val weekStart = java.time.LocalDate.now().minusDays(7).toEpochDay()
                    val weekCount = db.workoutDao().countWorkoutsSince(weekStart)
                    if (weekCount >= 5 && SubscriptionManager.getCurrentTier(ctx) == com.fantasyfoodplanner.logic.SubscriptionTier.FREE) {
                        launch(Dispatchers.Main) { showWorkoutLimitUpgrade = true }
                    } else {
                        launch(Dispatchers.Main) { onFinish() }
                    }
                }
            }
        }
    }

    // Upgrade-Dialog: 5 Workouts/Woche Limit erreicht
    if (showWorkoutLimitUpgrade) {
        AlertDialog(
            onDismissRequest = { showWorkoutLimitUpgrade = false; onFinish() },
            containerColor = FantasyColors.CardBg,
            title = {
                FText("Du trainierst fleissig!", bold = true, sizeSp = 18, color = FantasyColors.Accent)
            },
            text = {
                Column {
                    FText("Du hast 5 Workouts diese Woche erreicht – das Maximum im Free-Plan.")
                    Spacer(Modifier.height(8.dp))
                    FText("Mit Premium trainierst du unbegrenzt und nutzt alle Trainingstypen!", color = FantasyColors.GrayText, sizeSp = 13)
                }
            },
            confirmButton = {
                FantasyButton("Auf Premium upgraden") {
                    showWorkoutLimitUpgrade = false
                    onFinish()
                }
            },
            dismissButton = {
                TextButton(onClick = { showWorkoutLimitUpgrade = false; onFinish() }) {
                    FText("Spaeter", color = FantasyColors.GrayText)
                }
            }
        )
    }

    // Dialog: Kein Set abgehakt – Training wird nicht gezählt
    if (showNothingDoneDialog) {
        AlertDialog(
            onDismissRequest = { showNothingDoneDialog = false },
            containerColor = FantasyColors.CardBg,
            title = {
                FText("Kein Training erfasst", bold = true, sizeSp = 18, color = Color.Red.copy(alpha = 0.85f))
            },
            text = {
                FText("Du hast keine einzige Übung abgeschlossen. Hake mindestens einen Satz ab, damit das Training gezählt wird.")
            },
            confirmButton = {
                FantasyButton("Zurück zum Training") {
                    showNothingDoneDialog = false
                }
            },
            dismissButton = {
                TextButton(onClick = { showNothingDoneDialog = false; onFinish() }) {
                    FText("Trotzdem beenden", color = FantasyColors.GrayText)
                }
            }
        )
    }

    } // end outer Box

    // Übersprungene Übungen Dialog
    if (showSkipped) {
        SkippedExercisesDialog(
            skippedExercises = skippedExercises,
            onRestore = { ex ->
                SettingsManager.removeSkippedExercise(ctx, ex)
                skippedExercises.remove(ex)
            },
            onDismiss = { showSkipped = false }
        )
    }
}

@Composable
private fun WarmupCard(block: WorkoutBlock) {
    val ctx = LocalContext.current
    FantasyCard(Modifier.fillMaxWidth()) {
        FText(block.title.uppercase(), bold = true, sizeSp = 20, color = FantasyColors.Accent)
        Spacer(Modifier.height(12.dp))
        FText("Empfohlene Dauer: ${block.minutes} Minuten", sizeSp = 14, color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        block.items.forEach { item ->
            val parsedSeconds = remember(item) { parseWarmupTime(item) }
            // Zeitbereich: 30 Sek Minimum, vorgegebene Zeit + 1 Min als Maximum
            val minSeconds = if (parsedSeconds > 0) 30 else 0
            val maxSeconds = if (parsedSeconds > 0) parsedSeconds + 60 else 0

            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    FText("• ", color = FantasyColors.Accent, bold = true)
                    FText(item, sizeSp = 16, modifier = Modifier.weight(1f))
                    Surface(
                        onClick = {
                            val query = URLEncoder.encode("$item Aufwärmen Anleitung korrekt ausführen", "UTF-8")
                            val url = "https://www.youtube.com/results?search_query=$query"
                            try {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "Internet erforderlich", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = Color.Transparent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PlayArrow, "Video", tint = FantasyColors.Accent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                // Anpassbarer Timer (nur wenn Zeitangabe vorhanden)
                if (parsedSeconds > 0) {
                    Spacer(Modifier.height(6.dp))
                    AdjustableTimerRow(
                        exerciseId = "warmup-$item",
                        defaultSeconds = parsedSeconds,
                        minSeconds = minSeconds,
                        maxSeconds = maxSeconds,
                        stepSeconds = 30
                    )
                }
            }
        }
    }
}

@Composable
private fun CooldownCard(block: WorkoutBlock) {
    val ctx = LocalContext.current
    // Standard-Dauer pro Dehnübung: Gesamtminuten / Anzahl Items, mind. 60 Sekunden
    val defaultPerItem = if (block.items.isNotEmpty()) (block.minutes * 60 / block.items.size).coerceAtLeast(60) else 60

    FantasyCard(Modifier.fillMaxWidth()) {
        FText("🧘 ${block.title.uppercase()}", bold = true, sizeSp = 20, color = Color(0xFF4CAF50))
        Spacer(Modifier.height(12.dp))
        FText("Nimm dir ${block.minutes} Minuten Zeit zum Abkühlen", sizeSp = 14, color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        block.items.forEach { item ->
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    FText("• ", color = Color(0xFF4CAF50), bold = true)
                    FText(item, sizeSp = 16, modifier = Modifier.weight(1f))
                    Surface(
                        onClick = {
                            val query = URLEncoder.encode("$item Dehnen Anleitung", "UTF-8")
                            val url = "https://www.youtube.com/results?search_query=$query"
                            try {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "Internet erforderlich", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = Color.Transparent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PlayArrow, "Video", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                AdjustableTimerRow(
                    exerciseId = "cooldown-$item",
                    defaultSeconds = defaultPerItem,
                    minSeconds = 30,
                    maxSeconds = defaultPerItem + 60,
                    stepSeconds = 30,
                    accentColor = Color(0xFF4CAF50)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        FText("✨ Gut gemacht! Dehnen fördert die Regeneration.", sizeSp = 12, color = Color.Gray)
    }
}

/**
 * Dialog: Limit erreicht
 */
@Composable
fun LimitDialog(
    plan: WorkoutPlan,
    selectedType: TrainingType,
    onKeep: () -> Unit,
    onReduce: (WorkoutPlan) -> Unit,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = { onDismiss() },
        containerColor = Color(0xFF151515),
        icon = { Icon(Icons.Default.Warning, "Limit", tint = Color.Yellow) },
        title = { FText("TEMPORÄRES LIMIT ERREICHT", color = FantasyColors.Accent, bold = true) },
        text = {
            Column {
                FText("Dein aktuelles Leistungsniveau ist strukturell ausgereizt.", sizeSp = 14)
                Spacer(Modifier.height(8.dp))
                FText("Du kannst entweder stabil weiter trainieren oder eine leichtere Einheit wählen.", sizeSp = 12, color = Color.Gray)
            }
        },
        confirmButton = {
            FantasyButton("Aktuelle Einheit beibehalten") {
                onKeep()
            }
        },
        dismissButton = {
            FantasyButton("Leichtere Einheit starten", alpha = 0.7f) {
                val reducedEx = plan.exercises.map { block ->
                    if (block.type == "ex") {
                        block.copy(
                            weight = (block.weight * 0.9).coerceAtLeast(0.0),
                            reps = (block.reps * 0.85).roundToInt().coerceAtLeast(1)
                        )
                    } else block
                }
                val reducedPlan = plan.copy(exercises = reducedEx)
                FitnessPersistence.savePlan(ctx, selectedType.name.lowercase(), reducedPlan)
                onReduce(reducedPlan)
            }
        }
    )
}

/**
 * Dialog: Übersprungene Übungen
 */
@Composable
fun SkippedExercisesDialog(
    skippedExercises: List<String>,
    onRestore: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151515),
        title = { FText("Übersprungene Übungen", color = FantasyColors.Accent, bold = true) },
        text = {
            LazyColumn {
                items(skippedExercises) { ex ->
                    Row(Modifier.fillMaxWidth().clickable {
                        onDismiss()
                        onRestore(ex)
                    }.padding(12.dp)) {
                        FText(text = ex, modifier = Modifier.weight(1f))
                        FText("Wiederholen ➔", color = FantasyColors.Accent, sizeSp = 12)
                    }
                }
            }
        },
        confirmButton = { FantasyButton("Schließen") { onDismiss() } }
    )
}

// --- Anpassbarer Timer für zeitbasierte Übungen ---

/**
 * Timer-Zeile mit +/- Buttons zum Anpassen der Dauer.
 * Der User kann die Zeit zwischen [minSeconds] und [maxSeconds] in [stepSeconds]-Schritten einstellen.
 */
@Composable
fun AdjustableTimerRow(
    exerciseId: String,
    defaultSeconds: Int,
    minSeconds: Int,
    maxSeconds: Int,
    stepSeconds: Int = 30,
    accentColor: Color = FantasyColors.Accent
) {
    var currentSeconds by remember(exerciseId) { mutableIntStateOf(defaultSeconds) }
    val displayMin = currentSeconds / 60
    val displaySec = currentSeconds % 60
    val timeLabel = if (displaySec == 0) "${displayMin} Min" else "${displayMin}:${"%02d".format(displaySec)} Min"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Minus-Button
        Surface(
            onClick = { currentSeconds = (currentSeconds - stepSeconds).coerceAtLeast(minSeconds) },
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                FText("−", sizeSp = 18, bold = true, color = accentColor)
            }
        }

        // Zeitanzeige
        FText(timeLabel, sizeSp = 14, bold = true, color = accentColor, modifier = Modifier.padding(horizontal = 8.dp))

        // Plus-Button
        Surface(
            onClick = { currentSeconds = (currentSeconds + stepSeconds).coerceAtMost(maxSeconds) },
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                FText("+", sizeSp = 18, bold = true, color = accentColor)
            }
        }

        Spacer(Modifier.weight(1f))

        // Timer mit der angepassten Zeit
        CompactTimer(exerciseId = "$exerciseId-$currentSeconds", totalSeconds = currentSeconds)
    }
}

// --- Hilfsfunktionen (aus dem Original übernommen) ---

internal fun parseWarmupTime(text: String): Int {
    val lower = text.lowercase()
    val minMatch = Regex("""(\d+)\s*-?\s*(\d+)?\s*min""").find(lower)
    if (minMatch != null) {
        val val1 = minMatch.groupValues[1].toInt()
        val val2 = minMatch.groupValues[2].takeIf { it.isNotEmpty() }?.toInt() ?: val1
        return ((val1 + val2) / 2) * 60
    }
    val secMatch = Regex("""(\d+)\s*sek""").find(lower)
    if (secMatch != null) return secMatch.groupValues[1].toInt()
    return 0
}

internal fun calculateSuccessRate(plan: WorkoutPlan): Float {
    val exerciseBlocks = plan.exercises.filter { it.type == "ex" }
    if (exerciseBlocks.isEmpty()) return 1f
    var totalSets = 0
    var doneSets = 0
    exerciseBlocks.forEach { block ->
        val originalIndex = plan.exercises.indexOf(block)
        val states = plan.savedSetStates[originalIndex] ?: return@forEach
        totalSets += block.sets
        doneSets += states.count { it.isDone }
    }
    return if (totalSets > 0) doneSets.toFloat() / totalSets.toFloat() else 1f
}

internal suspend fun saveWorkoutToDb(db: AppDb, plan: WorkoutPlan, type: String): Boolean {
    // Prüfen ob mindestens ein Set abgehakt wurde
    val anySetDone = plan.exercises.indices.any { index ->
        val block = plan.exercises[index]
        if (block.type != "ex") return@any false
        val states = plan.savedSetStates[index] ?: emptyList()
        states.any { it.isDone }
    }
    if (!anySetDone) return false

    val today = LocalDate.now().toEpochDay()
    db.workoutDao().insert(WorkoutEntry(dateEpochDay = today, type = type))
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
                    difficultyLevel = block.reps, // BODYWEIGHT: Schwierigkeitsstufe
                    timeTargetSeconds = if (exType == ExerciseType.TIME) block.durationSeconds else 0,
                    wasSuccessful = states.all { it.isDone },
                    scoreValue = 0.0
                )
                val setLogs = states.mapIndexed { sIdx, s ->
                    SetLog(
                        exerciseLogId = log.id,
                        setIndex = sIdx + 1,
                        repsDone = if (exType != ExerciseType.TIME) s.reps else null,
                        timeDoneSeconds = if (exType == ExerciseType.TIME) s.duration else null,
                        setSuccess = s.isDone
                    )
                }
                val calculatedScore = ExerciseLogic.calculateScore(log, setLogs)
                db.workoutDao().insertLog(log.copy(scoreValue = calculatedScore))
                db.workoutDao().insertSets(setLogs)
            }
        }
    }
    return true
}

