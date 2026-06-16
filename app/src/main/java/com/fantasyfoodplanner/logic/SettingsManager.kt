package com.fantasyfoodplanner.logic

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

enum class ModuleSelection { FITNESS_ONLY, NUTRITION_ONLY, BOTH }
enum class TrainingType { CROSSFIT, STRENGTH, BASICS, HOME, OTHER_ACTIVITY }

// Neue Enum für Ernährungs-spezifische Trainingsarten
enum class NutritionTrainingType { KRAFTTRAINING, AUSDAUER, HIIT, LEICHT_MOBILITY, KEIN_TRAINING }

data class LearningProfile(
    val baselineReadiness: Float = 70f,
    val typicalSuccessRate: Float = 0.85f,
    val fatigueSensitivity: Float = 1.0f,
    val sessionCount: Int = 0
)

data class CheckInLog(
    val timestamp: Long,
    val text: String,
    val type: TrainingType,
    val readiness: Int,
    val intensityMod: Float,
    val successRate: Float = 0f
)

object SettingsManager {
    private const val PREFS_NAME = "app_settings"
    private const val SECURE_PREFS_NAME = "formix_secure_prefs"
    private val gson = Gson()

    private fun getSecurePrefs(ctx: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                ctx, SECURE_PREFS_NAME, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback auf normale Prefs wenn Verschluesselung fehlschlaegt
            ctx.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getModuleSelection(ctx: Context): ModuleSelection {
        val str = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("module_selection", "BOTH")
        return try { ModuleSelection.valueOf(str ?: "BOTH") } catch(e: Exception) { ModuleSelection.BOTH }
    }

    fun setModuleSelection(ctx: Context, sel: ModuleSelection) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString("module_selection", sel.name).apply()
    }

    fun getTrainingType(ctx: Context): TrainingType {
        val str = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("training_type", "STRENGTH")
        return try { TrainingType.valueOf(str ?: "STRENGTH") } catch(e: Exception) { TrainingType.STRENGTH }
    }

    fun setTrainingType(ctx: Context, type: TrainingType) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString("training_type", type.name).apply()
    }

    // Persistenz für Ernährungs-Trainingsart (Scope Punkt 3)
    fun getNutritionTrainingType(ctx: Context): NutritionTrainingType {
        val str = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("nutrition_training_type", NutritionTrainingType.KRAFTTRAINING.name)
        return try { NutritionTrainingType.valueOf(str ?: NutritionTrainingType.KRAFTTRAINING.name) } catch(e: Exception) { NutritionTrainingType.KRAFTTRAINING }
    }

    fun setNutritionTrainingType(ctx: Context, type: NutritionTrainingType) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString("nutrition_training_type", type.name).apply()
    }

    fun getLearningProfile(ctx: Context): LearningProfile {
        val json = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("learning_profile", null)
        return if (json == null) LearningProfile() else try { gson.fromJson(json, LearningProfile::class.java) } catch(e: Exception) { LearningProfile() }
    }

    fun saveLearningProfile(ctx: Context, profile: LearningProfile) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString("learning_profile", gson.toJson(profile)).apply()
    }
    
    fun isModuleSetupDone(ctx: Context): Boolean {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).contains("module_selection")
    }

    fun addCheckIn(ctx: Context, log: CheckInLog) {
        val logs = getCheckInLogs(ctx).toMutableList()
        logs.add(log)
        if (logs.size > 50) logs.removeAt(0)
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString("checkin_logs", gson.toJson(logs)).apply()
    }

    fun getCheckInLogs(ctx: Context): List<CheckInLog> {
        val json = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("checkin_logs", null)
        return if (json == null) emptyList() else try {
            val type = object : TypeToken<List<CheckInLog>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) { emptyList() }
    }

    fun calibrateProfile(ctx: Context, sessionSuccessRate: Float) {
        val p = getLearningProfile(ctx)
        val alpha = 0.1f
        val newSuccess = p.typicalSuccessRate * (1-alpha) + sessionSuccessRate * alpha
        
        val lastCheck = getCheckInLogs(ctx).lastOrNull()
        var newSens = p.fatigueSensitivity
        if (lastCheck != null) {
            if (lastCheck.intensityMod < 1.0f && sessionSuccessRate > 0.9f) newSens *= 0.95f
            if (lastCheck.intensityMod > 1.0f && sessionSuccessRate < 0.7f) newSens *= 1.05f
        }
        
        saveLearningProfile(ctx, p.copy(
            typicalSuccessRate = newSuccess,
            fatigueSensitivity = newSens.coerceIn(0.5f, 2.0f),
            sessionCount = p.sessionCount + 1
        ))
    }

    fun getSkippedExercises(ctx: Context): List<String> {
        val json = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("skipped_today", null)
        val date = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong("skipped_date", 0)
        if (date != java.time.LocalDate.now().toEpochDay()) return emptyList()
        return if (json == null) emptyList() else try { gson.fromJson(json, Array<String>::class.java).toList() } catch(e: Exception) { emptyList() }
    }

    fun addSkippedExercise(ctx: Context, name: String) {
        val current = getSkippedExercises(ctx).toMutableSet()
        current.add(name)
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("skipped_today", gson.toJson(current))
            .putLong("skipped_date", java.time.LocalDate.now().toEpochDay())
            .apply()
    }

    fun removeSkippedExercise(ctx: Context, name: String) {
        val current = getSkippedExercises(ctx).toMutableSet()
        current.remove(name)
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("skipped_today", gson.toJson(current))
            .apply()
    }
}

