package com.fantasyfoodplanner.ui

import android.content.*
import android.os.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fantasyfoodplanner.logic.ExerciseLogic
import com.fantasyfoodplanner.logic.ExerciseType
import kotlinx.coroutines.delay

@Composable
fun CompactTimer(
    exerciseId: String,
    totalSeconds: Int,
    onFinish: () -> Unit = {}
) {
    val context = LocalContext.current
    var service by remember { mutableStateOf<TimerService?>(null) }
    var remainingMs by remember { mutableLongStateOf(totalSeconds * 1000L) }
    var isRunning by remember { mutableStateOf(false) }
    var isAlarmActive by remember { mutableStateOf(false) }

    // Service-Verbindung mit sauberem Lifecycle-Management
    DisposableEffect(context) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val s = (binder as? TimerService.TimerBinder)?.getService()
                service = s
                // Initialwerte sofort abgleichen, wenn der Timer zu DIESER Übung gehört
                s?.let {
                    if (it.boundExerciseId == exerciseId) {
                        remainingMs = it.remainingMs
                        isRunning = it.isRunning
                        isAlarmActive = it.isAlarmActive
                    } else {
                        // Timer gehört zu einer anderen Übung -> UI auf Default
                        remainingMs = totalSeconds * 1000L
                        isRunning = false
                        isAlarmActive = false
                    }
                }
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
            }
        }
        
        val intent = Intent(context, TimerService::class.java)
        try {
            // Wir binden nur, gestartet wird der Service explizit im onClick
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {}

        onDispose {
            try {
                context.unbindService(connection)
            } catch (e: Exception) {}
            service = null
        }
    }

    // Live-Update der UI aus dem Service (nur wenn boundExerciseId passt)
    LaunchedEffect(service, exerciseId) {
        while (service != null) {
            service?.let {
                if (it.boundExerciseId == exerciseId) {
                    remainingMs = it.remainingMs
                    isRunning = it.isRunning
                    isAlarmActive = it.isAlarmActive
                } else {
                    isRunning = false
                    isAlarmActive = false
                    remainingMs = totalSeconds * 1000L
                }
            }
            delay(200L) // Schnellere Updates für die UI
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val displayMin = (remainingMs / 1000) / 60
    val displaySec = (remainingMs / 1000) % 60
    val timeText = "%02d:%02d".format(displayMin, displaySec)

    Surface(
        onClick = {
            val intent = Intent(context, TimerService::class.java)
            service?.let { s ->
                if (isAlarmActive) {
                    s.stopAlarm()
                } else if (isRunning) {
                    s.stopTimer()
                } else {
                    // Explizit starten, damit der Service auch nach Unbind (Rotation) weiterläuft
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                    s.startTimer(exerciseId, totalSeconds)
                }
            }
        },
        modifier = Modifier
            .height(42.dp)
            .widthIn(min = 100.dp)
            .scale(pulseScale)
            .clip(CircleShape),
        color = if (isAlarmActive) Color.Red else Color(0xFF1A1A1A),
        border = androidx.compose.foundation.BorderStroke(2.dp, if (isAlarmActive) Color.White else FantasyColors.Accent)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                imageVector = if (isAlarmActive || isRunning) Icons.Default.Refresh else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isAlarmActive) Color.White else FantasyColors.Accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isAlarmActive) "STOP" else timeText,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun UnifiedExerciseCard(
    name: String,
    info: String,
    sets: Int,
    reps: Int,
    weight: Double,
    durationSeconds: Int,
    isChecked: Boolean,
    setStates: List<SetState>,
    onCheckChange: (Boolean) -> Unit,
    onSetStatesChange: (List<SetState>) -> Unit,
    onWeightChange: (Double) -> Unit,
    onClick: () -> Unit,
    onSwap: (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    val exType = remember(name) { ExerciseLogic.getExerciseType(name) }

    Box {
        FantasyCard(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isChecked) FantasyColors.Accent else Color.Transparent)
                            .border(1.dp, FantasyColors.Accent, RoundedCornerShape(6.dp))
                            .clickable { onCheckChange(!isChecked) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) FText("✔", color = FantasyColors.Background, sizeSp = 14, bold = true)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        FText(name, sizeSp = 17, bold = true)
                        FText(info, sizeSp = 12, color = Color.Gray)
                    }
                    if (onSwap != null) {
                        Box(Modifier.size(32.dp).clickable { onSwap() }, contentAlignment = Alignment.Center) {
                            FText("⇄", color = FantasyColors.Accent, sizeSp = 22)
                        }
                    }
                    Spacer(Modifier.width(44.dp))
                }

                if (isExpanded) {
                    Spacer(Modifier.height(16.dp))
                    
                    if (exType == ExerciseType.WEIGHTED) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().background(Color.Black.copy(0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp)
                        ) {
                            FText("Gewicht:", sizeSp = 14, color = Color.Gray, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onWeightChange(weight - 2.5) }) { FText("-", color = FantasyColors.Accent, bold = true) }
                            FText("${weight}kg", bold = true)
                            IconButton(onClick = { onWeightChange(weight + 2.5) }) { FText("+", color = FantasyColors.Accent, bold = true) }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    FText(if(exType == ExerciseType.TIME) "INTERVALLE" else "SÄTZE", sizeSp = 12, bold = true, color = FantasyColors.Accent)
                    setStates.forEachIndexed { index, state ->
                        var localValue by rememberSaveable(state) { mutableStateOf(if (exType == ExerciseType.TIME) state.duration.takeIf { it > 0 }?.toString() ?: "" else state.reps.takeIf { it > 0 }?.toString() ?: "") }

                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            FText(if(exType == ExerciseType.TIME) "Int. ${index + 1}" else "Satz ${index + 1}", sizeSp = 14, modifier = Modifier.width(60.dp))
                            TextField(
                                value = localValue,
                                onValueChange = { input ->
                                    val filtered = input.filter { c -> c.isDigit() }
                                    localValue = filtered
                                    val v = if (filtered.isEmpty()) null else filtered.toIntOrNull()
                                    val newStates = setStates.toMutableList()
                                    newStates[index] = if (exType == ExerciseType.TIME) state.copy(duration = v ?: 0) else state.copy(reps = v ?: 0)
                                    onSetStatesChange(newStates)
                                },
                                modifier = Modifier.width(70.dp).height(48.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = FantasyColors.Accent,
                                    cursorColor = FantasyColors.Accent,
                                    focusedTextColor = FantasyColors.Text,
                                    unfocusedTextColor = FantasyColors.Text,
                                    focusedLabelColor = FantasyColors.GrayText,
                                    unfocusedLabelColor = FantasyColors.GrayText
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(color = FantasyColors.Text, textAlign = TextAlign.Center)
                            )
                            FText(if(exType == ExerciseType.TIME) "sek" else "Reps", sizeSp = 12, color = Color.Gray, modifier = Modifier.padding(start = 4.dp).weight(1f))
                            if (exType == ExerciseType.TIME && state.duration > 0) {
                                CompactTimer(exerciseId = "$name-set-$index", totalSeconds = state.duration)
                                Spacer(Modifier.width(8.dp))
                            }
                            // Korrektur: Checkbox-Parameter explizit typisiert
                            Checkbox(
                                checked = state.isDone,
                                onCheckedChange = { checked: Boolean ->
                                    val newStates = setStates.toMutableList()
                                    newStates[index] = state.copy(isDone = checked)
                                    if (newStates.all { s -> s.isDone }) onCheckChange(true)
                                    onSetStatesChange(newStates)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = FantasyColors.Accent, uncheckedColor = Color.Gray)
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val summaryText = when(exType) {
                            ExerciseType.TIME -> "$sets Intervalle à $durationSeconds sek"
                            ExerciseType.WEIGHTED -> "$sets Sätze à $reps Reps @ ${weight}kg"
                            ExerciseType.BODYWEIGHT -> "$sets Sätze à $reps Reps"
                        }
                        FText(summaryText, sizeSp = 13, color = FantasyColors.GrayText, modifier = Modifier.padding(start = 40.dp).weight(1f))
                        
                        if (exType == ExerciseType.TIME && durationSeconds > 0) {
                            CompactTimer(exerciseId = name, totalSeconds = durationSeconds)
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }
            }
        }

        Surface(
            onClick = onClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(36.dp),
            shape = CircleShape,
            color = FantasyColors.Accent.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, FantasyColors.Accent)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Info, 
                    contentDescription = "Details", 
                    tint = FantasyColors.Accent, 
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

data class SetState(
    val reps: Int = 0,
    val duration: Int = 0,
    val isDone: Boolean = false
)
