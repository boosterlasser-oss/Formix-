package com.fantasyfoodplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.fantasyfoodplanner.data.*
import com.fantasyfoodplanner.features.*
import com.fantasyfoodplanner.features.fitness.*
import com.fantasyfoodplanner.logic.BillingManager
import com.fantasyfoodplanner.logic.ModuleSelection
import com.fantasyfoodplanner.logic.SettingsManager
import com.fantasyfoodplanner.logic.SubscriptionManager
import com.fantasyfoodplanner.logic.TrainingType
import com.fantasyfoodplanner.ui.*

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.net.URLEncoder
import com.fantasyfoodplanner.utils.NutrientCalculator
import java.time.LocalDate

private object AppSessionState {
    var splashCompleted: Boolean = false
}

class MainActivity : ComponentActivity() {
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SubscriptionManager.init(this)
        billingManager = BillingManager(this)
        billingManager.connect()
        setContent { App(billingManager = billingManager) }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.disconnect()
    }
}

@Composable
fun App(billingManager: BillingManager? = null) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val nav = rememberNavController()
    val db = remember { AppDb.get(ctx) }

    var showSplashScreen by rememberSaveable { mutableStateOf(!AppSessionState.splashCompleted) }
    var initializationFinished by rememberSaveable { mutableStateOf(false) }
    var startDestination by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val destination = withContext(Dispatchers.IO) {
            Seeder.seedAll(db)
            if (BuildConfig.DEBUG) {
                val existing = db.userDao().profile().first()
                if (existing == null || !SettingsManager.isModuleSetupDone(ctx)) {
                    db.userDao().save(UserProfile(
                        name = "Debug", age = 30, weightKg = 80.0, heightCm = 180,
                        sex = "male", goal = "build", experience = "some", activityLevel = "moderate",
                        dailyKcalTarget = 2500, dailyProteinTarget = 180,
                        trainLocation = "gym", availableEquipment = "full",
                        timePerSession = 60, sessionsPerWeek = 4
                    ))
                    SettingsManager.setModuleSelection(ctx, ModuleSelection.BOTH)
                }
                "dashboard"
            } else {
                val profile = db.userDao().profile().first()
                if (profile != null && SettingsManager.isModuleSetupDone(ctx)) "dashboard" else "onboarding"
            }
        }
        startDestination = destination
        initializationFinished = true
    }

    // Display Splash Screen → Coach Fly-In → Main App Content
    Box(modifier = Modifier.fillMaxSize()) {
        // Phase 1: SplashScreen
        AnimatedVisibility(
            visible = showSplashScreen,
            exit = fadeOut(animationSpec = tween(durationMillis = 400))
        ) {
            SplashScreenContent(
                initializationFinished = initializationFinished,
                    onFinished = {
                        AppSessionState.splashCompleted = true
                        showSplashScreen = false
                    }
                )
        }

        // Hauptinhalt
        AnimatedVisibility(
            visible = !showSplashScreen,
            enter = fadeIn(animationSpec = tween(durationMillis = 400))
        ) {
            if (startDestination == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FantasyColors.Accent)
                }
            } else {
                NavHost(
                    navController = nav,
                    startDestination = startDestination!!,
                    enterTransition = { slideIntoContainer(androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) },
                    exitTransition = { slideOutOfContainer(androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) },
                    popEnterTransition = { slideIntoContainer(androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.End, tween(300)) },
                    popExitTransition = { slideOutOfContainer(androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.End, tween(300)) }
                ) {
                    composable("onboarding") {
                        CoachOnboarding(onFinish = {
                            nav.navigate("dashboard") { popUpTo("onboarding") { inclusive = true } }
                        })
                    }
                    composable("dashboard") {
                        DashboardScreen(
                            onGoPlanner = { nav.navigate("planner") },
                            onGoStatsOverview = { nav.navigate("stats_overview") },
                            onGoProfile = { nav.navigate("profile") },
                            onGoFitness = { nav.navigate("training_flow") },

                            onGoUpgrade = { nav.navigate("upgrade/PREMIUM") }
                        )
                    }
                    composable("training_flow") {
                        TrainingFlowScreen(
                            onBack = { nav.popBackStack() },
                            onOpenExercise = { name ->
                                val encodedName = URLEncoder.encode(name, "UTF-8")
                                nav.navigate("exercise_detail/$encodedName")
                            },
                            onGoStats = { nav.navigate("stats_overview") },
                            onGoUpgrade = { nav.navigate("upgrade/PREMIUM") }
                        )
                    }
                    composable("stats_overview") {
                        StatsOverviewScreen(
                            onBack = { nav.popBackStack() },
                            onGoHistory = { nav.navigate("workout_stats") },
                            onGoFitness = { nav.navigate("training_flow") },
                            onGoPlanner = { nav.navigate("planner") }
                        )
                    }
                    composable("workout_stats") {
                        WorkoutStatsScreen(onBack = { nav.popBackStack() })
                    }
                    composable("exercise_detail/{name}") { backStackEntry ->
                        val encodedName = backStackEntry.arguments?.getString("name") ?: ""
                        val name = URLDecoder.decode(encodedName, "UTF-8")
                        ExerciseDetailScreen(exerciseName = name, onBack = { nav.popBackStack() })
                    }
                    composable("planner") {
                        PlannerScreen(onBack = { nav.popBackStack() }, onOpenDay = { d -> nav.navigate("daymenu/${d}") })
                    }
                    composable("daymenu/{date}") {
                        val dateStr = it.arguments?.getString("date") ?: LocalDate.now().toString()
                        DayMenuScreen(
                            date = LocalDate.parse(dateStr),
                            onBack = { nav.popBackStack() },
                            onGoSearch = { epoch -> nav.navigate("food_database/$epoch") }
                        )
                    }
                    composable("profile") {
                        FullProfileScreen(
                            onBack = { nav.popBackStack() },
                            onGoSystemLog = { nav.navigate("system_log") },
                            onGoUpgrade = { nav.navigate("upgrade/PREMIUM") },
                            onGoLegal = { nav.navigate("legal") }
                        )
                    }
                    composable("system_log") { SystemLogScreen(onBack = { nav.popBackStack() }) }
                    composable("upgrade/{tier}") { backStackEntry ->
                        val tierName = backStackEntry.arguments?.getString("tier") ?: "PREMIUM"
                        val tier = try {
                            com.fantasyfoodplanner.logic.SubscriptionTier.valueOf(tierName)
                        } catch (e: Exception) {
                            com.fantasyfoodplanner.logic.SubscriptionTier.PREMIUM
                        }
                        UpgradeScreen(
                            onBack = { nav.popBackStack() },
                            highlightTier = tier,
                            billingManager = billingManager
                        )
                    }
                    composable("legal") {
                        LegalScreen(onBack = { nav.popBackStack() })
                    }
                    composable("food_database/{date}") { backStackEntry ->
                        val dateEpochDay = backStackEntry.arguments?.getString("date")?.toLong() ?: LocalDate.now().toEpochDay()
                        FoodDatabaseScreen(
                            dateEpochDay = dateEpochDay,
                            onBack = { nav.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CoachOnboarding(onFinish: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDb.get(ctx) }
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(1) }
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("male") }
    var goal by remember { mutableStateOf("build") }
    var activity by remember { mutableStateOf("moderate") }
    var experience by remember { mutableStateOf("new") }
    var moduleChoice by remember { mutableStateOf(ModuleSelection.BOTH) }
    
    // v9: Neue Onboarding-States (Steps 8-17)
    var targetWeight by remember { mutableStateOf("") }
    var focusAreas by remember { mutableStateOf(setOf<String>()) }
    var bodyFormNow by remember { mutableStateOf("normal") }
    var bodyFormGoal by remember { mutableStateOf("athletic") }
    var trainLocation by remember { mutableStateOf("gym") }
    var equipment by remember { mutableStateOf("full") }
    var timePerSession by remember { mutableStateOf(45) }
    var sessionsPerWeek by remember { mutableStateOf(3) }
    var healthRestrictions by remember { mutableStateOf(setOf<String>()) }
    var motivationText by remember { mutableStateOf("") }
    
    FantasySurface {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AIHeadIcon(Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            FText("AI COACH", color = FantasyColors.Accent, bold = true, sizeSp = 12)
            Spacer(Modifier.height(24.dp))

            when(step) {
                1 -> {
                        FText("Wie lautet dein Name?", sizeSp = 20, bold = true)
                        Spacer(Modifier.height(16.dp))
                    FantasyTextField(name, { name = it }, "Name eingeben...")
                }
                2 -> {
                        FText("Deine körperlichen Daten:", sizeSp = 20, bold = true)
                        Spacer(Modifier.height(16.dp))
                    FantasyTextField(age, { age = it }, "Alter", keyboardType = KeyboardType.Number)
                    FantasyTextField(height, { height = it }, "Größe (cm)", keyboardType = KeyboardType.Number)
                    FantasyTextField(weight, { weight = it }, "Gewicht (kg)", keyboardType = KeyboardType.Number)
                }
                3 -> {
                        FText("Wähle dein biologisches Geschlecht:", sizeSp = 20, bold = true)
                        FText("Das Modell wird entsprechend angepasst.", sizeSp = 12, color = FantasyColors.GrayText)
                        Spacer(Modifier.height(16.dp))
                    listOf("male" to "Männlich", "female" to "Weiblich").forEach { (id, label) ->
                        FantasyButton(label = label, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), alpha = if(sex == id) 1f else 0.5f) { sex = id }
                    }
                }
                4 -> {
                        FText("Was ist dein Ziel?", sizeSp = 20, bold = true)
                        Spacer(Modifier.height(16.dp))
                    listOf("build" to "Muskelaufbau", "lose" to "Gewichtsreduktion", "fit" to "Fitness & Ausdauer").forEach { (id, label) ->
                        FantasyButton(label = label, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), alpha = if(goal == id) 1f else 0.5f) { goal = id }
                    }
                }
                 5 -> {
                        FText("Wie viel Erfahrung hast du?", sizeSp = 20, bold = true)
                        Spacer(Modifier.height(16.dp))
                    listOf("new" to "Anfänger", "some" to "Fortgeschritten", "pro" to "Profi").forEach { (id, label) ->
                        FantasyButton(label = label, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), alpha = if(experience == id) 1f else 0.5f) { experience = id }
                    }
                }
                6 -> {
                        FText("Wie aktiv bist du im Alltag?", sizeSp = 20, bold = true)
                        Spacer(Modifier.height(16.dp))
                    listOf("sedentary" to "Sitzend", "moderate" to "Mäßig aktiv", "active" to "Sehr aktiv").forEach { (id, label) ->
                        FantasyButton(label = label, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), alpha = if(activity == id) 1f else 0.5f) { activity = id }
                    }
                }
                7 -> {
                        FText("Welche Module möchtest du nutzen?", sizeSp = 20, bold = true)
                        Spacer(Modifier.height(16.dp))
                    listOf(ModuleSelection.FITNESS_ONLY to "Nur Fitness", ModuleSelection.NUTRITION_ONLY to "Nur Ernährung", ModuleSelection.BOTH to "Beides").forEach { (sel, label) ->
                        FantasyButton(label = label, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), alpha = if(moduleChoice == sel) 1f else 0.5f) { moduleChoice = sel }
                    }
                }
                8 -> {
                        FText("Was ist dein Zielgewicht?", sizeSp = 20, bold = true)
                        Spacer(Modifier.height(16.dp))
                    OutlinedTextField(value = targetWeight, onValueChange = { targetWeight = it }, label = { Text("Zielgewicht (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
                9 -> {
                    val focusOptions = listOf("bauch" to "Bauch", "beine" to "Beine", "po" to "Po", "arme" to "Arme", "ruecken" to "Rücken", "brust" to "Brust")
                        FText("Welche Körperpartien möchtest du trainieren?", sizeSp = 20, bold = true)
                        Spacer(Modifier.height(8.dp))
                        FText("(Mehrfachauswahl möglich)", sizeSp = 14)
                        Spacer(Modifier.height(16.dp))
                    focusOptions.forEach { (id, label) ->
                        val selected = focusAreas.contains(id)
                        FantasyButton(label = label, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), alpha = if(selected) 1f else 0.5f) {
                            focusAreas = if(selected) focusAreas - id else focusAreas + id
                        }
                    }
                }
                10 -> {
                        FText("Wie würdest du deine aktuelle Körperform beschreiben?", sizeSp = 20, bold = true)
                        Spacer(Modifier.height(16.dp))
                    listOf("schlank" to "Schlank", "normal" to "Normal", "kraftig" to "Kräftig", "ubergewichtig" to "Übergewichtig").forEach { (id, label) ->
                        FantasyButton(label = label, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), alpha = if(bodyFormNow == id) 1f else 0.5f) { bodyFormNow = id }
                    }
                }
                11 -> {
                        FText("Welche Körperform ist dein Ziel?", sizeSp = 20, bold = true)
                        Spacer(Modifier.height(16.dp))
                    listOf("schlank" to "Schlank", "athletisch" to "Athletisch", "muskulos" to "Muskulös", "definiert" to "Definiert").forEach { (id, label) ->
                        FantasyButton(label = label, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), alpha = if(bodyFormGoal == id) 1f else 0.5f) { bodyFormGoal = id }
                    }
                }
                12 -> {
                        FText("Wo trainierst du hauptsächlich?", sizeSp = 20, bold = true)
                        Spacer(Modifier.height(16.dp))
                    listOf("gym" to "Fitnessstudio", "home" to "Zuhause", "outdoor" to "Draußen").forEach { (id, label) ->
                        FantasyButton(label = label, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), alpha = if(trainLocation == id) 1f else 0.5f) { trainLocation = id }
                    }
                }
                13 -> {
                        FText("Welche Geräte stehen dir zur Verfügung?", sizeSp = 20, bold = true)
                        Spacer(Modifier.height(16.dp))
                    listOf("full" to "Komplettes Gym", "minimal" to "Hanteln & Bänder", "bodyweight" to "Nur Körpergewicht").forEach { (id, label) ->
                        FantasyButton(label = label, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), alpha = if(equipment == id) 1f else 0.5f) { equipment = id }
                    }
                }
                14 -> {
                        FText("Wie viel Zeit hast du pro Training?", sizeSp = 20, bold = true)
                        Spacer(Modifier.height(16.dp))
                    listOf(30 to "30 Minuten", 45 to "45 Minuten", 60 to "60 Minuten", 90 to "90+ Minuten").forEach { (id, label) ->
                        FantasyButton(label = label, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), alpha = if(timePerSession == id) 1f else 0.5f) { timePerSession = id }
                    }
                }
                15 -> {
                        FText("Wie oft trainierst du pro Woche?", sizeSp = 20, bold = true)
                        Spacer(Modifier.height(16.dp))
                    listOf(1 to "1x", 2 to "2x", 3 to "3x", 4 to "4x", 5 to "5x+").forEach { (id, label) ->
                        FantasyButton(label = label, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), alpha = if(sessionsPerWeek == id) 1f else 0.5f) { sessionsPerWeek = id }
                    }
                }
                16 -> {
                    val restrictions = listOf("knie" to "Knie", "ruecken" to "Rücken", "schulter" to "Schulter", "handgelenk" to "Handgelenk")
                        FText("Hast du gesundheitliche Einschränkungen?", sizeSp = 20, bold = true)
                        Spacer(Modifier.height(8.dp))
                        FText("(Mehrfachauswahl möglich)", sizeSp = 14)
                        Spacer(Modifier.height(16.dp))
                    restrictions.forEach { (id, label) ->
                        val selected = healthRestrictions.contains(id)
                        FantasyButton(label = label, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), alpha = if(selected) 1f else 0.5f) {
                            healthRestrictions = if(selected) healthRestrictions - id else healthRestrictions + id
                        }
                    }
                }
                17 -> {
                        FText("Was motiviert dich zum Training?", sizeSp = 20, bold = true)
                        Spacer(Modifier.height(16.dp))
                    OutlinedTextField(value = motivationText, onValueChange = { motivationText = it }, label = { Text("Deine Motivation") }, modifier = Modifier.fillMaxWidth().height(120.dp), maxLines = 4)
                }
            }

            Spacer(Modifier.weight(1f))
            val canProceed = when(step) {
                1 -> name.isNotBlank()
                2 -> age.isNotBlank() && weight.isNotBlank() && height.isNotBlank()
                else -> true
            }
            FantasyButton(label = if(step < 17) "Weiter" else "Plan erstellen", modifier = Modifier.fillMaxWidth(), enabled = canProceed) {
                if(step < 17) step++
                else {
                    scope.launch {
                        val w = weight.toDoubleOrNull() ?: 75.0
                        val h = height.toDoubleOrNull() ?: 180.0
                        val a = age.toIntOrNull() ?: 25

                        // Temporäres Profil für die zentrale TDEE-Berechnung
                        val tempProfile = UserProfile(
                            name = name,
                            age = a,
                            weightKg = w,
                            heightCm = h.toInt(),
                            sex = sex,
                            goal = goal,
                            experience = experience,
                            activityLevel = activity,
                            dailyKcalTarget = 0,
                            dailyProteinTarget = 0,
                            targetWeightKg = targetWeight.toDoubleOrNull(),
                            focusAreas = focusAreas.joinToString(","),
                            bodyFormNow = bodyFormNow,
                            bodyFormGoal = bodyFormGoal,
                            trainLocation = trainLocation,
                            availableEquipment = equipment,
                            timePerSession = timePerSession,
                            sessionsPerWeek = sessionsPerWeek,
                            healthRestrictions = healthRestrictions.joinToString(","),
                            motivation = motivationText
                        )

                        val profile = tempProfile.copy(
                            dailyKcalTarget = NutrientCalculator.tdee(tempProfile),
                            dailyProteinTarget = NutrientCalculator.targetProtein(tempProfile)
                        )
                        db.userDao().save(profile)
                        SettingsManager.setModuleSelection(ctx, moduleChoice)
                        onFinish()
                    }
                }
            }
        }
    }
}
