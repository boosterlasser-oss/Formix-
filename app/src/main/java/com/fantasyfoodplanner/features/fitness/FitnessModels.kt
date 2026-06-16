package com.fantasyfoodplanner.features.fitness

import com.google.gson.Gson
import com.fantasyfoodplanner.ui.SetState
import java.time.LocalDate

// ════════════════════════════════════════════
// Andere Sportart / Aktivität — Enums & Datenklassen
// ════════════════════════════════════════════

/** Intensitätsstufe einer Aktivität mit Scoring-Faktor */
enum class ActivityIntensity(val factor: Double, val label: String, val dbValue: Int) {
    LIGHT(0.3, "Leicht", 1),
    MEDIUM(0.6, "Mittel", 2),
    HARD(1.0, "Anstrengend", 3),
    VERY_HARD(1.3, "Sehr anstrengend", 4);

    companion object {
        fun fromDbValue(value: Int): ActivityIntensity = entries.firstOrNull { it.dbValue == value } ?: MEDIUM
    }
}

/** Fokusbereich der Aktivität — beeinflusst Regenerationslogik */
enum class ActivityFocus(val label: String, val dbKey: String) {
    ENDURANCE("Ausdauer", "ENDURANCE"),
    STRENGTH("Kraft", "STRENGTH"),
    COORDINATION("Koordination", "COORDINATION"),
    FLEXIBILITY("Beweglichkeit", "FLEXIBILITY"),
    FULL_BODY("Ganzkörper", "FULL_BODY"),
    LEGS("Beine", "LEGS"),
    UPPER_BODY("Oberkörper", "UPPER_BODY"),
    ARMS_SHOULDERS("Arme + Schultern", "ARMS_SHOULDERS"),
    LEISURE("Freizeitaktivität", "LEISURE");

    companion object {
        fun fromDbKey(key: String): ActivityFocus = entries.firstOrNull { it.dbKey == key } ?: LEISURE
    }
}

/** Bewertungskategorie — bestimmt Farbe im Kalender und Streak-Wertung */
enum class ActivityCategory {
    /** Volles Training: Dauer >= 45min UND Intensität >= HARD, ODER Dauer >= 60min UND Intensität >= MEDIUM */
    FULL_WORKOUT,
    /** Ergänzende Aktivität: Dauer >= 20min UND Intensität >= MEDIUM, ODER Dauer >= 30min */
    SUPPLEMENTARY,
    /** Leichte Bewegung: alles andere */
    LIGHT_MOVEMENT
}

/** Ergebnis der Aktivitätserfassung (Chat oder Formular) */
data class ActivityResult(
    val sportName: String,
    val durationMinutes: Int,
    val intensity: ActivityIntensity,
    val focus: ActivityFocus,
    val category: ActivityCategory,
    val note: String = ""
) {
    /** Score = Dauer(min) × Intensitätsfaktor */
    val score: Double get() = durationMinutes * intensity.factor
}

/** Berechnet die Aktivitätskategorie aus Dauer und Intensität */
fun computeActivityCategory(durationMinutes: Int, intensity: ActivityIntensity): ActivityCategory {
    return when {
        durationMinutes >= 45 && intensity.dbValue >= ActivityIntensity.HARD.dbValue -> ActivityCategory.FULL_WORKOUT
        durationMinutes >= 60 && intensity.dbValue >= ActivityIntensity.MEDIUM.dbValue -> ActivityCategory.FULL_WORKOUT
        durationMinutes >= 20 && intensity.dbValue >= ActivityIntensity.MEDIUM.dbValue -> ActivityCategory.SUPPLEMENTARY
        durationMinutes >= 30 -> ActivityCategory.SUPPLEMENTARY
        else -> ActivityCategory.LIGHT_MOVEMENT
    }
}

// ════════════════════════════════════════════
// Bestehende Modelle (unverändert)
// ════════════════════════════════════════════

data class FitnessProfile(
    val gender: String = "male",
    val mainGoal: String = "build",
    val focusArea: String = "full",
    val motivation: String = "look",
    val experience: String = "new",
    val birthYear: Int = 1990,
    val heightCm: Int = 175,
    val weightKg: Double = 75.0,
    val targetWeightKg: Double = 80.0,
    val bodyFormNow: Int = 3,
    val bodyFormGoal: Int = 2,
    val job: String = "fulltime",
    val trainWhere: String = "gym",
    val workload: String = "mid"
)

data class WorkoutPlan(
    val dateEpochDay: Long = LocalDate.now().toEpochDay(),
    val targetMinutes: Int,
    val estTotalMinutes: Int,
    val restSeconds: Int,
    val exercises: List<WorkoutBlock>,
    val checkedIndices: List<Int> = emptyList(),
    val savedSetStates: Map<Int, List<SetState>> = emptyMap(),
    val currentExerciseIndex: Int = 0
)

data class WorkoutBlock(
    val type: String,
    val title: String = "",
    val items: List<String> = emptyList(),
    val minutes: Int = 0,
    val ex: String = "",
    val sets: Int = 0,
    val reps: Int = 12,
    val weight: Double = 0.0,
    val durationSeconds: Int = 0,
    val alts: List<String> = emptyList()
)

// ExerciseDefinition wurde nach ExerciseDefinitions.kt verschoben, um Konsistenz mit dem Enum zu wahren.

object FitnessPersistence {
    private const val PREFS_NAME = "fitness_prefs"
    private const val KEY_PLAN_PREFIX = "plan_"
    private val gson = Gson()

    fun savePlan(context: android.content.Context, type: String, plan: WorkoutPlan?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        if (plan == null) {
            prefs.edit().remove(KEY_PLAN_PREFIX + type).apply()
        } else {
            prefs.edit().putString(KEY_PLAN_PREFIX + type, gson.toJson(plan)).apply()
        }
    }

    fun loadPlan(context: android.content.Context, type: String): WorkoutPlan? {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PLAN_PREFIX + type, null) ?: return null
        val plan = try {
            gson.fromJson(json, WorkoutPlan::class.java)
        } catch (e: Exception) {
            null
        }
        
        if (plan != null && plan.dateEpochDay != LocalDate.now().toEpochDay()) {
            savePlan(context, type, null)
            return null
        }
        return plan
    }
}
