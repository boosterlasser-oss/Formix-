package com.fantasyfoodplanner.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object HelpContentRegistry {
    private val contentMap = mapOf(
        "FitnessDashboard" to """
            Trainingsübersicht
            Hier siehst du dein aktuelles Krafttraining. Der AI Coach plant deine Übungen basierend auf deinem Trainingsziel.

            Bedienung:
            1. Klappe eine Übung auf.
            2. Trage deine geschafften Wiederholungen/Sekunden pro Satz ein.
            3. Hake den Satz nach Abschluss ab.
            4. Wenn alle Sätze erledigt sind, gilt die Einheit als abgeschlossen.
            
            Anpassungen:
            Du kannst Werte jederzeit ändern. Der AI Coach berechnet den Fortschritt sofort neu.
        """.trimIndent(),
        "Elite" to """
            Elite Performance Training
            Dieser Plan fokussiert sich auf Übungen mit Zusatzgewichten. Die Gewichte werden basierend auf deiner Leistungsfähigkeit berechnet.

            Bedienung:
            Trage deine Leistungen in die Felder ein. Der AI Coach lernt bei jedem Training mit.
            
            Korrekturen:
            Einfach den Wert im Textfeld überschreiben oder den Haken bei Bedarf entfernen.
        """.trimIndent(),
        "CrossFit" to """
            CrossFit Training
            Ein hochintensives Zirkeltraining aus funktionalen Übungen.

            Bedienung:
            Absolviere die Übungen nacheinander. Markiere die Sätze nach Abschluss.
        """.trimIndent(),
        "Stats" to """
            Leistungsstatistik
            Hier siehst du deinen sportlichen Fortschritt über die Zeit.

            Bedienung:
            - Wähle oben den Zeitraum (Tag, Woche, Monat, Jahr).
            - Die Diagramme visualisieren dein Volumen und deine Kalorienbilanz.
        """.trimIndent()
    )

    fun get(screenId: String) = contentMap[screenId] ?: "Keine Informationen für diesen Bereich verfügbar."
}

@Composable
fun HelpFloatingButton(screenId: String) {
    var showDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomEnd) {
        FloatingActionButton(
            onClick = { showDialog = true },
            containerColor = FantasyColors.Accent,
            contentColor = FantasyColors.Text,
            shape = CircleShape,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = "Info")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { FText("Benutzer-Handbuch", color = FantasyColors.Accent, bold = true) },
            text = { 
                Column {
                    FText(HelpContentRegistry.get(screenId), sizeSp = 14)
                }
            },
            confirmButton = {
                FantasyButton(label = "OK") { showDialog = false }
            },
            containerColor = Color(0xFF151515)
        )
    }
}
