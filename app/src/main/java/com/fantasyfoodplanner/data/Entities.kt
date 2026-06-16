package com.fantasyfoodplanner.data

import androidx.room.*
import java.util.UUID

@Entity
data class UserProfile(
    @PrimaryKey val id: String = "default_user",
    val name: String,
    val age: Int = 25,
    val weightKg: Double = 75.0,
    val heightCm: Int = 180,
    val sex: String = "male",
    val activityLevel: String = "moderate",
    val goal: String = "fit",
    val experience: String = "new",
    val dailyKcalTarget: Int = 2000,
    val dailyProteinTarget: Int = 150,
    // Onboarding v9: Personalisierung
    val targetWeightKg: Double? = null,
    val focusAreas: String = "",              // "bauch,beine,po"
    val bodyFormNow: String = "",
    val bodyFormGoal: String = "",
    val trainLocation: String = "gym",
    val availableEquipment: String = "full",
    val timePerSession: Int = 45,
    val sessionsPerWeek: Int = 3,
    val healthRestrictions: String = "",
    val motivation: String = ""
)

@Entity(indices = [Index(value = ["dateEpochDay"])])
data class WeightEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dateEpochDay: Long,
    val weightKg: Double
)

@Entity(indices = [Index(value = ["dateEpochDay"]), Index(value = ["type"])])
data class WorkoutEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dateEpochDay: Long,
    val type: String,
    val completed: Boolean = true,
    val focusAreas: String = ""  // v9: Adaptive Fokus-Tracking
)

@Entity(indices = [
    Index(value = ["dateEpochDay"]),
    Index(value = ["exerciseName"]),
    Index(value = ["workoutType", "dateEpochDay"])
])
data class ExerciseLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dateEpochDay: Long,
    val workoutType: String,
    val exerciseName: String,
    val exerciseType: String,
    val plannedSets: Int,
    val actualSetsDone: Int = 0,
    val totalRepsDone: Int = 0,
    val weightKg: Double = 0.0,
    val difficultyLevel: Int = 0,
    val timeTargetSeconds: Int = 0,
    val wasSuccessful: Boolean = false,
    val scoreValue: Double = 0.0
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ExerciseLog::class,
            parentColumns = ["id"],
            childColumns = ["exerciseLogId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["exerciseLogId"])]
)
data class SetLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val exerciseLogId: String,
    val setIndex: Int,
    val repsDone: Int? = null,
    val timeDoneSeconds: Int? = null,
    val setSuccess: Boolean = false,
    val rpe: Int? = null
)

@Entity(indices = [Index(value = ["name"])])
data class Recipe(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ingredients: String,
    val kcal: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val servings: Int = 1,
    val instructions: String = "",
    val category: String = "Allgemein"
)

@Entity(indices = [Index(value = ["name"])])
data class Product(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val kcal: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

@Entity(indices = [Index(value = ["dateEpochDay"])])
data class MealEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dateEpochDay: Long,
    val recipeId: String? = null,
    val productId: String? = null,
    val foodName: String? = null,
    val servings: Int = 1,
    val grams: Int = 0
)

@Entity(indices = [Index(value = ["dateEpochDay"])])
data class ManualMealEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dateEpochDay: Long,
    val name: String,
    val kcal: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val createdAt: Long = System.currentTimeMillis()
)

data class ExerciseWithSets(
    @Embedded val log: ExerciseLog,
    @Relation(parentColumn = "id", entityColumn = "exerciseLogId")
    val sets: List<SetLog>
)

/**
 * Gescannte Lebensmittel (Barcode/QR → Open Food Facts).
 * Code ist UNIQUE – jeder Barcode existiert nur einmal lokal.
 */
@Entity(indices = [Index(value = ["code"], unique = true)])
data class ScannedFoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val codeType: String = "UNKNOWN",        // QR, EAN, UPC, UNKNOWN
    val status: String = "PENDING",           // PENDING, OK, NOT_FOUND, ERROR
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val name: String? = null,
    val brand: String? = null,
    val imageUrl: String? = null,
    val kcal100g: Double? = null,
    val protein100g: Double? = null,
    val carbs100g: Double? = null,
    val fat100g: Double? = null,
    val nutritionGrade: String? = null,
    val servingSize: String? = null,
    val lastError: String? = null
)

