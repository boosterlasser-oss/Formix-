@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.fantasyfoodplanner.features.fitness.training

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fantasyfoodplanner.data.AppDb
import com.fantasyfoodplanner.data.ExerciseLog
import com.fantasyfoodplanner.data.WorkoutEntry
import com.fantasyfoodplanner.features.fitness.*
import com.fantasyfoodplanner.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val TAG = "OtherActivity"

private enum class ActivityPhase {
    INPUT,
    CONFIRMATION,
    SAVING,
    DONE
}

private val POPULAR_SPORTS = listOf(
    "Fußball", "Basketball", "Volleyball", "Tennis", "Badminton",
    "Schwimmen", "Laufen", "Radfahren", "Wandern", "Klettern",
    "Bowling", "Kegeln", "Tanzen", "Yoga", "Pilates",
    "Kampfsport", "Tischtennis", "Skateboarden", "Reiten", "Rudern"
)

@Composable
fun OtherActivityStep(
    onActivitySaved: () -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDb.get(ctx) }

    var phase by remember { mutableStateOf(ActivityPhase.INPUT) }

    var sportName by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf("") }
    var selectedIntensity by remember { mutableStateOf<ActivityIntensity?>(null) }
    var selectedFocus by remember { mutableStateOf<ActivityFocus?>(null) }
    var note by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val activityResult: ActivityResult? = remember(sportName, durationMinutes, selectedIntensity, selectedFocus) {
        val duration = durationMinutes.toIntOrNull()
        if (sportName.isNotBlank() && duration != null && duration > 0 && selectedIntensity != null && selectedFocus != null) {
            val category = computeActivityCategory(duration, selectedIntensity!!)
            ActivityResult(
                sportName = sportName.trim(),
                durationMinutes = duration,
                intensity = selectedIntensity!!,
                focus = selectedFocus!!,
                category = category,
                note = note.trim()
            )
        } else null
    }

    val accentGreen = Color(0xFF4CAF50)
    val bgDark = Color(0xFF050510)
    val cardBg = Color(0xFF0D0D1A)

    fun saveActivity(result: ActivityResult) {
        scope.launch {
            isSaving = true
            phase = ActivityPhase.SAVING
            try {
                val today = LocalDate.now().toEpochDay()

                val workoutEntry = WorkoutEntry(
                    dateEpochDay = today,
                    type = "OTHER_ACTIVITY",
                    completed = true
                )

                val exerciseLog = ExerciseLog(
                    dateEpochDay = today,
                    workoutType = result.focus.dbKey,
                    exerciseName = result.sportName,
                    exerciseType = "OTHER",
                    plannedSets = 1,
                    actualSetsDone = 1,
                    totalRepsDone = result.durationMinutes,
                    weightKg = when (result.category) {
                        ActivityCategory.FULL_WORKOUT -> 1.0
                        ActivityCategory.SUPPLEMENTARY -> 2.0
                        ActivityCategory.LIGHT_MOVEMENT -> 3.0
                    },
                    difficultyLevel = result.intensity.dbValue,
                    timeTargetSeconds = result.durationMinutes * 60,
                    wasSuccessful = true,
                    scoreValue = result.score
                )

                withContext(Dispatchers.IO) {
                    db.workoutDao().insert(workoutEntry)
                    db.workoutDao().insertLog(exerciseLog)
                }

                Log.i(TAG, "Aktivität gespeichert: ${result.sportName}, " +
                        "${result.durationMinutes}min, ${result.intensity.label}, " +
                        "Fokus=${result.focus.label}, Kategorie=${result.category}, " +
                        "Score=${result.score}")

                phase = ActivityPhase.DONE

                withContext(Dispatchers.Main) {
                    Toast.makeText(ctx, "Aktivität gespeichert!", Toast.LENGTH_SHORT).show()
                    onActivitySaved()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Speichern der Aktivität", e)
                phase = ActivityPhase.INPUT
                isSaving = false
                withContext(Dispatchers.Main) {
                    Toast.makeText(ctx, "Fehler beim Speichern: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    FantasySurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgDark)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accentGreen.copy(alpha = 0.1f))
                        .border(1.dp, accentGreen.copy(alpha = 0.4f), CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FText("🏃", sizeSp = 22)
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    FText("ANDERE AKTIVITÄT", sizeSp = 17, bold = true, color = accentGreen)
                    FText("Strukturierte Eingabe", sizeSp = 12, color = FantasyColors.GrayText)
                }

                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    FText("✕", sizeSp = 18, bold = true, color = FantasyColors.GrayText)
                }
            }

            HorizontalDivider(color = accentGreen.copy(alpha = 0.15f), thickness = 1.dp)

            when {
                phase == ActivityPhase.CONFIRMATION && activityResult != null -> {
                    ActivityConfirmationView(
                        result = activityResult!!,
                        noteText = note,
                        onNoteChange = { note = it },
                        onConfirm = { saveActivity(activityResult!!) },
                        onEdit = { phase = ActivityPhase.INPUT },
                        onCancel = onBack,
                        accentColor = accentGreen
                    )
                }

                phase == ActivityPhase.SAVING || phase == ActivityPhase.DONE -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = accentGreen)
                            Spacer(Modifier.height(16.dp))
                            FText("Wird gespeichert...", sizeSp = 16, color = accentGreen)
                        }
                    }
                }

                else -> {
                    ActivityFormView(
                        sportName = sportName,
                        onSportNameChange = { sportName = it },
                        durationMinutes = durationMinutes,
                        onDurationChange = { durationMinutes = it },
                        selectedIntensity = selectedIntensity,
                        onIntensitySelect = { selectedIntensity = it },
                        selectedFocus = selectedFocus,
                        onFocusSelect = { selectedFocus = it },
                        note = note,
                        onNoteChange = { note = it },
                        onConfirm = {
                            if (activityResult != null) {
                                phase = ActivityPhase.CONFIRMATION
                            }
                        },
                        isValid = activityResult != null,
                        accentColor = accentGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// FORMULAR-ANSICHT
// ════════════════════════════════════════════════════════════════

@Composable
private fun ActivityFormView(
    sportName: String,
    onSportNameChange: (String) -> Unit,
    durationMinutes: String,
    onDurationChange: (String) -> Unit,
    selectedIntensity: ActivityIntensity?,
    onIntensitySelect: (ActivityIntensity) -> Unit,
    selectedFocus: ActivityFocus?,
    onFocusSelect: (ActivityFocus) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
    isValid: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val cardBg = Color(0xFF0D0D1A)

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            FText("Welche Sportart / Aktivität?", sizeSp = 16, bold = true, highlight = true)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = sportName,
                onValueChange = onSportNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { FText("z.B. Fußball, Schwimmen, Wandern...", sizeSp = 14, color = FantasyColors.GrayText) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = FantasyColors.Text,
                    unfocusedTextColor = FantasyColors.Text,
                    cursorColor = accentColor,
                    focusedBorderColor = accentColor.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                POPULAR_SPORTS.forEach { sport ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (sportName == sport) accentColor.copy(alpha = 0.2f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                1.dp,
                                if (sportName == sport) accentColor.copy(alpha = 0.5f)
                                else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onSportNameChange(sport) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        FText(sport, sizeSp = 12, color = if (sportName == sport) accentColor else FantasyColors.GrayText)
                    }
                }
            }
        }

        item {
            FText("Wie lange? (Minuten)", sizeSp = 16, bold = true, highlight = true)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("15", "30", "45", "60", "90", "120").forEach { minutes ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (durationMinutes == minutes) accentColor.copy(alpha = 0.2f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                1.dp,
                                if (durationMinutes == minutes) accentColor.copy(alpha = 0.5f)
                                else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onDurationChange(minutes) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        FText("${minutes}m", sizeSp = 13, color = if (durationMinutes == minutes) accentColor else FantasyColors.GrayText)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = durationMinutes,
                onValueChange = { onDurationChange(it.filter { c -> c.isDigit() }) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { FText("Oder eigene Dauer eingeben...", sizeSp = 14, color = FantasyColors.GrayText) },
                suffix = { FText("Minuten", sizeSp = 12, color = FantasyColors.GrayText) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = FantasyColors.Text,
                    unfocusedTextColor = FantasyColors.Text,
                    cursorColor = accentColor,
                    focusedBorderColor = accentColor.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        item {
            FText("Wie anstrengend war es?", sizeSp = 16, bold = true, highlight = true)
            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ActivityIntensity.entries.forEach { intensity ->
                    val isSelected = selectedIntensity == intensity
                    val intensityColor = when (intensity) {
                        ActivityIntensity.LIGHT -> Color(0xFF81C784)
                        ActivityIntensity.MEDIUM -> Color(0xFFFFB74D)
                        ActivityIntensity.HARD -> Color(0xFFFF8A65)
                        ActivityIntensity.VERY_HARD -> Color(0xFFE57373)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) intensityColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                            .border(
                                1.dp,
                                if (isSelected) intensityColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onIntensitySelect(intensity) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) intensityColor else intensityColor.copy(alpha = 0.4f))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                FText(
                                    intensity.label,
                                    sizeSp = 15,
                                    bold = isSelected,
                                    color = if (isSelected) intensityColor else FantasyColors.Text
                                )
                                FText(
                                    "Faktor: ×${intensity.factor}",
                                    sizeSp = 11,
                                    color = FantasyColors.GrayText
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            FText("Welcher Bereich wurde beansprucht?", sizeSp = 16, bold = true, highlight = true)
            Spacer(Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ActivityFocus.entries.forEach { focus ->
                    val isSelected = selectedFocus == focus
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.2f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) accentColor.copy(alpha = 0.5f)
                                else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onFocusSelect(focus) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        FText(
                            focus.label,
                            sizeSp = 13,
                            color = if (isSelected) accentColor else FantasyColors.GrayText,
                            bold = isSelected
                        )
                    }
                }
            }
        }

        item {
            FText("Notiz (optional)", sizeSp = 16, bold = true, highlight = true)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { FText("z.B. \"Vereinstraining, super Stimmung\"", sizeSp = 14, color = FantasyColors.GrayText) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = FantasyColors.Text,
                    unfocusedTextColor = FantasyColors.Text,
                    cursorColor = accentColor,
                    focusedBorderColor = accentColor.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
            FantasyButton(
                label = "Zusammenfassung anzeigen",
                modifier = Modifier.fillMaxWidth(),
                alpha = if (isValid) 1f else 0.4f
            ) {
                if (isValid) onConfirm()
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════════
// BESTÄTIGUNGS-ANSICHT
// ════════════════════════════════════════════════════════════════

@Composable
private fun ActivityConfirmationView(
    result: ActivityResult,
    noteText: String,
    onNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    accentColor: Color
) {
    val categoryLabel = when (result.category) {
        ActivityCategory.FULL_WORKOUT -> "Volles Training"
        ActivityCategory.SUPPLEMENTARY -> "Ergänzende Aktivität"
        ActivityCategory.LIGHT_MOVEMENT -> "Leichte Bewegung"
    }
    val categoryColor = when (result.category) {
        ActivityCategory.FULL_WORKOUT -> Color(0xFF2196F3)
        ActivityCategory.SUPPLEMENTARY -> Color(0xFFFF9800)
        ActivityCategory.LIGHT_MOVEMENT -> Color(0xFF9E9E9E)
    }
    val cardBg = Color(0xFF0D0D1A)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        FText("Zusammenfassung", sizeSp = 22, bold = true, highlight = true)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row {
                FText("Sportart:", sizeSp = 14, color = FantasyColors.GrayText)
                Spacer(Modifier.width(8.dp))
                FText(result.sportName, sizeSp = 14, bold = true)
            }

            Row {
                FText("Dauer:", sizeSp = 14, color = FantasyColors.GrayText)
                Spacer(Modifier.width(8.dp))
                FText("${result.durationMinutes} Minuten", sizeSp = 14, bold = true)
            }

            Row {
                FText("Intensität:", sizeSp = 14, color = FantasyColors.GrayText)
                Spacer(Modifier.width(8.dp))
                FText(result.intensity.label, sizeSp = 14, bold = true)
            }

            Row {
                FText("Fokus:", sizeSp = 14, color = FantasyColors.GrayText)
                Spacer(Modifier.width(8.dp))
                FText(result.focus.label, sizeSp = 14, bold = true)
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                FText("Einstufung:", sizeSp = 14, color = FantasyColors.GrayText)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(categoryColor.copy(alpha = 0.15f))
                        .border(1.dp, categoryColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    FText(categoryLabel, sizeSp = 13, bold = true, color = categoryColor)
                }
            }

            Row {
                FText("Score:", sizeSp = 14, color = FantasyColors.GrayText)
                Spacer(Modifier.width(8.dp))
                FText("${"%.1f".format(result.score)} Punkte", sizeSp = 14, bold = true, color = accentColor)
            }
        }

        OutlinedTextField(
            value = noteText,
            onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            label = { FText("Notiz (optional)", sizeSp = 12, color = FantasyColors.GrayText) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = FantasyColors.Text,
                unfocusedTextColor = FantasyColors.Text,
                cursorColor = accentColor,
                focusedBorderColor = accentColor.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg
            ),
            shape = RoundedCornerShape(12.dp),
            maxLines = 2
        )

        Spacer(Modifier.weight(1f))

        FantasyButton(
            label = "Bestätigen & Speichern",
            modifier = Modifier.fillMaxWidth()
        ) {
            onConfirm()
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FantasyButton(
                label = "Bearbeiten",
                modifier = Modifier.weight(1f),
                alpha = 0.6f
            ) {
                onEdit()
            }

            FantasyButton(
                label = "Abbrechen",
                modifier = Modifier.weight(1f),
                alpha = 0.4f
            ) {
                onCancel()
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
