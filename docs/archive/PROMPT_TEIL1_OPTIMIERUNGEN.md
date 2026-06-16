# FORMIX PROMPT TEIL 1: Code-Optimierungen

## Projekt-Info
- **Pfad:** `D:\Entwicklung\Android\FORMIX`
- **Package:** `com.fantasyfoodplanner.fix.v4.debug`
- **Version:** 3.0.0 → 3.1.0 (nach Fixes)
- **DB-Version:** 7 → 8 (nach Migration)
- **Backup:** `D:\Backups\FORMIX_Backup_20260407_1638`

## Deine Aufgabe

Führe diese 5 Optimierungen durch:

---

## 1. DATENBANK-INDIZES (Entities.kt)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/data/Entities.kt`

Füge Indizes zu allen Entities hinzu:

```kotlin
@Entity(indices = [Index(value = ["dateEpochDay"])])
data class WeightEntry(...)

@Entity(indices = [Index(value = ["dateEpochDay"]), Index(value = ["type"])])
data class WorkoutEntry(...)

@Entity(indices = [Index(value = ["dateEpochDay"]), Index(value = ["exerciseName"]), Index(value = ["workoutType", "dateEpochDay"])])
data class ExerciseLog(...)

@Entity(indices = [Index(value = ["exerciseLogId"])])
data class SetLog(...)

@Entity(indices = [Index(value = ["dateEpochDay"])])
data class MealEntry(...)

@Entity(indices = [Index(value = ["dateEpochDay"])])
data class ManualMealEntry(...)

@Entity(indices = [Index(value = ["name"])])
data class Recipe(...)

@Entity(indices = [Index(value = ["name"])])
data class Product(...)
```

---

## 2. MIGRATION_7_8 (AppDb.kt)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/data/AppDb.kt`

1. Version auf 8 erhöhen
2. Migration hinzufügen:

```kotlin
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
```

3. Migration registrieren: `.addMigrations(MIGRATION_6_7, MIGRATION_7_8)`

---

## 3. DATE-RANGE-QUERIES (Dao.kt)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/data/Dao.kt`

Neue Queries hinzufügen:

```kotlin
// MealDao
@Query("SELECT * FROM MealEntry WHERE dateEpochDay = :date")
fun getMealsForDate(date: Long): Flow<List<MealEntry>>

@Query("SELECT * FROM MealEntry WHERE dateEpochDay BETWEEN :start AND :end")
fun getMealsInRange(start: Long, end: Long): Flow<List<MealEntry>>

// WorkoutDao
@Query("SELECT * FROM WorkoutEntry WHERE dateEpochDay = :date")
fun getWorkoutsForDate(date: Long): Flow<List<WorkoutEntry>>

@Query("SELECT * FROM WorkoutEntry WHERE dateEpochDay BETWEEN :start AND :end")
fun getWorkoutsInRange(start: Long, end: Long): Flow<List<WorkoutEntry>>
```

---

## 4. ACCESSIBILITY (FantasyKit.kt)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/ui/FantasyKit.kt`

Bei FantasyButton und allen klickbaren Elementen:

```kotlin
Box(
    modifier = modifier
        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        .clickable(enabled = enabled) { onClick() }
        .semantics {
            role = Role.Button
            contentDescription = label
        }
)
```

---

## 5. API-KEY VERSCHLÜSSELUNG (SettingsManager.kt)

**Datei:** `app/src/main/java/com/fantasyfoodplanner/logic/SettingsManager.kt`

1. Dependency in build.gradle.kts:
```kotlin
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

2. EncryptedSharedPreferences nutzen:
```kotlin
private fun getEncryptedPrefs(ctx: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(ctx)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    return EncryptedSharedPreferences.create(
        ctx, "formix_secure_prefs", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
```

---

## Nach Abschluss

1. `gradlew clean assembleDebug`
2. App testen (nicht deinstallieren - Migration!)
3. Weiter mit PROMPT_TEIL2_MONETARISIERUNG.md
