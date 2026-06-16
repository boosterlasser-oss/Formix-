package com.fantasyfoodplanner.features.fitness.training

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fantasyfoodplanner.features.fitness.WorkoutPlan
import com.fantasyfoodplanner.ui.*

/**
 * Dialog: Trainingsplan-Vorschau vor dem Start
 */
@Composable
fun PlanPreviewDialog(
    plan: WorkoutPlan,
    onStartTraining: () -> Unit,
    onDismiss: () -> Unit
) {
    val exercisesOnly = plan.exercises.filter { it.type == "ex" }
    val warmups = plan.exercises.filter { it.type == "warmup" }
    val cooldowns = plan.exercises.filter { it.type == "cooldown" }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FantasyColors.CardBg,
        title = {
            Column(Modifier.fillMaxWidth()) {
                FText("📋 Dein Trainingsplan", sizeSp = 20, bold = true, color = FantasyColors.Accent)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FText("${exercisesOnly.size} Übungen", sizeSp = 13, bold = true, color = FantasyColors.Gold)
                    FText("  •  ", sizeSp = 13, color = Color.Gray)
                    FText("ca. ${plan.estTotalMinutes} Min.", sizeSp = 13, color = Color.Gray)
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Aufwärmen
                if (warmups.isNotEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFF9800).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                FText("🔥 Aufwärmen", sizeSp = 14, bold = true, color = Color(0xFFFF9800))
                                Spacer(Modifier.height(6.dp))
                                warmups.forEach { w ->
                                    FText("• ${w.title} – ${w.minutes} Min.", sizeSp = 12, color = FantasyColors.Text)
                                }
                            }
                        }
                    }
                }
                // Übungen
                exercisesOnly.forEachIndexed { idx, ex ->
                    item {
                        val detail = when {
                            ex.durationSeconds > 0 -> "${ex.sets} × ${ex.durationSeconds}s"
                            ex.weight > 0 -> "${ex.sets} × ${ex.reps} Reps @ ${ex.weight}kg"
                            else -> "${ex.sets} × ${ex.reps} Reps"
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(FantasyColors.Accent.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .border(1.dp, FantasyColors.Accent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(28.dp)
                                        .background(FantasyColors.Accent, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FText("${idx + 1}", sizeSp = 13, bold = true, color = Color.White)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    FText(ex.ex, sizeSp = 14, bold = true, color = FantasyColors.Text)
                                    FText(detail, sizeSp = 12, color = FantasyColors.GrayText)
                                }
                            }
                        }
                    }
                }
                // Cooldown
                if (cooldowns.isNotEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF4CAF50).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                FText("🧘 Cooldown", sizeSp = 14, bold = true, color = Color(0xFF4CAF50))
                                Spacer(Modifier.height(6.dp))
                                cooldowns.forEach { c ->
                                    FText("• ${c.title} – ${c.minutes} Min.", sizeSp = 12, color = FantasyColors.Text)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FantasyButton("Training starten") {
                onStartTraining()
            }
        },
        dismissButton = {
            FantasyButton("Zurück", alpha = 0.6f) {
                onDismiss()
            }
        }
    )
}

