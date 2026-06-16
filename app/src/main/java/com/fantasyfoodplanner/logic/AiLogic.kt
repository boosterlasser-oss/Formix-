package com.fantasyfoodplanner.logic

import com.fantasyfoodplanner.data.Recipe
import com.fantasyfoodplanner.data.UserProfile
import kotlin.math.abs
import kotlin.math.max

enum class TimingMode { PRE, POST, NEUTRAL }
enum class UserGoal { AUFBAU, FETTABBAU, ERHALT, PERFORMANCE }

data class DayPlanResult(
    val remainingKcal: Int,
    val remainingProtein: Double,
    val remainingCarbs: Double,
    val remainingFat: Double,
    val suggestions: List<ScoredRecipe>,
    val timingMode: TimingMode = TimingMode.NEUTRAL,
    val goal: UserGoal = UserGoal.AUFBAU
)

data class ScoredRecipe(
    val recipe: Recipe,
    val score: Int, // 0-100 (100 = best)
    val label: String, // "✅ Optimal", etc.
    val reason: String,
    val isTopMatch: Boolean,
    val tags: List<String> = emptyList(),
    val betterAlternative: String? = null
)

object AiLogic {

    fun calculateRemaining(
        profile: UserProfile,
        consumedKcal: Int,
        consumedP: Double,
        consumedC: Double,
        consumedF: Double
    ): DayPlanResult {
        val rKcal = max(0, profile.dailyKcalTarget - consumedKcal)
        val rP = max(0.0, profile.dailyProteinTarget.toDouble() - consumedP)
        
        // Defaults if targets missing or to supplement
        val rC = max(0.0, (profile.dailyKcalTarget * 0.4 / 4.0) - consumedC)
        val rF = max(0.0, (profile.dailyKcalTarget * 0.3 / 9.0) - consumedF)

        return DayPlanResult(rKcal, rP, rC, rF, emptyList())
    }

    fun generateDayPlanWithContext(
        profile: UserProfile,
        allRecipes: List<Recipe>,
        consumedKcal: Int,
        consumedP: Double,
        consumedC: Double,
        consumedF: Double,
        timingMode: TimingMode = TimingMode.NEUTRAL,
        trainingIntensity: String = "MED",
        timeOffsetMinutes: Int = 60
    ): DayPlanResult {
        val context = calculateRemaining(profile, consumedKcal, consumedP, consumedC, consumedF)
        val goal = mapGoal(profile.goal)
        
        if (allRecipes.isEmpty()) return context.copy(timingMode = timingMode, goal = goal)

        val scored = allRecipes.map { recipe ->
            evaluateRecipe(recipe, timingMode, goal, context, trainingIntensity, timeOffsetMinutes)
        }.sortedByDescending { it.score }

        return context.copy(suggestions = scored, timingMode = timingMode, goal = goal)
    }

    private fun mapGoal(goalStr: String): UserGoal = when(goalStr.lowercase()) {
        "build", "aufbau" -> UserGoal.AUFBAU
        "lose", "fettabbau" -> UserGoal.FETTABBAU
        "fit", "erhalt" -> UserGoal.ERHALT
        "performance" -> UserGoal.PERFORMANCE
        else -> UserGoal.AUFBAU
    }

    private fun evaluateRecipe(
        recipe: Recipe,
        mode: TimingMode,
        goal: UserGoal,
        context: DayPlanResult,
        intensity: String,
        timeOffset: Int
    ): ScoredRecipe {
        var baseScore = 50
        var label = "⚠️ Geht, aber…"
        var reason = "Allgemeiner Vorschlag basierend auf deinen Zielen."
        var alternative: String? = null
        val tags = mutableListOf<String>()

        when (mode) {
            TimingMode.PRE -> {
                val carbRatio = recipe.carbs / (recipe.kcal.toDouble() / 10.0).coerceAtLeast(1.0)
                val fatRatio = recipe.fat / (recipe.kcal.toDouble() / 10.0).coerceAtLeast(1.0)
                
                if (timeOffset <= 45) {
                    // Fokus auf sehr schnelle Carbs, wenig Fett/Volumen
                    if (recipe.carbs > 30 && recipe.fat < 5 && recipe.kcal < 400) {
                        baseScore = 90
                        label = "✅ Optimal"
                        reason = "Schnelle Energie bei minimaler Verdauungsbelastung."
                    } else if (recipe.fat > 15 || recipe.kcal > 600) {
                        baseScore = 20
                        label = "❌ Nicht ideal"
                        reason = "Zu schwer verdaulich so kurz vor dem Training."
                        alternative = "Leichter Snack wie Banane oder Reiswaffel."
                    } else {
                        reason = "Moderate Energiequelle vor der Einheit."
                    }
                } else if (timeOffset <= 120) {
                    if (recipe.carbs > 40 && recipe.fat < 12) {
                        baseScore = 95
                        label = "✅ Optimal"
                        reason = "Gute Carbs für langanhaltende Power im Workout."
                    } else if (recipe.fat > 20) {
                        baseScore = 40
                        reason = "Etwas zu viel Fett, könnte schwer im Magen liegen."
                    }
                } else {
                    reason = "Ausgewogene Mahlzeit vor dem Training möglich."
                }
            }
            TimingMode.POST -> {
                val proteinContent = recipe.protein
                if (proteinContent > 30) baseScore += 30
                
                when (goal) {
                    UserGoal.AUFBAU -> {
                        if (proteinContent > 35 && recipe.carbs > 50) {
                            baseScore = 95
                            label = "✅ Optimal"
                            reason = "Viel Protein und Carbs für maximalen Muskelaufbau."
                        } else {
                            reason = "Solide Basis für die Regeneration."
                        }
                    }
                    UserGoal.PERFORMANCE -> {
                        if (proteinContent > 30 && recipe.carbs > 60) {
                            baseScore = 95
                            label = "✅ Optimal"
                            reason = "Füllt Glykogenspeicher optimal auf nach hoher Intensität."
                        }
                    }
                    UserGoal.FETTABBAU -> {
                        if (proteinContent > 40 && recipe.carbs < 40) {
                            baseScore = 90
                            label = "✅ Optimal"
                            reason = "Viel Protein bei moderaten Carbs zur Fettverbrennung."
                        } else if (recipe.kcal > 700) {
                            baseScore = 30
                            label = "❌ Nicht ideal"
                            reason = "Zu viele Kalorien für dein Ziel Fettabbau."
                            alternative = "Leichtere Variante mit mehr Gemüse und Pute."
                        }
                    }
                    UserGoal.ERHALT -> {
                        if (proteinContent > 25) baseScore = 80
                    }
                }
            }
            TimingMode.NEUTRAL -> {
                // Bestehendes Scoring Delta
                val dKcal = abs(context.remainingKcal - recipe.kcal).toDouble()
                val dP = abs(context.remainingProtein - recipe.protein)
                val fitScore = 100 - ((dKcal / 100.0) * 5.0 + dP * 2.0).toInt().coerceIn(0, 100)
                baseScore = fitScore
                reason = "Passt gut in dein verbleibendes Tagesbudget."
            }
        }

        // Burger Spezialregel (Punkt 9)
        if (recipe.name.lowercase().contains("burger")) {
            if (mode == TimingMode.POST && intensity == "HIGH" && (goal == UserGoal.AUFBAU || goal == UserGoal.PERFORMANCE)) {
                baseScore = maxOf(baseScore, 85)
                label = "✅ Optimal"
                reason = "Viel Energie und Protein nach harter Einheit ideal."
                alternative = "Besser ohne Pommes genießen."
            } else if (goal == UserGoal.FETTABBAU) {
                baseScore = minOf(baseScore, 50)
                label = "⚠️ Geht, aber…"
                reason = "Gönn dir, aber achte auf die Saucenmenge."
                alternative = "Chicken-Burger oder Salatbeilage wählen."
            }
        }

        if (recipe.protein > 30) tags.add("High Protein")
        if (recipe.carbs < 25) tags.add("Low Carb")
        if (recipe.fat < 10) tags.add("Low Fat")

        return ScoredRecipe(
            recipe = recipe,
            score = baseScore.coerceIn(0, 100),
            label = label,
            reason = reason,
            isTopMatch = baseScore >= 80,
            tags = tags,
            betterAlternative = alternative
        )
    }

    fun generateDayPlan(profile: UserProfile, allRecipes: List<Recipe>): List<Recipe> {
        val res = generateDayPlanWithContext(profile, allRecipes, 0, 0.0, 0.0, 0.0)
        return res.suggestions.take(3).map { it.recipe }
    }
}
