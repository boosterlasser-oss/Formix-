package com.fantasyfoodplanner.features.fitness

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fantasyfoodplanner.data.AppDb
import com.fantasyfoodplanner.data.ExerciseWithSets
import com.fantasyfoodplanner.logic.ExerciseLogic
import com.fantasyfoodplanner.logic.ExerciseType
import com.fantasyfoodplanner.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WorkoutStatsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { AppDb.get(ctx) }
    val scope = rememberCoroutineScope()
    val logsWithSets by db.workoutDao().getAllWithSets().collectAsState(initial = emptyList(), context = Dispatchers.Main.immediate)
    
    var timeFilter by remember { mutableStateOf("Woche") } 
    var expandedExercise by remember { mutableStateOf<String?>(null) }

    val filteredLogs = remember(logsWithSets, timeFilter) {
        val now = LocalDate.now()
        val limit = when(timeFilter) {
            "Tag" -> now.toEpochDay()
            "Woche" -> now.minusWeeks(1).toEpochDay()
            "Monat" -> now.minusMonths(1).toEpochDay()
            "Jahr" -> now.minusYears(1).toEpochDay()
            else -> 0L
        }
        logsWithSets.filter { it.log.dateEpochDay >= limit }
    }

    val exercisesByName = remember(filteredLogs) {
        filteredLogs.groupBy { it.log.exerciseName }.toSortedMap()
    }

    Box(Modifier.fillMaxSize()) {
        FantasySurface {
            Column(Modifier.fillMaxSize()) {
                MainAppBar(title = "Detaillierte Statistik", onBack = onBack)

                // Tab-Auswahl für Zeitfilter
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Tag", "Woche", "Monat", "Jahr").forEach { filter ->
                        Box(
                            Modifier
                                .weight(1f)
                                .background(
                                    if (timeFilter == filter) FantasyColors.Accent else FantasyColors.Accent.copy(0.08f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { timeFilter = filter }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center) {
                            FText(filter, color = if(timeFilter == filter) Color.White else FantasyColors.Text, sizeSp = 12, bold = true)
                        }
                    }
                }

                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        FText("TRAININGSFORTSCHRITT", color = FantasyColors.Accent, bold = true, sizeSp = 14)
                    }

                    if (exercisesByName.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                FText("Keine Einheiten in diesem Zeitraum.", color = Color.Gray)
                            }
                        }
                    } else {
                        items(exercisesByName.keys.toList()) { exerciseName ->
                            val history = exercisesByName[exerciseName]!!
                            ExerciseHistoryCard(
                                exerciseName = exerciseName,
                                history = history,
                                isExpanded = expandedExercise == exerciseName,
                                onToggle = { 
                                    expandedExercise = if (expandedExercise == exerciseName) null else exerciseName
                                },
                                onDeleteLog = {
                                    scope.launch { db.workoutDao().deleteLog(it.log) }
                                }
                            )
                        }
                    }
                }
            }
        }
        HelpFloatingButton("Stats")
    }
}

@Composable
private fun ExerciseHistoryCard(
    exerciseName: String,
    history: List<ExerciseWithSets>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDeleteLog: (ExerciseWithSets) -> Unit
) {
    val latestLog = history.first().log

    FantasyCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(bottom = if(isExpanded) 12.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    FText(exerciseName, bold = true, sizeSp = 18, color = FantasyColors.Secondary)
                    FText("Letzte Einheit: ${LocalDate.ofEpochDay(latestLog.dateEpochDay).format(DateTimeFormatter.ofPattern("dd.MM.yy"))}", sizeSp = 11, color = Color.Gray)
                }
                FText(if(isExpanded) "▲" else "▼", color = FantasyColors.Accent, sizeSp = 20)
            }

            if (isExpanded) {
                Spacer(Modifier.height(8.dp))
                FText("VERLAUF & WIEDERHOLUNGEN", sizeSp = 12, bold = true, color = FantasyColors.Accent)
                history.forEach { entry ->
                    LogEntryCard(entry, onDelete = { onDeleteLog(entry) })
                }
            }
        }
    }
}

@Composable
private fun LogEntryCard(entry: ExerciseWithSets, onDelete: () -> Unit) {
    val log = entry.log
    val isOtherActivity = log.exerciseType == "OTHER"
    val exType = if (isOtherActivity) null else ExerciseLogic.getExerciseType(log.exerciseName)
    val totalReps = entry.sets.sumOf { it.repsDone ?: 0 }
    val repsList = entry.sets.map { it.repsDone ?: 0 }.joinToString(" | ")
    val timeList = entry.sets.map { "${it.timeDoneSeconds ?: 0}s" }.joinToString(" | ")

    val dateStr = LocalDate.ofEpochDay(log.dateEpochDay).format(DateTimeFormatter.ofPattern("dd.MM.yy"))
    
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(0.2f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FText(dateStr, sizeSp = 13, color = FantasyColors.Gold, bold = true)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier=Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, "Löschen", tint = Color.Red.copy(0.4f), modifier=Modifier.size(16.dp))
                }
            }
            
            Spacer(Modifier.height(4.dp))

            if (isOtherActivity) {
                // Andere Sportart / Aktivität — Dauer + Intensität anzeigen
                val durationMin = log.totalRepsDone
                val intensityLabel = when (log.difficultyLevel) {
                    1 -> "Leicht"
                    2 -> "Mittel"
                    3 -> "Anstrengend"
                    4 -> "Sehr anstrengend"
                    else -> "Unbekannt"
                }
                val catLabel = when (log.weightKg.toInt()) {
                    1 -> "Volles Workout"
                    2 -> "Ergänzend"
                    3 -> "Leichte Bewegung"
                    else -> "Aktivität"
                }
                val catColor = when (log.weightKg.toInt()) {
                    1 -> FantasyColors.Accent
                    2 -> Color(0xFFFFB347)
                    else -> Color(0xFF888888)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        FText("$intensityLabel · $catLabel", sizeSp = 15, bold = true, color = catColor)
                        FText("Fokus: ${log.workoutType}", sizeSp = 12, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        FText("MIN", sizeSp = 10, color = catColor, bold = true)
                        FText(
                            text = "$durationMin",
                            sizeSp = 22,
                            bold = true,
                            color = Color.White
                        )
                    }
                }
            } else {
                // Reguläres Training — wie bisher
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        val mainInfo = when(exType) {
                            ExerciseType.WEIGHTED -> "${log.weightKg}kg"
                            ExerciseType.BODYWEIGHT -> "Level ${log.difficultyLevel}"
                            ExerciseType.TIME -> "Ausdauer"
                            else -> ""
                        }
                        FText(mainInfo, sizeSp = 15, bold = true)
                        
                        val detailInfo = if(exType == ExerciseType.TIME) "Sätze: $timeList" else "Sätze: $repsList"
                        FText(detailInfo, sizeSp = 12, color = Color.Gray)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        FText(if(exType == ExerciseType.TIME) "SEK" else "REPS", sizeSp = 10, color = FantasyColors.Accent, bold = true)
                        FText(
                            text = if(exType == ExerciseType.TIME) "${entry.sets.sumOf { it.timeDoneSeconds ?: 0 }}" else "$totalReps",
                            sizeSp = 22,
                            bold = true,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
