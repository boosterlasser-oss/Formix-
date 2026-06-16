package com.fantasyfoodplanner.features.fitness

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fantasyfoodplanner.data.*
import com.fantasyfoodplanner.logic.*
import com.fantasyfoodplanner.ui.*
import com.fantasyfoodplanner.utils.NutrientCalculator
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// ── Datenklassen ───────────────────────────────────────────────

data class ChartPoint(val label: String, val volume: Double, val kcal: Double)

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

data class StatsResult(
    val logEntryCount: Int,
    val exerciseCount: Int,
    val totalScore: Double,
    val totalSets: Int,
    val totalReps: Int,
    val totalVolume: Double,
    val successRate: Float,
    val avgKcal: Int,
    val mealCount: Int,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val avgProtein: Double,
    val avgCarbs: Double,
    val avgFat: Double,
    val chartPoints: List<ChartPoint>,
    val consistency: TrainingConsistencyCalculator.ConsistencyStats,
    val currentWeight: Double? = null,
    val weightChange: Double = 0.0
)

// ── Berechnung ─────────────────────────────────────────────────

fun calculateStatsInternal(
    tab: String,
    logsWithSets: List<ExerciseWithSets>,
    manualMeals: List<ManualMealEntry>,
    mealEntries: List<MealEntry>,
    workoutEntries: List<WorkoutEntry>,
    recipes: List<Recipe>,
    products: List<Product>,
    weights: List<WeightEntry>
): StatsResult {
    val now = LocalDate.now()
    val filteredLogs = when (tab) {
        "Tag" -> logsWithSets.filter { it.log.dateEpochDay == now.toEpochDay() }
        "Woche" -> logsWithSets.filter { it.log.dateEpochDay >= now.minusDays(7).toEpochDay() }
        "Monat" -> logsWithSets.filter { it.log.dateEpochDay >= now.minusDays(30).toEpochDay() }
        else -> logsWithSets.filter { it.log.dateEpochDay >= now.minusDays(365).toEpochDay() }
    }
    // Echte Fitness-Logs (kein OTHER_ACTIVITY) für gewichtsbasierte Metriken
    // OTHER_ACTIVITY nutzt weightKg als Kategorie-Code (1.0/2.0/3.0), nicht als echtes Gewicht
    val fitnessLogs = filteredLogs.filter { it.log.exerciseType != "OTHER" }
    val logEntryCount = filteredLogs.size
    val exerciseCount = filteredLogs.map { it.log.exerciseName }.distinct().size
    val totalScore = filteredLogs.sumOf { it.log.scoreValue }
    val totalSets = fitnessLogs.sumOf { it.sets.size }
    val totalReps = fitnessLogs.sumOf { it.sets.sumOf { s -> s.repsDone ?: 0 } }
    val totalVolume = fitnessLogs.sumOf { l -> l.sets.sumOf { (it.repsDone ?: 0) * l.log.weightKg } }
    // Erfolgsrate: nur echte Übungen (Sätze bei Aktivitäten sind Platzhalter)
    val totalPlannedSets = fitnessLogs.sumOf { it.log.plannedSets }
    val totalSuccessfulSets = fitnessLogs.sumOf { l -> l.sets.count { it.setSuccess } }
    val successRate = if (totalPlannedSets > 0) totalSuccessfulSets.toFloat() / totalPlannedSets.toFloat() else 0f
    val steps = when(tab) {
        "Tag" -> 1
        "Woche" -> 7
        "Monat" -> 30
        else -> 30
    }
    // Für Charts immer mind. 7 Punkte zeigen (Kontext), auch bei "Tag"
    val chartSteps = when(tab) {
        "Tag" -> 7
        else -> steps
    }
    val points = mutableListOf<ChartPoint>()
    var totalKcalSum = 0.0
    var totalProt = 0.0; var totalCarb = 0.0; var totalFat = 0.0
    var activeDays = 0 // Nur Tage zählen an denen tatsächlich etwas eingetragen wurde
    for (i in 0 until chartSteps) {
        val d = now.minusDays(i.toLong())
        val dE = d.toEpochDay()
        val dayVol = fitnessLogs.filter { it.log.dateEpochDay == dE }.sumOf { l -> l.sets.sumOf { (it.repsDone ?: 0) * l.log.weightKg } }
        val dManual = manualMeals.filter { it.dateEpochDay == dE }
        val dEntries = mealEntries.filter { it.dateEpochDay == dE }
        val dayTotals = NutrientCalculator.calculateTotals(dEntries, dManual, recipes, products)

        // Nur Tage im eigentlichen Zeitraum (steps) für Ø-Berechnung zählen
        if (i < steps) {
            val hasFood = dEntries.isNotEmpty() || dManual.isNotEmpty()
            val hasTraining = filteredLogs.any { it.log.dateEpochDay == dE }
            if (hasFood || hasTraining) {
                activeDays++
            }
            totalKcalSum += dayTotals.kcal
            totalProt += dayTotals.p
            totalCarb += dayTotals.c
            totalFat += dayTotals.f
        }

        points.add(ChartPoint(d.dayOfMonth.toString(), dayVol, dayTotals.kcal.toDouble()))
    }
    val consistency = TrainingConsistencyCalculator.calculate(workoutEntries, logsWithSets.map { it.log })
    // Durchschnitt nur über aktive Tage berechnen (Pause/Krankheit wird ignoriert)
    val avgDivisor = if (activeDays > 0) activeDays else 1

    // Mahlzeiten nur für den gewählten Zeitraum zählen
    val startEpoch = now.minusDays((steps - 1).toLong()).toEpochDay()
    val filteredMealCount = mealEntries.count { it.dateEpochDay >= startEpoch } +
            manualMeals.count { it.dateEpochDay >= startEpoch }

    // Gewichtsveränderung über den gewählten Zeitraum
    val currentW = weights.firstOrNull()?.weightKg
    val periodStartEpoch = now.minusDays((steps - 1).toLong()).toEpochDay()
    val oldestWeightInPeriod = weights.lastOrNull { it.dateEpochDay >= periodStartEpoch }?.weightKg
        ?: weights.getOrNull(1)?.weightKg
    val wChange = if (currentW != null && oldestWeightInPeriod != null) currentW - oldestWeightInPeriod else 0.0

    return StatsResult(
        logEntryCount = logEntryCount,
        exerciseCount = exerciseCount,
        totalScore = totalScore,
        totalSets = totalSets,
        totalReps = totalReps,
        totalVolume = totalVolume,
        successRate = successRate.coerceIn(0f, 1f),
        avgKcal = (totalKcalSum / avgDivisor).roundToInt(),
        mealCount = filteredMealCount,
        totalProtein = totalProt,
        totalCarbs = totalCarb,
        totalFat = totalFat,
        avgProtein = totalProt / avgDivisor,
        avgCarbs = totalCarb / avgDivisor,
        avgFat = totalFat / avgDivisor,
        chartPoints = points.reversed(),
        consistency = consistency,
        currentWeight = currentW,
        weightChange = wChange
    )
}

// ── Hilfs-Composables ──────────────────────────────────────────

@Composable
fun SummaryItem(modifier: Modifier = Modifier, label: String, value: String, unit: String) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FText(value, sizeSp = 20, bold = true, color = FantasyColors.Accent)
        FText(label, sizeSp = 10, color = Color.Gray)
        FText(unit, sizeSp = 9, color = Color.Gray)
    }
}

@Composable
fun IntensityCard(data: StatsResult) {
    FantasyCard {
        FText("INTENSITÄTS-ANALYSE", color = Color.Magenta, bold = true, sizeSp = 12)
        Spacer(Modifier.height(8.dp))
        val avgVol = if (data.logEntryCount > 0) data.totalVolume / data.logEntryCount else 0.0
        FText("Ø Volumen pro Einheit: ${avgVol.roundToInt()} kg", sizeSp = 14, color = FantasyColors.Text)
        Spacer(Modifier.height(4.dp))
        val avgSets = if (data.logEntryCount > 0) data.totalSets.toFloat() / data.logEntryCount else 0f
        FText("Ø Sätze pro Einheit: ${"%.1f".format(avgSets)}", sizeSp = 14, color = FantasyColors.Text)
        Spacer(Modifier.height(8.dp))
        val progress = data.successRate
        FText("Erfolgsrate: ${(progress * 100).toInt()}%", sizeSp = 12, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(Color.DarkGray, RoundedCornerShape(6.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(
                        if (progress < 0.4f) Color.Green else if (progress < 0.8f) Color.Yellow else Color.Red,
                        RoundedCornerShape(6.dp)
                    )
            )
        }
    }
}

// ── Entwicklungskurven ─────────────────────────────────────────

@Composable
fun DevelopmentCurveCard(data: StatsResult, moduleSelection: ModuleSelection = ModuleSelection.BOTH, lazyListState: LazyListState? = null, itemIndex: Int = 0, onGoFitness: (() -> Unit)? = null, onGoPlanner: (() -> Unit)? = null) {
    // Tabs je nach Modul filtern
    val allTabs = mutableListOf<Pair<String, Int>>() // label -> index (0=Training, 1=Ernährung)
    if (moduleSelection != ModuleSelection.NUTRITION_ONLY) {
        allTabs.add("🏋️ Training" to 0)
    }
    if (moduleSelection != ModuleSelection.FITNESS_ONLY) {
        allTabs.add("🍎 Ernährung" to 1)
    }

    var selectedCurveTabIndex by remember { mutableIntStateOf(0) }
    FantasyCard {
        Column {
            FText("ENTWICKLUNGSKURVEN", color = FantasyColors.Accent, bold = true, sizeSp = 12)
            Spacer(Modifier.height(4.dp))
            FText("Dein Fortschritt im Überblick", sizeSp = 10, color = Color.Gray)
            Spacer(Modifier.height(12.dp))

            // Tabs nur anzeigen wenn mehr als 1 vorhanden
            if (allTabs.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FantasyColors.Accent.copy(0.08f), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    allTabs.forEachIndexed { idx, (title, _) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selectedCurveTabIndex == idx)
                                        if (allTabs[idx].second == 0) FantasyColors.Accent else FantasyColors.Gold
                                    else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedCurveTabIndex = idx }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            FText(
                                title,
                                sizeSp = 12,
                                bold = selectedCurveTabIndex == idx,
                                color = if (selectedCurveTabIndex == idx) Color.White else FantasyColors.Text
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Inhalt je nach ausgewähltem Tab
            val currentTabType = allTabs.getOrNull(selectedCurveTabIndex)?.second ?: 0
            when (currentTabType) {
                0 -> TrainingCurveContent(data, lazyListState, itemIndex, onGoFitness)
                1 -> NutritionCurveContent(data, onGoPlanner)
            }
        }
    }
}

@Composable
fun TrainingCurveContent(data: StatsResult, lazyListState: LazyListState? = null, itemIndex: Int = 0, onGoFitness: (() -> Unit)? = null) {
    var showTable by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FantasyColors.Accent.copy(0.1f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MiniStatItem("📊", "Volumen", "${(data.totalVolume / 1000).toInt()}t")
            MiniStatItem("🔄", "Sets", "${data.totalSets}")
            MiniStatItem("💪", "Reps", "${data.totalReps}")
            MiniStatItem("✅", "Erfolg", "${(data.successRate * 100).toInt()}%")
        }
        Spacer(Modifier.height(12.dp))
        FText("Trainingsvolumen", sizeSp = 10, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        TrainingVolumeChart(data.chartPoints, onGoFitness)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            LegendItem(FantasyColors.Accent, "Volumen (kg)")
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            FantasyButton(
                label = if (showTable) "Tabelle ausblenden" else "Tabelle anzeigen",
                modifier = Modifier,
                alpha = 0.95f
            ) {
                showTable = !showTable
                if (showTable && lazyListState != null) {
                    coroutineScope.launch {
                        // Scroll zum Entwicklungskurven-Item (letztes Item) damit die Tabelle sichtbar wird
                        lazyListState.animateScrollToItem(itemIndex)
                    }
                }
            }
        }
        if (showTable) {
            Spacer(Modifier.height(12.dp))
            TrainingImprovementTable()
        }
    }
}

@Composable
fun NutritionCurveContent(data: StatsResult, onGoPlanner: (() -> Unit)? = null) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FantasyColors.Gold.copy(0.1f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MiniStatItem("🔥", "Ø kcal", "${data.avgKcal}")
            MiniStatItem("🥩", "Ø Protein", "${data.avgProtein.roundToInt()}g")
            MiniStatItem("🍞", "Ø Carbs", "${data.avgCarbs.roundToInt()}g")
            MiniStatItem("🧈", "Ø Fett", "${data.avgFat.roundToInt()}g")
        }
        Spacer(Modifier.height(12.dp))
        FText("Kalorienaufnahme", sizeSp = 10, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        NutritionCalorieChart(data.chartPoints, onGoPlanner)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            LegendItem(FantasyColors.Gold, "Kalorien (kcal)")
        }
    }
}

@Composable
fun TrainingImprovementTable() {
    val ctx = LocalContext.current
    val db = remember { AppDb.get(ctx) }

    // Letzte 10 Workout-Tage aus der DB laden
    val workoutData by remember {
        combine(
            db.workoutDao().getAllWorkouts(),
            db.workoutDao().getAllWithSets()
        ) { workouts, logsWithSets ->
            // Gruppiere nach Tag, sortiere absteigend, maximal 10 Tage
            val dayMap = workouts
                .sortedByDescending { it.dateEpochDay }
                .distinctBy { it.dateEpochDay }
                .take(10)

            dayMap.map { workout ->
                val dayLogs = logsWithSets.filter { it.log.dateEpochDay == workout.dateEpochDay }

                if (workout.type == "OTHER_ACTIVITY") {
                    // Andere Sportart — zeige Sportname + Dauer + Kategorie
                    val otherLog = dayLogs.firstOrNull { it.log.exerciseType == "OTHER" }?.log
                    val sportName = otherLog?.exerciseName ?: "Aktivität"
                    val durationMin = otherLog?.totalRepsDone ?: 0
                    val catCode = when (otherLog?.weightKg?.toInt()) {
                        1 -> 1; 2 -> 2; 3 -> 3; else -> 0
                    }
                    TrainingDayRow(
                        dateEpochDay = workout.dateEpochDay,
                        type = "Aktivität",
                        exerciseCount = 1,
                        totalSets = 0,
                        totalVolume = 0.0,
                        successRate = 1f,
                        sportName = sportName,
                        durationMinutes = durationMin,
                        activityCategory = catCode
                    )
                } else {
                    val totalSets = dayLogs.sumOf { it.sets.size }
                    val successfulSets = dayLogs.sumOf { l -> l.sets.count { it.setSuccess } }
                    val totalVolume = dayLogs.sumOf { l -> l.sets.sumOf { (it.repsDone ?: 0) * l.log.weightKg } }
                    val exerciseCount = dayLogs.map { it.log.exerciseName }.distinct().size
                    val successRate = if (totalSets > 0) successfulSets.toFloat() / totalSets else 0f

                    TrainingDayRow(
                        dateEpochDay = workout.dateEpochDay,
                        type = workout.type,
                        exerciseCount = exerciseCount,
                        totalSets = totalSets,
                        totalVolume = totalVolume,
                        successRate = successRate
                    )
                }
            }
        }
    }.collectAsState(initial = emptyList())

    val dateFormat = remember { java.text.SimpleDateFormat("dd.MM.yy", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1608),
                        Color(0xFF0D0B01)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FText("Datum", sizeSp = 10, color = FantasyColors.Gold, modifier = Modifier.weight(1.2f), bold = true)
                FText("Typ", sizeSp = 10, color = FantasyColors.Gold, modifier = Modifier.weight(1f), bold = true)
                FText("Übungen", sizeSp = 10, color = FantasyColors.Gold, modifier = Modifier.weight(0.8f), bold = true)
                FText("Volumen", sizeSp = 10, color = FantasyColors.Gold, modifier = Modifier.weight(1f), bold = true)
                FText("Erfolg", sizeSp = 10, color = FantasyColors.Gold, modifier = Modifier.weight(0.8f), bold = true)
            }
            Spacer(Modifier.height(6.dp))
            if (workoutData.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FText("📝", sizeSp = 24)
                        Spacer(Modifier.height(8.dp))
                        FText("Noch keine Trainings aufgezeichnet", sizeSp = 12, color = Color.Gray)
                    }
                }
            } else {
                workoutData.forEach { row ->
                    val dateMillis = LocalDate.ofEpochDay(row.dateEpochDay)
                        .let { java.util.GregorianCalendar(it.year, it.monthValue - 1, it.dayOfMonth).timeInMillis }

                    if (row.activityCategory > 0) {
                        // OTHER_ACTIVITY Zeile — Sportname + Dauer statt Volumen/Erfolg
                        val catLabel = when (row.activityCategory) {
                            1 -> "Voll"
                            2 -> "Ergänzend"
                            else -> "Leicht"
                        }
                        val catColor = when (row.activityCategory) {
                            1 -> FantasyColors.Accent
                            2 -> Color(0xFFFFB347)
                            else -> Color(0xFF888888)
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    color = catColor.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FText(dateFormat.format(Date(dateMillis)), sizeSp = 11, color = Color.White, modifier = Modifier.weight(1.2f))
                            FText(row.sportName, sizeSp = 11, color = Color.White, modifier = Modifier.weight(1f))
                            FText("${row.durationMinutes}m", sizeSp = 11, color = Color.White, modifier = Modifier.weight(0.8f))
                            FText(catLabel, sizeSp = 11, color = catColor, modifier = Modifier.weight(1f), bold = true)
                            FText("✓", sizeSp = 11, color = catColor, modifier = Modifier.weight(0.8f), bold = true)
                        }
                    } else {
                        // Reguläres Workout
                        val successPercent = (row.successRate * 100).toInt()
                        val statusColor = when {
                            successPercent >= 80 -> Color(0xFF00FF7F)
                            successPercent >= 50 -> FantasyColors.Accent
                            else -> Color(0xFFFF4C4C)
                        }
                        val volumeText = if (row.totalVolume >= 1000) "${"%.1f".format(row.totalVolume / 1000)}t" else "${row.totalVolume.toInt()}kg"

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    color = statusColor.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FText(dateFormat.format(Date(dateMillis)), sizeSp = 11, color = Color.White, modifier = Modifier.weight(1.2f))
                            FText(row.type, sizeSp = 11, color = Color.White, modifier = Modifier.weight(1f))
                            FText("${row.exerciseCount}", sizeSp = 11, color = Color.White, modifier = Modifier.weight(0.8f))
                            FText(volumeText, sizeSp = 11, color = Color.White, modifier = Modifier.weight(1f))
                            FText("${successPercent}%", sizeSp = 11, color = statusColor, modifier = Modifier.weight(0.8f), bold = true)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

private data class TrainingDayRow(
    val dateEpochDay: Long,
    val type: String,
    val exerciseCount: Int,
    val totalSets: Int,
    val totalVolume: Double,
    val successRate: Float,
    val sportName: String = "",       // Für OTHER_ACTIVITY: Name der Sportart
    val durationMinutes: Int = 0,     // Für OTHER_ACTIVITY: Dauer in Minuten
    val activityCategory: Int = 0     // 0=normal, 1=FULL_WORKOUT, 2=SUPPLEMENTARY, 3=LIGHT_MOVEMENT
)

@Composable
fun MiniStatItem(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FText(emoji, sizeSp = 16)
        FText(value, sizeSp = 14, bold = true, color = FantasyColors.Text)
        FText(label, sizeSp = 9, color = Color.Gray)
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(4.dp))
        FText(label, sizeSp = 10, color = Color.Gray)
    }
}

// ── Charts ─────────────────────────────────────────────────────

@Composable
fun TrainingVolumeChart(points: List<ChartPoint>, onGoFitness: (() -> Unit)? = null) {
    if (points.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FText("🏋️", sizeSp = 28)
                Spacer(Modifier.height(8.dp))
                FText("Noch keine Trainingsdaten", sizeSp = 12, color = Color.Gray)
                if (onGoFitness != null) {
                    Spacer(Modifier.height(12.dp))
                    FantasyButton("Training starten", alpha = 0.9f) { onGoFitness() }
                }
            }
        }
        return
    }
    val maxVol = points.maxOf { it.volume }.coerceAtLeast(1.0)
    val pointWidth = 48.dp
    val chartWidth = (points.size * pointWidth.value).dp
    val barAreaRatio = 0.85f

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Canvas(
                modifier = Modifier
                    .width(chartWidth)
                    .height(140.dp)
            ) {
                val barWidth = size.width / points.size
                points.forEachIndexed { i, p ->
                    val barHeight = (p.volume / maxVol * size.height * barAreaRatio).toFloat()
                    val x = i * barWidth + barWidth * 0.1f
                    val w = barWidth * 0.8f

                    // Bar with gradient
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(FantasyColors.Accent, FantasyColors.Accent.copy(alpha = 0.3f)),
                            startY = size.height - barHeight,
                            endY = size.height
                        ),
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(w, barHeight)
                    )
                    // Top highlight
                    drawRect(
                        color = FantasyColors.Accent.copy(alpha = 0.6f),
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(w, 2f)
                    )
                }
                // Grid lines
                for (g in 1..3) {
                    val gy = size.height * (1 - g * 0.25f * barAreaRatio)
                    drawLine(Color.White.copy(alpha = 0.06f), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
                }
            }
        }
        // Labels
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.width(chartWidth),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                points.forEach { point ->
                    FText(point.label, sizeSp = 8, color = Color.Gray, modifier = Modifier.width(pointWidth))
                }
            }
        }
    }
}

@Composable
fun NutritionCalorieChart(points: List<ChartPoint>, onGoPlanner: (() -> Unit)? = null) {
    if (points.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FText("🍎", sizeSp = 28)
                Spacer(Modifier.height(8.dp))
                FText("Noch keine Ernährungsdaten", sizeSp = 12, color = Color.Gray)
                if (onGoPlanner != null) {
                    Spacer(Modifier.height(12.dp))
                    FantasyButton("Mahlzeit planen", alpha = 0.9f) { onGoPlanner() }
                }
            }
        }
        return
    }
    val maxKcal = points.maxOf { it.kcal }.coerceAtLeast(1.0)
    val pointWidth = 48.dp
    val chartWidth = (points.size * pointWidth.value).dp
    val barAreaRatio = 0.85f

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Canvas(
                modifier = Modifier
                    .width(chartWidth)
                    .height(140.dp)
            ) {
                val stepX = size.width / (points.size - 1).coerceAtLeast(1)

                // Grid
                for (g in 1..3) {
                    val gy = size.height * (1 - g * 0.25f * barAreaRatio)
                    drawLine(Color.White.copy(alpha = 0.06f), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
                }

                // Filled area under curve
                val areaPath = Path()
                points.forEachIndexed { i, p ->
                    val x = i * stepX
                    val y = size.height - (p.kcal / maxKcal * size.height * barAreaRatio).toFloat()
                    if (i == 0) {
                        areaPath.moveTo(x, size.height)
                        areaPath.lineTo(x, y)
                    } else {
                        areaPath.lineTo(x, y)
                    }
                    if (i == points.size - 1) {
                        areaPath.lineTo(x, size.height)
                        areaPath.close()
                    }
                }
                drawPath(areaPath, brush = Brush.verticalGradient(
                    colors = listOf(FantasyColors.Gold.copy(alpha = 0.3f), FantasyColors.Gold.copy(alpha = 0.02f)),
                    startY = 0f, endY = size.height
                ))

                // Line
                val linePath = Path()
                points.forEachIndexed { i, p ->
                    val x = i * stepX
                    val y = size.height - (p.kcal / maxKcal * size.height * barAreaRatio).toFloat()
                    if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                }
                drawPath(linePath, color = FantasyColors.Gold, style = Stroke(width = 2.5f))

                // Points
                points.forEachIndexed { i, p ->
                    val x = i * stepX
                    val y = size.height - (p.kcal / maxKcal * size.height * barAreaRatio).toFloat()
                    drawCircle(color = Color(0xFF1A1A1A), radius = 5f, center = Offset(x, y))
                    drawCircle(color = FantasyColors.Gold, radius = 3.5f, center = Offset(x, y))
                }
            }
        }
        // Labels
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.width(chartWidth),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                points.forEach { point ->
                    FText(point.label, sizeSp = 8, color = Color.Gray, modifier = Modifier.width(pointWidth))
                }
            }
        }
    }
}

// ── Haupt-Screen ───────────────────────────────────────────────

@Composable
fun StatsOverviewScreen(onBack: () -> Unit, onGoHistory: (() -> Unit)? = null, onGoFitness: (() -> Unit)? = null, onGoPlanner: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    val db = remember { AppDb.get(ctx) }
    val moduleSelection = remember { SettingsManager.getModuleSelection(ctx) }

    var selectedTab by remember { mutableStateOf("Tag") }

    val statsData by remember(selectedTab) {
        val trainingAndManualFlow = combine(
            db.workoutDao().getAllWithSets(),
            db.manualMealDao().getAll(),
            db.mealDao().getAll(),
            db.workoutDao().getAllWorkouts()
        ) { sets, manual, entries, workouts -> Quadruple(sets, manual, entries, workouts) }

        val dataFlow = combine(
            db.recipeDao().getAll(),
            db.productDao().getAll(),
            db.weightDao().getAll()
        ) { recipes, products, weights -> Triple(recipes, products, weights) }

        combine(trainingAndManualFlow, dataFlow) { f1, f2 ->
            calculateStatsInternal(selectedTab, f1.first, f1.second, f1.third, f1.fourth, f2.first, f2.second, f2.third)
        }
    }.collectAsState(initial = null)

    FantasySurface {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                val screenTitle = when (moduleSelection) {
                    ModuleSelection.FITNESS_ONLY -> "Leistungsstatistik"
                    ModuleSelection.NUTRITION_ONLY -> "Ernährungsstatistik"
                    ModuleSelection.BOTH -> "Statistik & Verlauf"
                }
                MainAppBar(screenTitle, onBack = onBack)

                // Tab-Auswahl für Zeiträume
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Tag", "Woche", "Monat", "Jahr").forEach { tab ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selectedTab == tab) FantasyColors.Accent else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedTab = tab }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            FText(tab, sizeSp = 12, bold = selectedTab == tab, color = if (selectedTab == tab) Color.White else FantasyColors.Text)
                        }
                    }
                }

                val lazyListState = rememberLazyListState()

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    statsData?.let { data ->
                        val showFitness = moduleSelection != ModuleSelection.NUTRITION_ONLY
                        val showNutrition = moduleSelection != ModuleSelection.FITNESS_ONLY

                        // Berechne den Index des Entwicklungskurven-Items dynamisch
                        var curveItemIndex = 0
                        if (onGoHistory != null && showFitness) curveItemIndex++
                        if (showFitness) curveItemIndex++ // Konstanz
                        if (showFitness) curveItemIndex++ // Intensität
                        if (showFitness) curveItemIndex++ // Performance
                        if (showNutrition) curveItemIndex++ // Ernährung
                        if (data.currentWeight != null) curveItemIndex++ // Gewicht

                        // Historie Button (nur bei Fitness)
                        if (onGoHistory != null && showFitness) {
                            item {
                                FantasyButton("Trainings-Historie", Modifier.fillMaxWidth()) {
                                    onGoHistory.invoke()
                                }
                            }
                        }

                        // Trainings-Konstanz (nur bei Fitness)
                        if (showFitness) {
                            item {
                                FantasyCard {
                                    FText("TRAININGS-KONSTANZ", color = FantasyColors.Accent, bold = true, sizeSp = 12)
                                    Spacer(Modifier.height(12.dp))
                                    Row(Modifier.fillMaxWidth()) {
                                        SummaryItem(Modifier.weight(1f), "Aktivität", "${data.consistency.trainingDaysLast30} Tage", "letzte 30")
                                        SummaryItem(Modifier.weight(1f), "Serie", "${data.consistency.currentStreak}", "Tage")
                                        SummaryItem(Modifier.weight(1f), "Pause", "${data.consistency.currentPauseDays}", "Tage")
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Row(Modifier.fillMaxWidth()) {
                                        SummaryItem(Modifier.weight(1f), "Erholung", "${data.consistency.recoveryPercent.toInt()}%", "Ruheanteil")
                                        SummaryItem(Modifier.weight(1f), "Pausen", "${data.consistency.pausePhasesThisMonth}", "diesen Monat")
                                        SummaryItem(Modifier.weight(1f), "Status",
                                            if (data.consistency.isPausePhaseActive) "Pause" else "Aktiv",
                                            if (data.consistency.isPausePhaseActive) "⏸️" else "✅")
                                    }
                                }
                            }
                        }

                        // Intensitäts-Analyse (nur bei Fitness)
                        if (showFitness) {
                            item {
                                IntensityCard(data)
                            }
                        }

                        // Training & Performance (nur bei Fitness)
                        if (showFitness) {
                            item {
                                FantasyCard {
                                    FText("TRAINING & PERFORMANCE", color = FantasyColors.Accent, bold = true, sizeSp = 12)
                                    Spacer(Modifier.height(12.dp))
                                    Row(Modifier.fillMaxWidth()) {
                                        SummaryItem(Modifier.weight(1f), "Einheiten", data.logEntryCount.toString(), "Workouts")
                                        SummaryItem(Modifier.weight(1f), "Vielfalt", data.exerciseCount.toString(), "Übungen")
                                        SummaryItem(Modifier.weight(1f), "Fortschritt", data.totalScore.toInt().toString(), "Punkte")
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Row(Modifier.fillMaxWidth()) {
                                        SummaryItem(Modifier.weight(1f), "Sätze", if (data.totalSets > 0) data.totalSets.toString() else "—", "Gesamt")
                                        SummaryItem(Modifier.weight(1f), "Reps", if (data.totalReps > 0) data.totalReps.toString() else "—", "Gesamt")
                                        SummaryItem(Modifier.weight(1f), "Volumen", "${(data.totalVolume / 1000).toInt()}t", "Tonnage")
                                    }
                                }
                            }
                        }

                        // Ernährung & Makros (nur bei Ernährung)
                        if (showNutrition) {
                            item {
                                FantasyCard {
                                    FText("ERNÄHRUNG & MAKROS", color = FantasyColors.Gold, bold = true, sizeSp = 12)
                                    Spacer(Modifier.height(12.dp))
                                    Row(Modifier.fillMaxWidth()) {
                                        SummaryItem(Modifier.weight(1f), "Kalorien Ø", data.avgKcal.toString(), "kcal/Tag")
                                        SummaryItem(Modifier.weight(1f), "Mahlzeiten", data.mealCount.toString(), "Einträge")
                                        SummaryItem(Modifier.weight(1f), "Protein Ø", "${data.avgProtein.roundToInt()}g", "pro Tag")
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Row(Modifier.fillMaxWidth()) {
                                        SummaryItem(Modifier.weight(1f), "Carbs Ø", "${data.avgCarbs.roundToInt()}g", "pro Tag")
                                        SummaryItem(Modifier.weight(1f), "Fett Ø", "${data.avgFat.roundToInt()}g", "pro Tag")
                                        SummaryItem(Modifier.weight(1f), "Protein ges.", "${data.totalProtein.toInt()}g", "Zeitraum")
                                    }
                                }
                            }
                        }

                        // Körpergewicht (immer sichtbar – relevant für beide Module)
                        if (data.currentWeight != null) {
                            item {
                                FantasyCard {
                                    FText("KÖRPERGEWICHT", color = Color.Cyan, bold = true, sizeSp = 12)
                                    Spacer(Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        FText("${"%.1f".format(data.currentWeight)} kg", sizeSp = 24, bold = true, color = FantasyColors.Accent)
                                        Spacer(Modifier.width(12.dp))
                                        val sign = if (data.weightChange > 0) "+" else ""
                                        val changeFormatted = "%.1f".format(data.weightChange)
                                        FText("$sign$changeFormatted kg", sizeSp = 16, bold = true, color = if (data.weightChange <= 0) Color.Green else Color.Red)
                                    }
                                }
                            }
                        }

                        // Entwicklungskurven (Tabs werden innerhalb nach Modul gefiltert)
                        item {
                            DevelopmentCurveCard(data, moduleSelection, lazyListState, curveItemIndex, onGoFitness, onGoPlanner)
                        }
                    } ?: item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = FantasyColors.Accent)
                        }
                    }
                }
            }
        }
    }
}
