package com.fantasyfoodplanner.data
import android.content.Context
import android.util.Log
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities=[Recipe::class, Product::class, MealEntry::class, UserProfile::class, WeightEntry::class, WorkoutEntry::class, ExerciseLog::class, SetLog::class, ManualMealEntry::class, ScannedFoodEntity::class], version=9, exportSchema = true)
abstract class AppDb: RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun productDao(): ProductDao
    abstract fun mealDao(): MealDao
    abstract fun userDao(): UserDao
    abstract fun weightDao(): WeightDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun manualMealDao(): ManualMealDao
    abstract fun scannedFoodDao(): ScannedFoodDao

    companion object {
        @Volatile private var I: AppDb? = null

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS ScannedFoodEntity (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    code TEXT NOT NULL,
                    codeType TEXT NOT NULL DEFAULT 'UNKNOWN',
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    name TEXT,
                    brand TEXT,
                    imageUrl TEXT,
                    kcal100g REAL,
                    protein100g REAL,
                    carbs100g REAL,
                    fat100g REAL,
                    nutritionGrade TEXT,
                    servingSize TEXT,
                    lastError TEXT
                )""")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_ScannedFoodEntity_code ON ScannedFoodEntity(code)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_WeightEntry_dateEpochDay ON WeightEntry(dateEpochDay)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_WorkoutEntry_dateEpochDay ON WorkoutEntry(dateEpochDay)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_WorkoutEntry_type ON WorkoutEntry(type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ExerciseLog_dateEpochDay ON ExerciseLog(dateEpochDay)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ExerciseLog_exerciseName ON ExerciseLog(exerciseName)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ExerciseLog_workoutType_dateEpochDay ON ExerciseLog(workoutType, dateEpochDay)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_SetLog_exerciseLogId ON SetLog(exerciseLogId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_MealEntry_dateEpochDay ON MealEntry(dateEpochDay)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ManualMealEntry_dateEpochDay ON ManualMealEntry(dateEpochDay)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_Recipe_name ON Recipe(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_Product_name ON Product(name)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d("AppDb", "Start Migration 8→9: UserProfile & WorkoutEntry erweitern")
                
                // UserProfile: 10 neue Felder für Personalisierung
                db.execSQL("ALTER TABLE UserProfile ADD COLUMN targetWeightKg REAL")
                db.execSQL("ALTER TABLE UserProfile ADD COLUMN focusAreas TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE UserProfile ADD COLUMN bodyFormNow TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE UserProfile ADD COLUMN bodyFormGoal TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE UserProfile ADD COLUMN trainLocation TEXT NOT NULL DEFAULT 'gym'")
                db.execSQL("ALTER TABLE UserProfile ADD COLUMN availableEquipment TEXT NOT NULL DEFAULT 'full'")
                db.execSQL("ALTER TABLE UserProfile ADD COLUMN timePerSession INTEGER NOT NULL DEFAULT 45")
                db.execSQL("ALTER TABLE UserProfile ADD COLUMN sessionsPerWeek INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE UserProfile ADD COLUMN healthRestrictions TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE UserProfile ADD COLUMN motivation TEXT NOT NULL DEFAULT ''")
                
                // WorkoutEntry: Fokus-Tracking für adaptives Lernen
                db.execSQL("ALTER TABLE WorkoutEntry ADD COLUMN focusAreas TEXT NOT NULL DEFAULT ''")
                
                Log.d("AppDb", "Migration 8→9 erfolgreich abgeschlossen")
            }
        }

        fun get(ctx: Context): AppDb = I ?: synchronized(this) {
            I ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "fantasy-db")
                .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build().also { I = it }
        }
    }
}
