package com.fantasyfoodplanner.features

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fantasyfoodplanner.data.AppDb
import com.fantasyfoodplanner.ui.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PlannerScreen(onBack: () -> Unit, onOpenDay: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val ctx = LocalContext.current
    val db = remember { AppDb.get(ctx) }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    // Lade Tage mit Mahlzeiten und Workouts
    val mealDays by db.mealDao().getAll().collectAsState(initial = emptyList())
    val manualMealDays by db.manualMealDao().getAll().collectAsState(initial = emptyList())
    val workoutDays by db.workoutDao().getAllWorkouts().collectAsState(initial = emptyList())
    val allLogs by db.workoutDao().getAllWithSets().collectAsState(initial = emptyList())

    val mealEpochDays = remember(mealDays) { mealDays.map { it.dateEpochDay }.toSet() }
    val manualMealEpochDays = remember(manualMealDays) { manualMealDays.map { it.dateEpochDay }.toSet() }

    // Workout-Tage mit Kategorie-Status: 1=voll, 2=ergänzend, 3=leicht
    val workoutDayCategories = remember(workoutDays, allLogs) {
        val workoutsByDay = workoutDays.groupBy { it.dateEpochDay }
        val logsByDay = allLogs.groupBy { it.log.dateEpochDay }

        workoutsByDay.mapValues { (epochDay, dayWorkouts) ->
            val hasRegular = dayWorkouts.any { it.type != "OTHER_ACTIVITY" }
            if (hasRegular) {
                1 // Volles Workout
            } else {
                // Nur OTHER_ACTIVITY — Kategorie aus weightKg lesen (1.0/2.0/3.0)
                val otherLogs = (logsByDay[epochDay] ?: emptyList()).filter { it.log.exerciseType == "OTHER" }
                val bestCategory = otherLogs.minOfOrNull { it.log.weightKg } ?: 3.0
                when {
                    bestCategory <= 1.0 -> 1
                    bestCategory <= 2.0 -> 2
                    else -> 3
                }
            }
        }
    }

    val glowGreen = Color(0xFF00FF7F)

    FantasySurface {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
            MainAppBar("Tagesübersicht", onBack = onBack)
            Spacer(Modifier.height(12.dp))
            
            // ══════ MONAT NAVIGATION ══════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(FantasyColors.CardBg, Color(0xFF0D1A0F), FantasyColors.CardBg)
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, glowGreen.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(glowGreen.copy(alpha = 0.08f))
                        .border(1.dp, glowGreen.copy(alpha = 0.25f), CircleShape)
                        .clickable { currentMonth = currentMonth.minusMonths(1) },
                    contentAlignment = Alignment.Center
                ) {
                    FText("◂", sizeSp = 18, bold = true, color = glowGreen)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FText(
                        text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.GERMAN)
                            .replaceFirstChar { it.uppercase() },
                        sizeSp = 20,
                        bold = true,
                        color = FantasyColors.Accent
                    )
                    FText("${currentMonth.year}", sizeSp = 12, color = FantasyColors.GrayText)
                }

                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(glowGreen.copy(alpha = 0.08f))
                        .border(1.dp, glowGreen.copy(alpha = 0.25f), CircleShape)
                        .clickable { currentMonth = currentMonth.plusMonths(1) },
                    contentAlignment = Alignment.Center
                ) {
                    FText("▸", sizeSp = 18, bold = true, color = glowGreen)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ══════ WOCHENTAG HEADER ══════
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEach { day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        FText(day, sizeSp = 12, bold = true, color = FantasyColors.GrayText)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // ══════ KALENDER-GRID ══════
            val firstDayOfMonth = currentMonth.atDay(1)
            val startOffset = (firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
            val daysInMonth = currentMonth.lengthOfMonth()
            val totalCells = startOffset + daysInMonth
            val rows = (totalCells + 6) / 7

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(rows) { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - startOffset + 1

                            if (dayNum in 1..daysInMonth) {
                                val date = currentMonth.atDay(dayNum)
                                val epochDay = date.toEpochDay()
                                val isToday = date == today
                                val hasMeals = mealEpochDays.contains(epochDay) || manualMealEpochDays.contains(epochDay)
                                val workoutCategory = workoutDayCategories[epochDay] ?: 0
                                val hasWorkout = workoutCategory > 0
                                val isPast = date.isBefore(today)

                                CalendarDayCell(
                                    dayNum = dayNum,
                                    isToday = isToday,
                                    isPast = isPast,
                                    hasMeals = hasMeals,
                                    hasWorkout = hasWorkout,
                                    workoutCategory = workoutCategory,
                                    glowGreen = glowGreen,
                                    onClick = { onOpenDay(date) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Box(Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                }

                // Legende
                item {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(FantasyColors.CardBg.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PlannerLegendDot(color = Color(0xFF00FF7F), label = "Mahlzeit")
                        PlannerLegendDot(color = FantasyColors.Accent, label = "Training")
                        PlannerLegendDot(color = Color(0xFFFFB347), label = "Aktivität")
                        PlannerLegendDot(color = Color.Red, label = "Heute")
                    }
                }

                // Schnell-Zugriff
                item {
                    Spacer(Modifier.height(12.dp))
                    if (currentMonth == YearMonth.now()) {
                        FantasyButton(
                            label = "📅 Heute planen",
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            onOpenDay(today)
                        }
                    } else {
                        FantasyButton(
                            label = "📅 Zum aktuellen Monat",
                            modifier = Modifier.fillMaxWidth(),
                            alpha = 0.7f
                        ) {
                            currentMonth = YearMonth.now()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNum: Int,
    isToday: Boolean,
    isPast: Boolean,
    hasMeals: Boolean,
    hasWorkout: Boolean,
    workoutCategory: Int = 0, // 0=keins, 1=voll, 2=ergänzend, 3=leicht
    glowGreen: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Workout-Dot Farbe je nach Kategorie
    val workoutDotColor = when (workoutCategory) {
        1 -> FantasyColors.Accent           // Blau — volles Workout
        2 -> Color(0xFFFFB347)              // Orange — ergänzende Aktivität
        3 -> Color(0xFF888888)              // Grau — leichte Bewegung
        else -> FantasyColors.Accent        // Fallback
    }

    val bgColor = when {
        isToday -> Color(0xFF1A0A0A)
        isPast -> FantasyColors.CardBg.copy(alpha = 0.4f)
        else -> FantasyColors.CardBg.copy(alpha = 0.7f)
    }
    val borderColor = when {
        isToday -> Color.Red.copy(alpha = 0.6f)
        hasMeals || hasWorkout -> glowGreen.copy(alpha = 0.2f)
        else -> Color.White.copy(alpha = 0.05f)
    }
    val textColor = when {
        isToday -> Color.Red
        isPast -> FantasyColors.GrayText.copy(alpha = 0.6f)
        else -> FantasyColors.Text
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            FText(
                "$dayNum",
                sizeSp = if (isToday) 16 else 14,
                bold = isToday,
                color = textColor
            )
            if (hasMeals || hasWorkout) {
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (hasMeals) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(glowGreen)
                        )
                    }
                    if (hasWorkout) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(workoutDotColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlannerLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        FText(label, sizeSp = 11, color = FantasyColors.GrayText)
    }
}
