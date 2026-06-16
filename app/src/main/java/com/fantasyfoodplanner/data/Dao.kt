package com.fantasyfoodplanner.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface RecipeDao {
    @Query("SELECT * FROM Recipe ORDER BY name") fun getAll(): Flow<List<Recipe>>
    @Query("SELECT COUNT(*) FROM Recipe") suspend fun count(): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<Recipe>)
    @Query("DELETE FROM Recipe") suspend fun clearAll()
    
    // Sync methods for transactions
    @Query("DELETE FROM Recipe") fun clearAllSync()
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertAllSync(list: List<Recipe>)
}

@Dao interface ProductDao {
    @Query("SELECT * FROM Product ORDER BY name") fun getAll(): Flow<List<Product>>
    @Query("SELECT COUNT(*) FROM Product") suspend fun count(): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<Product>)
    @Query("DELETE FROM Product") suspend fun clearAll()
    
    // Sync methods for transactions
    @Query("DELETE FROM Product") fun clearAllSync()
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertAllSync(list: List<Product>)
}

@Dao interface MealDao {
    @Query("SELECT * FROM MealEntry WHERE dateEpochDay=:d") fun mealsOn(d:Long): Flow<List<MealEntry>>
    @Query("SELECT * FROM MealEntry WHERE dateEpochDay = :date") fun getMealsForDate(date: Long): Flow<List<MealEntry>>
    @Query("SELECT * FROM MealEntry WHERE dateEpochDay BETWEEN :start AND :end") fun getMealsInRange(start: Long, end: Long): Flow<List<MealEntry>>
    @Query("SELECT * FROM MealEntry ORDER BY dateEpochDay DESC") fun getAll(): Flow<List<MealEntry>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(m:MealEntry): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<MealEntry>)
    @Delete suspend fun delete(m:MealEntry)
    @Query("DELETE FROM MealEntry") suspend fun clearAll()
    
    // Sync methods for transactions
    @Query("DELETE FROM MealEntry") fun clearAllSync()
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertAllSync(list: List<MealEntry>)
}

@Dao interface UserDao {
    @Query("SELECT * FROM UserProfile WHERE id='default_user'") fun profile(): Flow<UserProfile?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(p:UserProfile)
    @Query("DELETE FROM UserProfile") suspend fun clearAll()
    
    // Sync methods for transactions
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun saveSync(p:UserProfile)
    @Query("DELETE FROM UserProfile") fun clearAllSync()
}

@Dao interface WeightDao {
    @Query("SELECT * FROM WeightEntry ORDER BY dateEpochDay DESC") fun getAll(): Flow<List<WeightEntry>>
    @Upsert suspend fun save(entry: WeightEntry)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<WeightEntry>)
    @Query("DELETE FROM WeightEntry") suspend fun clearAll()
    
    // Sync methods for transactions
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertAllSync(list: List<WeightEntry>)
    @Query("DELETE FROM WeightEntry") fun clearAllSync()
}

@Dao interface WorkoutDao {
    @Query("SELECT * FROM WorkoutEntry WHERE dateEpochDay = :date")
    fun getForDay(date: Long): Flow<List<WorkoutEntry>>
    @Query("SELECT * FROM WorkoutEntry WHERE dateEpochDay = :date")
    fun getWorkoutsForDate(date: Long): Flow<List<WorkoutEntry>>
    @Query("SELECT * FROM WorkoutEntry WHERE dateEpochDay BETWEEN :start AND :end")
    fun getWorkoutsInRange(start: Long, end: Long): Flow<List<WorkoutEntry>>
    @Query("SELECT COUNT(*) FROM WorkoutEntry WHERE dateEpochDay >= :since")
    suspend fun countWorkoutsSince(since: Long): Int
    @Query("SELECT * FROM WorkoutEntry")
    fun getAllWorkouts(): Flow<List<WorkoutEntry>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entry: WorkoutEntry)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAllWorkouts(list: List<WorkoutEntry>)
    
    @Transaction @Query("SELECT * FROM ExerciseLog ORDER BY dateEpochDay DESC")
    fun getAllWithSets(): Flow<List<ExerciseWithSets>>
    
    @Transaction @Query("SELECT * FROM ExerciseLog WHERE exerciseName = :name ORDER BY dateEpochDay DESC")
    fun getHistoryForExercise(name: String): Flow<List<ExerciseWithSets>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLog(log: ExerciseLog): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLogs(logs: List<ExerciseLog>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSets(sets: List<SetLog>)
    
    @Delete suspend fun deleteLog(log: ExerciseLog)
    @Query("DELETE FROM ExerciseLog WHERE dateEpochDay = :date AND workoutType = :type")
    suspend fun deleteLogsForDay(date: Long, type: String)
    
    @Query("DELETE FROM ExerciseLog") suspend fun clearAllLogs()
    @Query("DELETE FROM SetLog") suspend fun clearAllSets()
    @Query("DELETE FROM WorkoutEntry") suspend fun clearAllWorkouts()
    
    // Sync methods for transactions
    @Query("DELETE FROM ExerciseLog") fun clearAllLogsSync()
    @Query("DELETE FROM SetLog") fun clearAllSetsSync()
    @Query("DELETE FROM WorkoutEntry") fun clearAllWorkoutsSync()
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertAllWorkoutsSync(list: List<WorkoutEntry>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertLogsSync(list: List<ExerciseLog>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertSetsSync(list: List<SetLog>)
}

@Dao interface ManualMealDao {
    @Query("SELECT * FROM ManualMealEntry WHERE dateEpochDay = :date")
    fun getForDay(date: Long): Flow<List<ManualMealEntry>>
    @Query("SELECT * FROM ManualMealEntry ORDER BY dateEpochDay DESC")
    fun getAll(): Flow<List<ManualMealEntry>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entry: ManualMealEntry)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<ManualMealEntry>)
    @Delete suspend fun delete(entry: ManualMealEntry)
    @Query("DELETE FROM ManualMealEntry") suspend fun clearAll()
    
    // Sync methods for transactions
    @Query("DELETE FROM ManualMealEntry") fun clearAllSync()
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertAllSync(list: List<ManualMealEntry>)
}

@Dao interface ScannedFoodDao {
    @Query("SELECT * FROM ScannedFoodEntity WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): ScannedFoodEntity?

    @Query("SELECT * FROM ScannedFoodEntity WHERE code = :code LIMIT 1")
    fun observeByCode(code: String): Flow<ScannedFoodEntity?>

    @Query("SELECT * FROM ScannedFoodEntity ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<ScannedFoodEntity>>

    @Query("SELECT * FROM ScannedFoodEntity WHERE status IN ('PENDING','ERROR') ORDER BY updatedAt DESC")
    fun observePendingOrError(): Flow<List<ScannedFoodEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: ScannedFoodEntity): Long

    @Query("""UPDATE ScannedFoodEntity SET 
        status = :status, name = :name, brand = :brand, imageUrl = :imageUrl,
        kcal100g = :kcal100g, protein100g = :protein100g, carbs100g = :carbs100g, fat100g = :fat100g,
        nutritionGrade = :nutritionGrade, servingSize = :servingSize, lastError = :lastError,
        updatedAt = :updatedAt
        WHERE code = :code""")
    suspend fun updateByCode(
        code: String, status: String,
        name: String?, brand: String?, imageUrl: String?,
        kcal100g: Double?, protein100g: Double?, carbs100g: Double?, fat100g: Double?,
        nutritionGrade: String?, servingSize: String?, lastError: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE ScannedFoodEntity SET status = :status, lastError = :lastError, updatedAt = :updatedAt WHERE code = :code")
    suspend fun updateStatus(code: String, status: String, lastError: String? = null, updatedAt: Long = System.currentTimeMillis())
}

