package com.fantasyfoodplanner.logic

import android.content.Context
import android.net.Uri
import com.fantasyfoodplanner.data.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class BackupPayload(
    val profile: UserProfile?,
    val weights: List<WeightEntry>,
    val workouts: List<WorkoutEntry>,
    val exerciseLogs: List<ExerciseLog>,
    val sets: List<SetLog>,
    val recipes: List<Recipe>,
    val products: List<Product>,
    val meals: List<MealEntry>,
    val manualMeals: List<ManualMealEntry>,
    val moduleSelection: String? = null,
    val trainingType: String? = null,
    val learningProfile: LearningProfile? = null,
    val checkInLogs: List<CheckInLog>? = null
)

data class BackupContainer(
    val backupFormatVersion: Int = 1,
    val schemaVersion: Int = 3, // Current version with String IDs
    val appVersionCreated: String = "1.0.0_fixed",
    val createdAt: Long = System.currentTimeMillis(),
    val payload: BackupPayload
)

object BackupManager {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    suspend fun createBackup(ctx: Context, db: AppDb, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = BackupPayload(
                profile = db.userDao().profile().first(),
                weights = db.weightDao().getAll().first(),
                workouts = db.workoutDao().getAllWorkouts().first(),
                exerciseLogs = db.workoutDao().getAllWithSets().first().map { it.log },
                sets = db.workoutDao().getAllWithSets().first().flatMap { it.sets },
                recipes = db.recipeDao().getAll().first(),
                products = db.productDao().getAll().first(),
                meals = db.mealDao().getAll().first(),
                manualMeals = db.manualMealDao().getAll().first(),
                moduleSelection = SettingsManager.getModuleSelection(ctx).name,
                trainingType = SettingsManager.getTrainingType(ctx).name,
                learningProfile = SettingsManager.getLearningProfile(ctx),
                checkInLogs = SettingsManager.getCheckInLogs(ctx)
            )

            val container = BackupContainer(payload = payload)

            ctx.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(gson.toJson(container).toByteArray())
            }
            Result.success("Backup erfolgreich erstellt")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importBackup(ctx: Context, db: AppDb, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = ctx.contentResolver.openInputStream(uri)?.use { isr ->
                BufferedReader(InputStreamReader(isr)).readText()
            } ?: return@withContext Result.failure(Exception("Datei konnte nicht gelesen werden"))

            val root = gson.fromJson(json, JsonObject::class.java)
            
            // Check versions
            val backupFormat = root.get("backupFormatVersion")?.asInt ?: 1
            val schemaVer = root.get("schemaVersion")?.asInt ?: 1

            if (backupFormat > 1) {
                // Future format? If we can parse it, we try. 
                // In a real app we might check if critical changes happened.
            }

            // Migration logic
            val container = if (schemaVer < 3) {
                migrateToSchemaV3(root, schemaVer)
            } else {
                gson.fromJson(json, BackupContainer::class.java)
            }

            // Transactional Import (Full Replace)
            db.runInTransaction {
                try {
                    // 1. Clear all
                    db.recipeDao().clearAllSync()
                    db.productDao().clearAllSync()
                    db.mealDao().clearAllSync()
                    db.userDao().clearAllSync()
                    db.weightDao().clearAllSync()
                    db.workoutDao().clearAllWorkoutsSync()
                    db.workoutDao().clearAllLogsSync()
                    db.workoutDao().clearAllSetsSync()
                    db.manualMealDao().clearAllSync()

                    // 2. Insert all
                    val p = container.payload
                    p.profile?.let { db.userDao().saveSync(it) }
                    db.weightDao().insertAllSync(p.weights)
                    db.recipeDao().insertAllSync(p.recipes)
                    db.productDao().insertAllSync(p.products)
                    db.workoutDao().insertAllWorkoutsSync(p.workouts)
                    db.workoutDao().insertLogsSync(p.exerciseLogs)
                    db.workoutDao().insertSetsSync(p.sets)
                    db.manualMealDao().insertAllSync(p.manualMeals)
                    db.mealDao().insertAllSync(p.meals)

                    // 3. Settings (non-DB)
                    p.moduleSelection?.let { 
                        try { SettingsManager.setModuleSelection(ctx, ModuleSelection.valueOf(it)) } catch(e:Exception){}
                    }
                    p.trainingType?.let { 
                        try { SettingsManager.setTrainingType(ctx, TrainingType.valueOf(it)) } catch(e:Exception){}
                    }
                    p.learningProfile?.let { SettingsManager.saveLearningProfile(ctx, it) }
                    p.checkInLogs?.let { logs ->
                        ctx.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit()
                            .putString("checkin_logs", Gson().toJson(logs)).apply()
                    }
                } catch (e: Exception) {
                    throw e // Trigger Rollback
                }
            }

            Result.success("Import erfolgreich (Schema $schemaVer -> 3)")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun migrateToSchemaV3(root: JsonObject, fromVersion: Int): BackupContainer {
        // Simple migration: parse what's there, missing String IDs will be generated or mapped
        // Since we changed IDs from Int to String, we map them as strings "1", "2" etc.
        // This is a simplified migration for the prompt requirements.
        val legacy = gson.fromJson(root, LegacyBackupContainer::class.java)
        
        val payload = BackupPayload(
            profile = legacy.profile?.let { it.copy(id = "default_user") },
            weights = legacy.weights.map { it.toV3() },
            workouts = legacy.workouts.map { it.toV3() },
            exerciseLogs = legacy.exerciseLogs.map { it.toV3() },
            sets = legacy.sets.map { it.toV3() },
            recipes = legacy.recipes.map { it.toV3() },
            products = legacy.products.map { it.toV3() },
            meals = legacy.meals.map { it.toV3() },
            manualMeals = legacy.manualMeals.map { it.toV3() },
            moduleSelection = legacy.moduleSelection,
            trainingType = legacy.trainingType,
            learningProfile = legacy.learningProfile,
            checkInLogs = legacy.checkInLogs
        )
        return BackupContainer(schemaVersion = 3, payload = payload)
    }
}

// Helper classes for Migration
data class LegacyBackupContainer(
    val profile: UserProfile?,
    val weights: List<LegacyWeightEntry>,
    val workouts: List<LegacyWorkoutEntry>,
    val exerciseLogs: List<LegacyExerciseLog>,
    val sets: List<LegacySetLog>,
    val recipes: List<LegacyRecipe>,
    val products: List<LegacyProduct>,
    val meals: List<LegacyMealEntry>,
    val manualMeals: List<LegacyManualMealEntry>,
    val moduleSelection: String?,
    val trainingType: String?,
    val learningProfile: LearningProfile?,
    val checkInLogs: List<CheckInLog>?
)

data class LegacyWeightEntry(val id: Int, val dateEpochDay: Long, val weightKg: Double) {
    fun toV3() = WeightEntry(id = id.toString(), dateEpochDay = dateEpochDay, weightKg = weightKg)
}
data class LegacyWorkoutEntry(val id: Int, val dateEpochDay: Long, val type: String, val completed: Boolean) {
    fun toV3() = WorkoutEntry(id = id.toString(), dateEpochDay = dateEpochDay, type = type, completed = completed)
}
data class LegacyExerciseLog(val id: Long, val dateEpochDay: Long, val workoutType: String, val exerciseName: String, val exerciseType: String, val plannedSets: Int, val actualSetsDone: Int, val totalRepsDone: Int, val weightKg: Double, val difficultyLevel: Int, val timeTargetSeconds: Int, val wasSuccessful: Boolean, val scoreValue: Double) {
    fun toV3() = ExerciseLog(id = id.toString(), dateEpochDay = dateEpochDay, workoutType = workoutType, exerciseName = exerciseName, exerciseType = exerciseType, plannedSets = plannedSets, actualSetsDone = actualSetsDone, totalRepsDone = totalRepsDone, weightKg = weightKg, difficultyLevel = difficultyLevel, timeTargetSeconds = timeTargetSeconds, wasSuccessful = wasSuccessful, scoreValue = scoreValue)
}
data class LegacySetLog(val id: Long, val exerciseLogId: Long, val setIndex: Int, val repsDone: Int?, val timeDoneSeconds: Int?, val setSuccess: Boolean, val rpe: Int?) {
    fun toV3() = SetLog(id = id.toString(), exerciseLogId = exerciseLogId.toString(), setIndex = setIndex, repsDone = repsDone, timeDoneSeconds = timeDoneSeconds, setSuccess = setSuccess, rpe = rpe)
}
data class LegacyRecipe(val id: Int, val name: String, val ingredients: String, val kcal: Int, val protein: Double, val carbs: Double, val fat: Double, val servings: Int, val instructions: String, val category: String) {
    fun toV3() = Recipe(id = id.toString(), name = name, ingredients = ingredients, kcal = kcal, protein = protein, carbs = carbs, fat = fat, servings = servings, instructions = instructions, category = category)
}
data class LegacyProduct(val id: Int, val name: String, val kcal: Int, val protein: Double, val carbs: Double, val fat: Double) {
    fun toV3() = Product(id = id.toString(), name = name, kcal = kcal, protein = protein, carbs = carbs, fat = fat)
}
data class LegacyMealEntry(val id: Int, val dateEpochDay: Long, val recipeId: Int?, val productId: Int?, val foodName: String?, val servings: Int, val grams: Int) {
    fun toV3() = MealEntry(id = id.toString(), dateEpochDay = dateEpochDay, recipeId = recipeId?.toString(), productId = productId?.toString(), foodName = foodName, servings = servings, grams = grams)
}
data class LegacyManualMealEntry(val id: Int, val dateEpochDay: Long, val name: String, val kcal: Int, val protein: Double, val carbs: Double, val fat: Double, val createdAt: Long) {
    fun toV3() = ManualMealEntry(id = id.toString(), dateEpochDay = dateEpochDay, name = name, kcal = kcal, protein = protein, carbs = carbs, fat = fat, createdAt = createdAt)
}

// Add sync methods to Dao interfaces for transaction support
// Note: I will update Dao.kt in the next step to add these sync methods.
