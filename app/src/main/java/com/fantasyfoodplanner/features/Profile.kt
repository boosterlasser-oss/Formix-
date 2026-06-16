package com.fantasyfoodplanner.features

import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fantasyfoodplanner.BuildConfig
import com.fantasyfoodplanner.data.*
import com.fantasyfoodplanner.logic.BackupManager
import com.fantasyfoodplanner.logic.ModuleSelection
import com.fantasyfoodplanner.logic.SettingsManager
import com.fantasyfoodplanner.logic.SubscriptionManager
import com.fantasyfoodplanner.logic.SubscriptionTier
import com.fantasyfoodplanner.logic.TrainingType
import com.fantasyfoodplanner.ui.*
import com.fantasyfoodplanner.utils.NutrientCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun FullProfileScreen(
    onBack: () -> Unit,
    onGoSystemLog: () -> Unit,
    onGoUpgrade: () -> Unit = {},
    onGoLegal: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val db = remember { AppDb.get(ctx) }
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<UserProfile?>(null) }
    
    var showModuleDialog by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }

    // Screenshot-Schutz: FLAG_SECURE verhindert Screenshots und Screen-Recording
    DisposableEffect(Unit) {
        val window = (ctx as? android.app.Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    LaunchedEffect(Unit) { user = db.userDao().profile().first() }

    val createBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            scope.launch {
                val res = BackupManager.createBackup(ctx, db, it)
                if (res.isSuccess) Toast.makeText(ctx, "Backup erstellt!", Toast.LENGTH_LONG).show()
                else Toast.makeText(ctx, "Fehler: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                val res = BackupManager.importBackup(ctx, db, it)
                if (res.isSuccess) {
                    Toast.makeText(ctx, "Import erfolgreich!", Toast.LENGTH_LONG).show()
                    onBack() // Refresh by going back
                }
                else Toast.makeText(ctx, "Fehler: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
            Modifier
                .fillMaxSize()
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                MainAppBar("Benutzerprofil", onBack = onBack)
                Spacer(Modifier.height(24.dp))

                user?.let { profile ->
                    var name by remember { mutableStateOf(profile.name) }
                    var weight by remember { mutableStateOf(profile.weightKg.toString()) }
                    var height by remember { mutableStateOf(profile.heightCm.toString()) }
                    var age by remember { mutableStateOf(profile.age.toString()) }
                    var sex by remember { mutableStateOf(profile.sex) }
                    var goal by remember { mutableStateOf(profile.goal) }
                    var experience by remember { mutableStateOf(profile.experience) }
                    var activity by remember { mutableStateOf(profile.activityLevel) }

                    FText("BASIS-DATEN", color = FantasyColors.Accent, bold = true, sizeSp = 14)
                    FantasyTextField(name, { name = it }, "Name")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FantasyTextField(age, { age = it }, "Alter", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                        FantasyTextField(height, { height = it }, "Größe (cm)", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    }
                    FantasyTextField(weight, { weight = it }, "Gewicht (kg)", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Number)

                    Spacer(Modifier.height(16.dp))
                    FText("GESCHLECHT (FÜR 3D MODELL)", sizeSp = 12, color = Color.Gray)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("male" to "Männlich", "female" to "Weiblich").forEach { (id, label) ->
                            FantasyButton(label = label, modifier = Modifier.weight(1f), alpha = if(sex == id) 1f else 0.4f) { sex = id }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    FText("EINSTELLUNGEN", color = FantasyColors.Accent, bold = true, sizeSp = 14)
                    Spacer(Modifier.height(8.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FantasyButton("Module verwalten", Modifier.weight(1f), alpha = 0.9f) { showModuleDialog = true }
                        FantasyButton("Trainingstyp", Modifier.weight(1f), alpha = 0.9f) { showTypeDialog = true }
                    }

                    Spacer(Modifier.height(24.dp))
                    FText("TRAININGSZIELE & ERFAHRUNG", color = FantasyColors.Accent, bold = true, sizeSp = 14)
                    Spacer(Modifier.height(8.dp))
                    FText("Dein Ziel:", sizeSp = 12, color = Color.Gray)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("build" to "Aufbau", "lose" to "Abnehmen", "fit" to "Fitness").forEach { (id, label) ->
                            FantasyButton(label = label, modifier = Modifier.weight(1f), alpha = if(goal == id) 1f else 0.4f) { goal = id }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    FText("Dein Trainingslevel:", sizeSp = 12, color = Color.Gray)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("new" to "Anfänger", "some" to "Fortgeschr.", "pro" to "Profi").forEach { (id, label) ->
                            FantasyButton(label = label, modifier = Modifier.weight(1f), alpha = if(experience == id) 1f else 0.4f) { experience = id }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    FText("Aktivitätsgrad im Alltag:", sizeSp = 12, color = Color.Gray)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                         listOf("sedentary" to "Sitzend", "moderate" to "Aktiv", "active" to "Sehr aktiv").forEach { (id, label) ->
                            FantasyButton(label = label, modifier = Modifier.weight(1f), alpha = if(activity == id) 1f else 0.4f) { activity = id }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                    FantasyButton(label = "Profil speichern", modifier = Modifier.fillMaxWidth()) {
                        scope.launch {
                            val tempProfile = profile.copy(
                                name = name,
                                weightKg = weight.toDoubleOrNull() ?: profile.weightKg,
                                heightCm = height.toIntOrNull() ?: profile.heightCm,
                                age = age.toIntOrNull() ?: profile.age,
                                sex = sex,
                                goal = goal,
                                experience = experience,
                                activityLevel = activity
                            )
                            db.userDao().save(tempProfile.copy(
                                dailyKcalTarget = NutrientCalculator.tdee(tempProfile),
                                dailyProteinTarget = NutrientCalculator.targetProtein(tempProfile)
                            ))
                            onBack()
                        }
                    }
                }

                Column(Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = FantasyColors.GrayText.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FText("DATENSICHERUNG & BACKUP", color = FantasyColors.Accent, bold = true, sizeSp = 14)
                        Spacer(Modifier.width(8.dp))
                        FText("PREMIUM", sizeSp = 10, bold = true, color = Color(0xFF8B5CF6))
                    }

                    val hasPremiumBackup = SubscriptionManager.getCurrentTier(ctx) != SubscriptionTier.FREE

                    if (!hasPremiumBackup) {
                        // FREE-Nutzer – kein Export
                        Spacer(Modifier.height(8.dp))
                        FText(
                            "Lokales Backup & Export ist ab PREMIUM verfuegbar.",
                            color = Color.Gray, sizeSp = 11
                        )
                        Spacer(Modifier.height(8.dp))
                        FantasyButton("Jetzt upgraden – PREMIUM", Modifier.fillMaxWidth()) {
                            onGoUpgrade()
                        }
                    } else {
                        // PREMIUM / PRO
                        FText("Sichere deine Trainingsdaten in einer lokalen Datei.", color = Color.Gray, sizeSp = 11)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FantasyButton("Backup erstellen", Modifier.weight(1f)) {
                                createBackupLauncher.launch("formix_backup_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}.json")
                            }
                            FantasyButton("Importieren", Modifier.weight(1f), alpha = 0.8f) {
                                importBackupLauncher.launch(arrayOf("application/json"))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    FantasyButton("System-Log anzeigen", Modifier.fillMaxWidth(), alpha = 0.9f) {
                        onGoSystemLog()
                    }

                    // ── DEBUG-ONLY: Tier-Unlock ──────────────────────────
                    if (BuildConfig.DEBUG) {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFFF4444).copy(alpha = 0.4f))
                        Spacer(Modifier.height(8.dp))
                        FText("DEBUG – TIER SIMULIEREN", sizeSp = 11, bold = true, color = Color(0xFFFF6666))
                        FText("Nur im Debug-Build. Im Release nicht vorhanden.", sizeSp = 10, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FantasyButton("FREE", Modifier.weight(1f), alpha = 0.7f) {
                                SubscriptionManager.setTier(ctx, SubscriptionTier.FREE)
                                Toast.makeText(ctx, "Tier: FREE", Toast.LENGTH_SHORT).show()
                            }
                            FantasyButton("PREMIUM", Modifier.weight(1f)) {
                                SubscriptionManager.setTier(ctx, SubscriptionTier.PREMIUM)
                                Toast.makeText(ctx, "Tier: PREMIUM", Toast.LENGTH_SHORT).show()
                            }
                            // PRO wurde entfernt
                            FantasyButton("PRO (entfernt)", Modifier.weight(1f), alpha = 0.5f) {
                                Toast.makeText(ctx, "PRO wurde entfernt", Toast.LENGTH_SHORT).show()
                            }
                        }
                        FText("Nach Wechsel: Screen verlassen und neu öffnen.", sizeSp = 10, color = Color.Gray)
                    }
                }

                // ═══════════════════════════════════════════
                // ABO-STATUS & UPGRADE
                // ═══════════════════════════════════════════
                Column(Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = FantasyColors.GrayText.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    FText("ABO & PREMIUM", color = FantasyColors.Accent, bold = true, sizeSp = 14)
                    Spacer(Modifier.height(8.dp))

                    val currentTier = SubscriptionManager.getCurrentTier(ctx)
                    val (tierLabel, tierColor) = when (currentTier) {
                        SubscriptionTier.PREMIUM -> "PREMIUM" to Color(0xFF8B5CF6)
                        SubscriptionTier.FREE    -> "FREE" to Color.Gray
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = tierColor.copy(alpha = 0.12f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            FText("Aktueller Plan", sizeSp = 11, color = Color.Gray)
                            FText(tierLabel, sizeSp = 18, bold = true, color = tierColor)
                        }
                        if (currentTier == SubscriptionTier.FREE) {
                            FText("Upgrade verfügbar", sizeSp = 12, color = Color(0xFF8B5CF6))
                        }
                    }
                    Spacer(Modifier.height(10.dp))

                    if (currentTier == SubscriptionTier.FREE) {
                        FantasyButton("Jetzt upgraden – PREMIUM", Modifier.fillMaxWidth()) {
                            onGoUpgrade()
                        }
                    } else {
                        FText("Du geniesst alle Premium-Features.", sizeSp = 12, color = Color(0xFF4CAF50))
                    }
                }

                // ═══════════════════════════════════════════
                // RECHTLICHES (Pflicht für DE / Play Store)
                // ═══════════════════════════════════════════
                Column(Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = FantasyColors.GrayText.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    FText("RECHTLICHES", color = FantasyColors.Accent, bold = true, sizeSp = 14)
                    FText("Datenschutz und Impressum.", color = Color.Gray, sizeSp = 11)
                    Spacer(Modifier.height(8.dp))
                    FantasyButton("Datenschutzerklärung & Impressum", Modifier.fillMaxWidth(), alpha = 0.85f) {
                        onGoLegal()
                    }
                }

                Spacer(Modifier.height(40.dp))
            }

            if (showModuleDialog) {
                ModuleChoiceDialog(
                    current = SettingsManager.getModuleSelection(ctx),
                    onDismiss = { showModuleDialog = false },
                    onSelect = { SettingsManager.setModuleSelection(ctx, it); showModuleDialog = false }
                )
            }

            if (showTypeDialog) {
                TrainingTypeDialog(
                    current = SettingsManager.getTrainingType(ctx),
                    onDismiss = { showTypeDialog = false },
                    onSelect = { SettingsManager.setTrainingType(ctx, it); showTypeDialog = false }
                )
            }
        }

}

@Composable
fun ModuleChoiceDialog(current: ModuleSelection, onDismiss: () -> Unit, onSelect: (ModuleSelection) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151515),
        title = { FText("Module verwalten", color = FantasyColors.Accent, bold = true) },
        text = {
            Column {
                ModuleSelection.entries.forEach { sel ->
                    val label = when(sel) {
                        ModuleSelection.FITNESS_ONLY -> "Nur Fitness"
                        ModuleSelection.NUTRITION_ONLY -> "Nur Ernährung"
                        ModuleSelection.BOTH -> "Beides nutzen"
                    }
                    FantasyButton(label, Modifier.fillMaxWidth().padding(vertical = 4.dp), alpha = if(current == sel) 1f else 0.5f) { onSelect(sel) }
                }
            }
        },
        confirmButton = { FantasyButton("Abbrechen") { onDismiss() } }
    )
}

@Composable
fun TrainingTypeDialog(current: TrainingType, onDismiss: () -> Unit, onSelect: (TrainingType) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151515),
        title = { FText("Trainingstyp ändern", color = FantasyColors.Accent, bold = true) },
        text = {
            Column {
                TrainingType.entries.forEach { type ->
                    val label = when(type) {
                        TrainingType.CROSSFIT -> "CrossFit Performance"
                        TrainingType.STRENGTH -> "Krafttraining (Gym)"
                        TrainingType.BASICS -> "Fitness Basics"
                        TrainingType.HOME -> "Zuhause Workout"
                        TrainingType.OTHER_ACTIVITY -> "Andere Sportart / Aktivität"
                    }
                    FantasyButton(label, Modifier.fillMaxWidth().padding(vertical = 4.dp), alpha = if(current == type) 1f else 0.5f) { onSelect(type) }
                }
            }
        },
        confirmButton = { FantasyButton("Abbrechen") { onDismiss() } }
    )
}
