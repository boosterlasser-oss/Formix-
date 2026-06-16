package com.fantasyfoodplanner.logic

import com.fantasyfoodplanner.data.*

/**
 * Adapter-Prinzip: Mappt FoodItems (Obst/Gemüse) auf die bestehende MealEntry-Logik.
 * Dadurch werden alle Berechnungen (NutrientCalculator) und Statistiken automatisch korrekt befüllt.
 */
object FoodAdapter {
    fun mapFoodToMealEntry(food: FoodItem, grams: Int, dateEpochDay: Long): MealEntry {
        // Wir nutzen das bestehende foodName Feld, da der NutrientCalculator 
        // dieses bereits korrekt über die foodDatabase auflöst.
        return MealEntry(
            dateEpochDay = dateEpochDay,
            foodName = food.name,
            grams = grams
        )
    }
}
