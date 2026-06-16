package com.fantasyfoodplanner.features

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fantasyfoodplanner.ui.*

@Composable
fun LegalScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val policyText = remember {
        try {
            ctx.assets.open("privacy_policy.txt").bufferedReader().use { it.readText() }
        } catch (_: Exception) { "Datenschutzerklärung nicht verfügbar." }
    }

    FantasySurface {
        Column(Modifier.fillMaxSize()) {
            MainAppBar(title = "Rechtliches", onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                FText("Datenschutzerklärung", sizeSp = 18, bold = true, color = FantasyColors.Accent)
                Spacer(Modifier.height(4.dp))
                FText("Zuletzt aktualisiert: 16.06.2026", sizeSp = 11, color = FantasyColors.GrayText)
                Spacer(Modifier.height(16.dp))

                policyText.split("\n").forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                    } else if (trimmed.all { it.isUpperCase() || it in " –" } || trimmed.endsWith(":")) {
                        FText(trimmed, sizeSp = 13, bold = true, color = FantasyColors.Text)
                        Spacer(Modifier.height(4.dp))
                    } else {
                        FText(trimmed, sizeSp = 12, color = FantasyColors.GrayText)
                    }
                }
            }
        }
    }
}
