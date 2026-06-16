package com.fantasyfoodplanner.features

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fantasyfoodplanner.R
import com.fantasyfoodplanner.data.*
import com.fantasyfoodplanner.logic.ModuleSelection
import com.fantasyfoodplanner.logic.SettingsManager
import com.fantasyfoodplanner.logic.SubscriptionManager
import com.fantasyfoodplanner.ui.*
import com.fantasyfoodplanner.viewmodel.DashboardViewModel
import com.fantasyfoodplanner.viewmodel.DashboardState
import kotlin.random.Random

// ── Coach-Tipps nach Modulen sortiert ──────────────────────────────────────

private val FITNESS_TIPS = listOf(
    "Technik vor Gewicht – saubere Form ist alles.",
    "Rumpf fest, Rücken neutral halten.",
    "Langsam ablassen, kontrolliert nach oben.",
    "Atme beim Hochdrücken aus.",
    "Kurzes Warmup reduziert das Verletzungsrisiko.",
    "Saubere Wiederholungen zählen mehr als viele.",
    "Pause 60–90 Sekunden zwischen den Sätzen.",
    "Fokus auf den Muskel, den du trainierst.",
    "Beständigkeit ist der Schlüssel zum Erfolg.",
    "Schlaf ist genauso wichtig wie das Training.",
    "Höre auf deinen Körper – Pausen sind okay.",
    "Die letzte Wiederholung sollte schwer sein.",
    "Halte die Spannung in der gesamten Bewegung.",
    "Dokumentiere dein Training für sichtbaren Fortschritt."
)

private val NUTRITION_TIPS = listOf(
    "Trinke mindestens 2 Liter Wasser am Tag.",
    "Protein unterstützt deinen Muskelaufbau.",
    "Plane deine Mahlzeiten am Abend vorher.",
    "Iss regelmäßig – dein Körper braucht Energie.",
    "Gemüse liefert wichtige Mikronährstoffe.",
    "Vermeide leere Kalorien durch Softdrinks.",
    "Frühstücke ausgewogen für Energie am Morgen.",
    "Komplexe Kohlenhydrate halten dich länger satt.",
    "Achte auf ausreichend Ballaststoffe.",
    "Meal-Prep spart Zeit und hält dich auf Kurs."
)

private val COMBINED_TIPS = FITNESS_TIPS + NUTRITION_TIPS

@Composable
fun DashboardScreen(
    onGoPlanner: () -> Unit,
    onGoStatsOverview: () -> Unit,
    onGoProfile: () -> Unit,
    onGoFitness: () -> Unit,

    onGoUpgrade: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val app = (ctx.applicationContext as android.app.Application)
    val db = remember { AppDb.get(ctx) }
    val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory(app, db))

    val dashState by vm.state.collectAsState()
    val userProfile = dashState.userProfile
    val todayKcal = dashState.todayKcal

    val moduleSelection = remember { SettingsManager.getModuleSelection(ctx) }
    val fitnessEnabled = moduleSelection != ModuleSelection.NUTRITION_ONLY
    val nutritionEnabled = moduleSelection != ModuleSelection.FITNESS_ONLY
    val hasNutrition = remember { SubscriptionManager.hasNutritionModule(ctx) }
    // Dashboard-Daten bei jedem Wiedereintritt (ON_RESUME) aktualisieren
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val coachTip = remember {
        val tips = when (moduleSelection) {
            ModuleSelection.FITNESS_ONLY -> FITNESS_TIPS
            ModuleSelection.NUTRITION_ONLY -> NUTRITION_TIPS
            ModuleSelection.BOTH -> COMBINED_TIPS
        }
        tips[Random.nextInt(tips.size)]
    }

    var showStartTip by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(4000)
        showStartTip = false
    }

    val glowGreen = Color(0xFF00FF7F)

    Box(Modifier.fillMaxSize()) {
        FantasySurface {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // ═══════════════════════════════════════════════════
                // HEADER
                // ═══════════════════════════════════════════════════
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            FText("Willkommen,", sizeSp = 14, color = FantasyColors.GrayText)
                            FText(
                                userProfile?.name ?: "Benutzer",
                                sizeSp = 24, bold = true, color = FantasyColors.Accent
                            )
                        }
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(FantasyColors.Accent.copy(alpha = 0.15f))
                                .clickable { showStartTip = !showStartTip }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AIHeadIcon(Modifier.size(36.dp))
                        }
                    }
                }

                // Coach-Tipp
                if (showStartTip) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(FantasyColors.Accent.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AIHeadIcon(Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            FText(coachTip, sizeSp = 13, color = FantasyColors.Text, italic = true)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ═══════════════════════════════════════════════════
                // GOAL CARD – Dein Ziel
                // ═══════════════════════════════════════════════════
                userProfile?.let { profile ->
                    GoalProgressCard(
                        profile = profile,
                        dashState = dashState,
                        fitnessEnabled = fitnessEnabled,
                        nutritionEnabled = nutritionEnabled
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // ═══════════════════════════════════════════════════
                // WOCHEN-STREAK (nur bei Fitness)
                // ═══════════════════════════════════════════════════
                if (fitnessEnabled) {
                    WeekStreakCard(
                        streak = dashState.currentStreak,
                        weekDaysDone = dashState.weekDaysDone,
                        glowGreen = glowGreen
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // ═══════════════════════════════════════════════════
                // STATUS HEUTE – 2 Cards nebeneinander
                // ═══════════════════════════════════════════════════
                if (fitnessEnabled && nutritionEnabled) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MiniCalorieCard(todayKcal, userProfile?.dailyKcalTarget ?: 2000, Modifier.weight(1f))
                        MiniTrainingCard(dashState.lastWorkoutDaysAgo, dashState.trainingDaysThisWeek, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                } else if (nutritionEnabled) {
                    CalorieStatusCard(currentKcal = todayKcal, targetKcal = userProfile?.dailyKcalTarget ?: 2000)
                    Spacer(Modifier.height(16.dp))
                } else if (fitnessEnabled) {
                    MiniTrainingCard(dashState.lastWorkoutDaysAgo, dashState.trainingDaysThisWeek, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                }

                // ═══════════════════════════════════════════════════
                // AKTIONEN
                // ═══════════════════════════════════════════════════
                FText("AKTIONEN", sizeSp = 13, bold = true, color = FantasyColors.GrayText, modifier = Modifier.padding(bottom = 12.dp))

                // Hero Button – primäre Aktion
                if (fitnessEnabled) {
                    DashboardActionCard(
                        title = "Start Training",
                        iconResId = R.drawable.ic_trainig_orig,
                        isPrimary = true,
                        subtitle = "Dein Workout starten",
                        iconSize = 120.dp,
                        onClick = onGoFitness
                    )
                    Spacer(Modifier.height(12.dp))
                } else if (nutritionEnabled) {
                    DashboardActionCard(
                        title = "PULS",
                        iconResId = R.drawable.ic_puls_orig,
                        isPrimary = true,
                        subtitle = "Mahlzeiten planen",
                        onClick = { if (hasNutrition) onGoPlanner() else onGoUpgrade() }
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Grid – 2-Spalten
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (fitnessEnabled && nutritionEnabled) {
                        // Beide aktiv → Planer + Statistik im Grid
                        DashboardGridCard(
                            emoji = "",
                            title = "PULS",
                            modifier = Modifier.weight(1f),
                            onClick = { if (hasNutrition) onGoPlanner() else onGoUpgrade() },
                            iconResId = R.drawable.ic_puls_orig
                        )
                    }
                    DashboardGridCard(
                        emoji = "",
                        title = "Statistik",
                        modifier = Modifier.weight(1f),
                        onClick = onGoStatsOverview,
                        iconResId = R.drawable.ic_statistik_orig
                    )
                    if (!fitnessEnabled || !nutritionEnabled) {
                        // Nur ein Modul → Statistik + Profil nebeneinander
                        DashboardGridCard(
                            emoji = "⚙",
                            title = "Profil",
                            modifier = Modifier.weight(1f),
                            onClick = onGoProfile
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Profil-Card (nur wenn beide Module aktiv → Profil hat eigene Zeile)
                if (fitnessEnabled && nutritionEnabled) {
                    DashboardActionCard(
                        title = "Profil & Einstellungen",
                        iconResId = R.drawable.ic_profil_vec,
                        subtitle = "Deine Daten verwalten",
                        onClick = onGoProfile
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }

    }
}

// ════════════════════════════════════════════════════════════════
// GOAL PROGRESS CARD
// ════════════════════════════════════════════════════════════════

@Composable
fun GoalProgressCard(
    profile: UserProfile,
    dashState: DashboardState,
    fitnessEnabled: Boolean,
    nutritionEnabled: Boolean
) {
    val goalLabel = when (profile.goal) {
        "build" -> "Muskelaufbau"
        "lose" -> "Abnehmen"
        else -> "Fit bleiben"
    }
    val goalEmoji = when (profile.goal) {
        "build" -> "💪"
        "lose" -> "🔥"
        else -> "⚡"
    }
    val glowGreen = Color(0xFF00FF7F)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, glowGreen.copy(alpha = 0.25f))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0D1A0F),
                            Color(0xFF0A0F0A)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FText("$goalEmoji  DEIN ZIEL: $goalLabel", sizeSp = 15, bold = true, color = glowGreen)
                }

                Spacer(Modifier.height(16.dp))

                // Status-Zeilen
                if (fitnessEnabled) {
                    GoalStatusRow(
                        label = "Training diese Woche",
                        value = "${dashState.trainingDaysThisWeek}x",
                        isGood = dashState.trainingDaysThisWeek >= 3
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (nutritionEnabled) {
                    val target = profile.dailyKcalTarget
                    val pct = if (target > 0) (dashState.avgKcalWeek.toFloat() / target * 100).toInt() else 0
                    GoalStatusRow(
                        label = "Ernährung Ø",
                        value = "${dashState.avgKcalWeek} kcal ($pct%)",
                        isGood = pct in 70..120
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (dashState.currentWeight != null) {
                    val sign = if (dashState.weightChange > 0) "+" else ""
                    val changeStr = "$sign${"%.1f".format(dashState.weightChange)} kg"
                    val isGoodWeight = when (profile.goal) {
                        "lose" -> dashState.weightChange <= 0
                        "build" -> dashState.weightChange >= 0
                        else -> true
                    }
                    GoalStatusRow(
                        label = "Gewicht",
                        value = "${"%.1f".format(dashState.currentWeight)} kg ($changeStr)",
                        isGood = isGoodWeight
                    )
                }

                if (fitnessEnabled && dashState.currentStreak > 0) {
                    Spacer(Modifier.height(8.dp))
                    GoalStatusRow(
                        label = "Aktuelle Serie",
                        value = "${dashState.currentStreak} Tage 🔥",
                        isGood = true
                    )
                }
            }
        }
    }
}

@Composable
fun GoalStatusRow(label: String, value: String, isGood: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        FText(
            if (isGood) "✓" else "○",
            sizeSp = 14, bold = true,
            color = if (isGood) Color(0xFF00FF7F) else Color(0xFFFF6B6B)
        )
        Spacer(Modifier.width(10.dp))
        FText(label, sizeSp = 13, color = FantasyColors.GrayText, modifier = Modifier.weight(1f))
        FText(value, sizeSp = 13, bold = true, color = if (isGood) Color.White else Color(0xFFFFD93D))
    }
}

// ════════════════════════════════════════════════════════════════
// WEEK STREAK CARD
// ════════════════════════════════════════════════════════════════

@Composable
fun WeekStreakCard(streak: Int, weekDaysDone: List<Int>, glowGreen: Color) {
    val days = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    val colorFull = FantasyColors.Accent          // Blau/Accent — volles Workout
    val colorSupplementary = Color(0xFFFFB347)    // Orange — ergänzende Aktivität
    val colorLight = Color(0xFF888888)            // Grau — leichte Bewegung

    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = FantasyColors.CardBg),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, glowGreen.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FText("🔥", sizeSp = 18)
                Spacer(Modifier.width(8.dp))
                FText(
                    if (streak > 0) "$streak-Tage-Serie" else "Starte deine Serie!",
                    sizeSp = 14, bold = true, color = if (streak > 0) glowGreen else FantasyColors.GrayText
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                days.forEachIndexed { idx, day ->
                    val status = weekDaysDone.getOrElse(idx) { 0 }
                    val done = status > 0
                    // Farbe je nach Aktivitäts-Kategorie
                    val dotColor = when (status) {
                        1 -> colorFull            // Volles Workout
                        2 -> colorSupplementary   // Ergänzende Aktivität
                        3 -> colorLight           // Leichte Bewegung
                        else -> Color.White       // Fallback (nicht sichtbar)
                    }
                    // Symbol je nach Kategorie
                    val symbol = when (status) {
                        1 -> "✓"
                        2 -> "~"
                        3 -> "·"
                        else -> "·"
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .background(
                                    if (done) dotColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    if (done) dotColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            FText(
                                symbol,
                                sizeSp = if (status == 1) 14 else 18,
                                bold = status == 1,
                                color = if (done) dotColor else FantasyColors.GrayText
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        FText(day, sizeSp = 10, color = FantasyColors.GrayText)
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// MINI STATUS CARDS
// ════════════════════════════════════════════════════════════════

@Composable
fun MiniCalorieCard(currentKcal: Int, targetKcal: Int, modifier: Modifier = Modifier) {
    val progress = if (targetKcal > 0) (currentKcal.toFloat() / targetKcal).coerceIn(0f, 1f) else 0f
    val progressColor = when {
        progress > 0.9f -> Color(0xFFFF6B6B)
        progress > 0.7f -> Color(0xFFFFD93D)
        else -> Color(0xFF00FF7F)
    }

    Card(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = FantasyColors.CardBg),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, progressColor.copy(alpha = 0.25f))
    ) {
        Column(Modifier.padding(14.dp)) {
            FText("🔥 Kalorien", sizeSp = 11, color = FantasyColors.GrayText)
            Spacer(Modifier.height(6.dp))
            FText("$currentKcal", sizeSp = 24, bold = true, color = FantasyColors.Accent)
            FText("/ $targetKcal kcal", sizeSp = 11, color = FantasyColors.GrayText)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun MiniTrainingCard(lastWorkoutDaysAgo: Int, trainingDaysThisWeek: Int, modifier: Modifier = Modifier) {
    val statusText = when {
        lastWorkoutDaysAgo == 0 -> "Heute trainiert ✓"
        lastWorkoutDaysAgo == 1 -> "Gestern trainiert"
        lastWorkoutDaysAgo in 2..3 -> "Vor $lastWorkoutDaysAgo Tagen"
        lastWorkoutDaysAgo > 3 -> "Pause: $lastWorkoutDaysAgo Tage"
        else -> "Noch kein Training"
    }
    val statusColor = when {
        lastWorkoutDaysAgo in 0..1 -> Color(0xFF00FF7F)
        lastWorkoutDaysAgo in 2..3 -> Color(0xFFFFD93D)
        else -> Color(0xFFFF6B6B)
    }

    Card(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = FantasyColors.CardBg),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
    ) {
        Column(Modifier.padding(14.dp)) {
            FText("🏋 Training", sizeSp = 11, color = FantasyColors.GrayText)
            Spacer(Modifier.height(6.dp))
            FText("${trainingDaysThisWeek}x", sizeSp = 24, bold = true, color = statusColor)
            FText("diese Woche", sizeSp = 11, color = FantasyColors.GrayText)
            Spacer(Modifier.height(8.dp))
            FText(statusText, sizeSp = 11, bold = true, color = statusColor)
        }
    }
}

// ════════════════════════════════════════════════════════════════
// DASHBOARD GRID CARD (quadratisch, Emoji-basiert)
// ════════════════════════════════════════════════════════════════

@Composable
fun DashboardGridCard(
    emoji: String,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    iconResId: Int? = null
) {
    val glowGreen = Color(0xFF00FF7F)

    Card(
        modifier = modifier
            .aspectRatio(1.1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = FantasyColors.CardBg),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, glowGreen.copy(alpha = 0.15f))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(FantasyColors.CardBg, Color(0xFF0A0F0A))
                    )
                )
                .padding(14.dp)
        ) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (iconResId != null) {
                    Image(
                        painter = painterResource(iconResId),
                        contentDescription = title,
                        modifier = Modifier.size(88.dp).padding(top = 8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    FText(emoji, sizeSp = 56)
                }
                Spacer(Modifier.height(8.dp))
                FText(title, sizeSp = 13, bold = true, color = FantasyColors.Text)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// COACH BOTTOM SHEET
// ════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachBottomSheet(
    moduleSelection: ModuleSelection,
    userProfile: UserProfile?,
    todayKcal: Int,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Coach-Tipps werden nur beim Öffnen erzeugt (Performance!)
    val tips = remember {
        val source = when (moduleSelection) {
            ModuleSelection.FITNESS_ONLY -> FITNESS_TIPS
            ModuleSelection.NUTRITION_ONLY -> NUTRITION_TIPS
            ModuleSelection.BOTH -> COMBINED_TIPS
        }
        source.shuffled().take(3)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF151515),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                AIHeadIcon(Modifier.size(40.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    FText("KI-COACH", sizeSp = 18, bold = true, color = FantasyColors.Accent)
                    FText("Dein persönlicher Berater", sizeSp = 13, color = FantasyColors.GrayText)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Dynamische Analyse basierend auf aktiven Modulen
            if (moduleSelection != ModuleSelection.FITNESS_ONLY && userProfile != null) {
                val target = userProfile.dailyKcalTarget
                val advice = when {
                    todayKcal == 0 -> "Du hast heute noch nichts gegessen. Starte jetzt mit deiner Ernährungsplanung!"
                    todayKcal < target * 0.5 -> "Gute Energiebilanz bisher. Achte auf ausreichend Protein."
                    todayKcal > target -> "Kalorienziel erreicht! Konzentriere dich auf Regeneration."
                    else -> "Optimale Balance heute – weiter so! \uD83D\uDCAA"
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = FantasyColors.Accent.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        FText("\uD83D\uDCCA Tages-Analyse", sizeSp = 14, bold = true, color = FantasyColors.Accent)
                        Spacer(Modifier.height(8.dp))
                        FText(advice, sizeSp = 14, color = FantasyColors.Text)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Tipps
            FText("\uD83D\uDCA1 Tipps für dich", sizeSp = 15, bold = true, color = FantasyColors.Text)
            Spacer(Modifier.height(8.dp))

            tips.forEach { tip ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    FText("\u2022", sizeSp = 16, color = FantasyColors.Accent, bold = true)
                    Spacer(Modifier.width(8.dp))
                    FText(tip, sizeSp = 14, color = FantasyColors.Text)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Schließen Button
            FantasyButton(
                label = "Verstanden",
                modifier = Modifier.fillMaxWidth()
            ) {
                onDismiss()
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun AiStatusDialog(advice: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151515),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AIHeadIcon(Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                FText("KI-COACH ANALYSE", color = FantasyColors.Accent, bold = true)
            }
        },
        text = { FText(advice, color = Color.White) },
        confirmButton = { FantasyButton("OK") { onDismiss() } }
    )
}
