package com.fantasyfoodplanner.features.fitness

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fantasyfoodplanner.ui.*

@Composable
fun FitnessOnboarding(
    onFinish: (FitnessProfile) -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var profile by remember { mutableStateOf(FitnessProfile()) }

    FantasySurface {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            MainAppBar(
                title = "Trainings-Setup",
                onBack = { if (step > 0) step-- }
            )

            Spacer(Modifier.height(24.dp))

            when (step) {
                0 -> GenderStep(profile.gender) { profile = profile.copy(gender = it); step++ }
                1 -> GoalStep(profile.mainGoal) { profile = profile.copy(mainGoal = it); step++ }
                2 -> ExperienceStep(profile.experience) { profile = profile.copy(experience = it); step++ }
                3 -> {
                    onFinish(profile)
                }
            }
        }
    }
}

@Composable fun GenderStep(curr: String, onSelect: (String) -> Unit) = ChoiceStep("Geschlecht?", listOf("m" to "Männlich", "f" to "Weiblich"), curr, onSelect)
@Composable fun GoalStep(curr: String, onSelect: (String) -> Unit) = ChoiceStep("Dein Ziel?", listOf("build" to "Muskelaufbau", "lose" to "Gewichtsreduktion", "fit" to "Fitness"), curr, onSelect)
@Composable fun ExperienceStep(curr: String, onSelect: (String) -> Unit) = ChoiceStep("Dein Level?", listOf("new" to "Anfänger", "some" to "Fortgeschritten", "pro" to "Profi"), curr, onSelect)

@Composable
fun ChoiceStep(title: String, options: List<Pair<String, String>>, current: String, onSelect: (String) -> Unit) {
    Column {
        FText(title, sizeSp = 22, bold = true)
        Spacer(Modifier.height(20.dp))
        options.forEach { (id, label) ->
            FantasyButton(
                label = label,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                alpha = if (current == id) 1f else 0.5f
            ) {
                onSelect(id)
            }
        }
    }
}
