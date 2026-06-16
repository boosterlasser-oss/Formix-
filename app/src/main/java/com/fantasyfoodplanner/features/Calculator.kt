package com.fantasyfoodplanner.features

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fantasyfoodplanner.ui.*

enum class Gender { MALE, FEMALE }
enum class ActivityLevel(val factor: Double, val displayName: String) {
    SEDENTARY(1.2, "Sitzend"),
    LIGHT(1.375, "Leicht aktiv"),
    MODERATE(1.55, "Moderat aktiv"),
    VERY_ACTIVE(1.725, "Sehr aktiv")
}
enum class Goal(val calDelta: Int, val displayName: String) {
    LOSE(-400, "Abnehmen"),
    MAINTAIN(0, "Halten"),
    BUILD(+400, "Aufbau")
}

data class NutritionPlan(val totalCalories: Int, val protein: Int, val carbs: Int, val fat: Int)

@Composable
fun CalculatorScreen(onBack: () -> Unit) {
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(Gender.MALE) }
    var activity by remember { mutableStateOf(ActivityLevel.LIGHT) }
    var goal by remember { mutableStateOf(Goal.MAINTAIN) }

    var plan by remember { mutableStateOf<NutritionPlan?>(null) }

    FantasySurface {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            MainAppBar("Ernährungs-Analyse", onBack = onBack)
            Spacer(Modifier.height(16.dp))

            FText("1. Deine Körperdaten", bold = true, color = FantasyColors.Accent)
            FantasyTextField(weight, { weight = it }, "Gewicht (kg)", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Number)
            FantasyTextField(height, { height = it }, "Größe (cm)", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Number)
            FantasyTextField(age, { age = it }, "Alter", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Number)

            Spacer(Modifier.height(16.dp))
            FText("2. Dein Geschlecht", bold = true, color = FantasyColors.Accent)
            Selector(listOf(Gender.MALE, Gender.FEMALE), gender, { gender = it }) { if(it == Gender.MALE) "Mann" else "Frau" }
            
            Spacer(Modifier.height(16.dp))
            FText("3. Dein Aktivitätslevel", bold = true, color = FantasyColors.Accent)
            Selector(ActivityLevel.entries, activity, { activity = it }) { it.displayName }

            Spacer(Modifier.height(16.dp))
            FText("4. Dein Ziel", bold = true, color = FantasyColors.Accent)
            Selector(Goal.entries, goal, { goal = it }) { it.displayName }

            Spacer(Modifier.height(24.dp))
            FantasyButton("Berechnung starten", Modifier.fillMaxWidth()) {
                plan = calculatePlan(weight, height, age, gender, activity, goal)
            }

            plan?.let {
                Spacer(Modifier.height(24.dp))
                FantasyCard(Modifier.fillMaxWidth()) {
                    FText("Dein persönlicher Ernährungsplan:", sizeSp = 18, bold = true, color = FantasyColors.Accent)
                    Spacer(Modifier.height(12.dp))
                    FText("Tagesziel: ${it.totalCalories} kcal", sizeSp = 22, bold = true)
                    Spacer(Modifier.height(8.dp))
                    FText("Eiweiß: ${it.protein}g | Kohlenhydrate: ${it.carbs}g | Fett: ${it.fat}g", color = FantasyColors.GrayText, sizeSp = 14)
                }
            }
        }
    }
}

private fun calculatePlan(wStr: String, hStr: String, aStr: String, gender: Gender, activity: ActivityLevel, goal: Goal): NutritionPlan? {
    val w = wStr.toDoubleOrNull() ?: 0.0
    val h = hStr.toDoubleOrNull() ?: 0.0
    val a = aStr.toIntOrNull() ?: 0
    if (w <= 0 || h <= 0 || a <= 0) return null
    val genderBonus = if (gender == Gender.MALE) 5 else -161
    val bmr = (10 * w + 6.25 * h - 5 * a + genderBonus)
    val tdee = bmr * activity.factor
    val targetCalories = (tdee + goal.calDelta).toInt()
    val proteinGrams = (w * 2.2).toInt()
    val fatGrams = (targetCalories * 0.25 / 9).toInt()
    val carbGrams = ((targetCalories - (proteinGrams * 4) - (fatGrams * 9)) / 4).toInt()
    return NutritionPlan(targetCalories, proteinGrams, carbGrams, fatGrams)
}

@Composable
private fun <T> Selector(options: List<T>, selected: T, onSelect: (T) -> Unit, name: (T) -> String) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) {
            val alpha = if (it == selected) 1f else 0.6f
            FantasyButton(name(it), alpha = alpha) { onSelect(it) }
        }
    }
}
