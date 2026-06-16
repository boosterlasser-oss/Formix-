package com.fantasyfoodplanner.utils
import com.fantasyfoodplanner.data.*
import kotlin.math.roundToInt

data class DaySummaryData(val kcal: Int, val p: Double, val c: Double, val f: Double)

object NutrientCalculator {
    fun tdee(p: UserProfile): Int {
        // Mifflin-St Jeor Equation
        val s = if (p.sex.lowercase() == "male") 5 else -161
        val bmr = 10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age + s
        
        val af = when(p.activityLevel) {
            "sedentary" -> 1.2
            "light" -> 1.375
            "moderate" -> 1.55
            "active" -> 1.725
            "very_active" -> 1.9
            else -> 1.55
        }
        
        var baseTdee = bmr * af
        
        // === V3.2.0: Training Frequency Bonus ===
        // Add TDEE bonus based on training frequency (sessionsPerWeek)
        if (p.sessionsPerWeek >= 3) {
            val trainingBonus = when {
                p.sessionsPerWeek >= 5 -> 0.15 // 5+ sessions = +15% TDEE
                p.sessionsPerWeek >= 4 -> 0.12 // 4 sessions = +12% TDEE
                else -> 0.08 // 3 sessions = +8% TDEE
            }
            baseTdee *= (1.0 + trainingBonus)
        }
        
        // Anpassung basierend auf dem Ziel
        var targetTdee = when(p.goal) {
            "lose" -> baseTdee * 0.8 // 20% Defizit
            "build" -> baseTdee * 1.1 // 10% Überschuss
            else -> baseTdee
        }
        
        // === V3.2.0: Target Weight Adjustment ===
        // Adjust calorie deficit/surplus based on target weight difference
        if (p.targetWeightKg != null && p.targetWeightKg > 0) {
            val weightDiff = p.targetWeightKg - p.weightKg
            when {
                weightDiff < -15 -> targetTdee *= 0.75 // Large deficit: -15kg+ → -25% calories
                weightDiff < -10 -> targetTdee *= 0.80 // Medium deficit: -10kg → -20% calories
                weightDiff < -5 -> targetTdee *= 0.85  // Small deficit: -5kg → -15% calories
                weightDiff > 15 -> targetTdee *= 1.25  // Large surplus: +15kg+ → +25% calories
                weightDiff > 10 -> targetTdee *= 1.20  // Medium surplus: +10kg → +20% calories
                weightDiff > 5 -> targetTdee *= 1.15   // Small surplus: +5kg → +15% calories
                // If within ±5kg, no additional adjustment (goal-based adjustment already applied above)
            }
        }
        
        return targetTdee.roundToInt()
    }
    
    fun targetProtein(p: UserProfile): Int {
        // === V3.2.0: Use target weight if available ===
        // Proteinbedarf: ca 1.8g - 2.2g pro kg Körpergewicht (Zielgewicht bevorzugt)
        val referenceWeight = p.targetWeightKg?.takeIf { it > 0 } ?: p.weightKg
        return (referenceWeight * 2.0).roundToInt()
    }

    /**
     * Zentralisierte Summenbildung für die Ernährung.
     * Nutzt für alle Typen (Rezepte, Produkte, Obst/Gemüse, Manuell) denselben Codepfad.
     */
    fun calculateTotals(
        meals: List<MealEntry>,
        manualMeals: List<ManualMealEntry>,
        recipes: List<Recipe>,
        products: List<Product>
    ): DaySummaryData {
        var tk = 0.0
        var tp = 0.0
        var tc = 0.0
        var tf = 0.0

        meals.forEach { entry ->
            when {
                entry.recipeId != null -> {
                    recipes.find { it.id == entry.recipeId }?.let { r ->
                        val factor = entry.servings.toDouble().coerceAtLeast(1.0)
                        tk += r.kcal * factor
                        tp += r.protein * factor
                        tc += r.carbs * factor
                        tf += r.fat * factor
                    }
                }
                entry.productId != null -> {
                    products.find { it.id == entry.productId }?.let { p ->
                        if (entry.grams > 0) {
                            val factor = entry.grams / 100.0
                            tk += p.kcal * factor
                            tp += p.protein * factor
                            tc += p.carbs * factor
                            tf += p.fat * factor
                        } else {
                            tk += p.kcal
                            tp += p.protein
                            tc += p.carbs
                            tf += p.fat
                        }
                    }
                }
                entry.foodName != null -> {
                    foodDatabase.find { it.name == entry.foodName }?.let { f ->
                        val factor = entry.grams / 100.0
                        tk += f.caloriesPer100g * factor
                        tp += f.proteinPer100g * factor
                        tc += f.carbsPer100g * factor
                        tf += f.fatPer100g * factor
                    }
                }
            }
        }

        manualMeals.forEach { m ->
            tk += m.kcal
            tp += m.protein
            tc += m.carbs
            tf += m.fat
        }

        return DaySummaryData(tk.toInt(), tp, tc, tf)
    }
}
