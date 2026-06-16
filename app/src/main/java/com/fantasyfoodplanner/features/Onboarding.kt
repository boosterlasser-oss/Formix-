package com.fantasyfoodplanner.features

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fantasyfoodplanner.data.*
import com.fantasyfoodplanner.ui.*
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDb.get(ctx) }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("build") }

    FantasySurface {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            FText("Benutzerprofil erstellen", sizeSp = 24, bold = true)
            Spacer(Modifier.height(16.dp))
            FantasyTextField(name, { name = it }, "Dein Name")
            Spacer(Modifier.height(16.dp))
            FantasyButton("Analyse starten", Modifier.fillMaxWidth()) {
                if (name.isNotEmpty()) {
                    scope.launch {
                        db.userDao().save(UserProfile(name = name, goal = goal, weightKg = 75.0, dailyKcalTarget = 2500))
                        onFinish()
                    }
                }
            }
        }
    }
}
