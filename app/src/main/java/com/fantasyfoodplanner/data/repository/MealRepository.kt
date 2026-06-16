package com.fantasyfoodplanner.data.repository

import com.fantasyfoodplanner.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Repository für Mahlzeiten, Rezepte und Produkte.
 * Kapselt den Datenbankzugriff für alle ernährungsbezogenen Daten.
 */
class MealRepository(private val db: AppDb) {

    fun recipesFlow(): Flow<List<Recipe>> = db.recipeDao().getAll()

    fun productsFlow(): Flow<List<Product>> = db.productDao().getAll()

    fun mealsOnDayFlow(dateEpochDay: Long): Flow<List<MealEntry>> = db.mealDao().mealsOn(dateEpochDay)

    fun allMealsFlow(): Flow<List<MealEntry>> = db.mealDao().getAll()

    fun manualMealsOnDayFlow(dateEpochDay: Long): Flow<List<ManualMealEntry>> = db.manualMealDao().getForDay(dateEpochDay)

    fun allManualMealsFlow(): Flow<List<ManualMealEntry>> = db.manualMealDao().getAll()

    suspend fun getRecipes(): List<Recipe> = withContext(Dispatchers.IO) {
        db.recipeDao().getAll().first()
    }

    suspend fun getProducts(): List<Product> = withContext(Dispatchers.IO) {
        db.productDao().getAll().first()
    }

    suspend fun upsertMeal(meal: MealEntry) = withContext(Dispatchers.IO) {
        db.mealDao().upsert(meal)
    }

    suspend fun deleteMeal(meal: MealEntry) = withContext(Dispatchers.IO) {
        db.mealDao().delete(meal)
    }

    suspend fun insertManualMeal(entry: ManualMealEntry) = withContext(Dispatchers.IO) {
        db.manualMealDao().insert(entry)
    }

    suspend fun deleteManualMeal(entry: ManualMealEntry) = withContext(Dispatchers.IO) {
        db.manualMealDao().delete(entry)
    }
}

