package com.fantasyfoodplanner.data

/**
 * Zentrale Engine zur automatischen Generierung von Performance-Tags.
 * Nutzt nun die exakten Tags aus dem Scope: pre_workout, post_workout, regeneration.
 */
object PerformanceTagEngine {
    fun computeTags(
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        vitaminC: Double = 0.0,
        potassium: Double = 0.0,
        magnesium: Double = 0.0,
        iron: Double = 0.0
    ): List<String> {
        val tags = mutableListOf<String>()
        
        // 1. Mikronährstoffe (Zusatz-Tags bleiben erhalten)
        if (potassium > 200) tags.add("Elektrolyt")
        
        // 2. Pre-Workout (Energie-Fokus) - pre_workout
        if (carbs > 15.0 && fat < 15.0) {
            tags.add("pre_workout")
        }
        
        // 3. Post-Workout (Wiederaufbau-Fokus) - post_workout
        if (protein > 8.0 && (carbs > 5.0 || kcal > 100)) {
            tags.add("post_workout")
        }

        // 4. Regeneration (Erholung) - regeneration
        if (protein > 15.0 || vitaminC > 30.0 || iron > 2.0) {
            tags.add("regeneration")
        }

        // 5. Muskelaufbau (Allgemein)
        if (protein > 12.0 || (protein > 5.0 && kcal > 150)) {
            tags.add("Muskelaufbau")
        }
        
        // 6. Diätfreundlich
        if (kcal < 60 && fat < 3.0) {
            tags.add("Diätfreundlich")
        }
        
        return tags.distinct()
    }
}
