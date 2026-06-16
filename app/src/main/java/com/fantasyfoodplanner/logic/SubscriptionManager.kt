package com.fantasyfoodplanner.logic

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

enum class SubscriptionTier { FREE, PREMIUM }

object SubscriptionManager {
    private const val PREFS = "formix_subscription"
    private val _tierFlow = MutableStateFlow(SubscriptionTier.FREE)
    val tierFlow: StateFlow<SubscriptionTier> = _tierFlow

    fun init(ctx: Context) {
        _tierFlow.value = getCurrentTier(ctx)
    }

    fun getCurrentTier(ctx: Context): SubscriptionTier {
        val name = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("tier", "FREE") ?: "FREE"
        return try { SubscriptionTier.valueOf(name) } catch (e: Exception) { SubscriptionTier.FREE }
    }

    fun setTier(ctx: Context, tier: SubscriptionTier) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("tier", tier.name).apply()
        _tierFlow.value = tier
    }

    // ── Feature-Checks ──────────────────────────────────────
    fun hasBarcodeScanner(ctx: Context) = getCurrentTier(ctx) != SubscriptionTier.FREE
    fun hasAllRecipes(ctx: Context) = getCurrentTier(ctx) != SubscriptionTier.FREE
    fun hasAllTrainingTypes(ctx: Context) = getCurrentTier(ctx) != SubscriptionTier.FREE
    fun hasFullStats(ctx: Context) = getCurrentTier(ctx) != SubscriptionTier.FREE
    fun hasUnlimitedWorkouts(ctx: Context) = getCurrentTier(ctx) != SubscriptionTier.FREE
    /** Ernaehrungsmodul (Tagesplaner, Rezepte, Datenbank) – nur mit PREMIUM */
    fun hasNutritionModule(ctx: Context) = getCurrentTier(ctx) != SubscriptionTier.FREE

    // ── Free-Limits ─────────────────────────────────────────
    fun getMaxWorkoutsPerWeek(ctx: Context) =
        if (getCurrentTier(ctx) == SubscriptionTier.FREE) 5 else Int.MAX_VALUE

    fun getMaxRecipes(ctx: Context) =
        if (getCurrentTier(ctx) == SubscriptionTier.FREE) 50 else Int.MAX_VALUE

    /**
     * Prueft ob ein neues Workout gespeichert werden darf.
     * Free: max 5 Workouts pro Woche.
     */
    suspend fun canSaveWorkout(ctx: Context, db: com.fantasyfoodplanner.data.AppDb): Boolean {
        if (getCurrentTier(ctx) != SubscriptionTier.FREE) return true
        val weekStart = LocalDate.now().minusDays(7).toEpochDay()
        val count = db.workoutDao().countWorkoutsSince(weekStart)
        return count < 5
    }

    /**
     * Allowed training types based on subscription tier.
     * Free: BASICS, HOME, OTHER_ACTIVITY
     * Premium: all types
     */
    fun getAllowedTrainingTypes(ctx: Context): List<TrainingType> {
        return if (getCurrentTier(ctx) == SubscriptionTier.FREE) {
            listOf(TrainingType.BASICS, TrainingType.HOME, TrainingType.OTHER_ACTIVITY)
        } else {
            TrainingType.entries
        }
    }
}
