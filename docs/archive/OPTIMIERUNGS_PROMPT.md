# FORMIX Optimierungs-Prompt für OpenCode

## Projekt-Kontext

Du arbeitest am FORMIX Android-Projekt (Fitness & Nutrition App) in Kotlin mit Jetpack Compose.

**Projekt-Pfad:** `D:\Entwicklung\Android\FORMIX`  
**App-Package:** `com.fantasyfoodplanner.fix.v4.debug`  
**Aktuelle Version:** 3.0.0 (Build 16)  
**Datenbank-Version:** 7 (Room Database)

## Projekt-Bewertung (Stand: 07.04.2026)

- **Gesamt:** B+ (83/100)
- **Architektur:** B+ (84/100) - Sehr gut
- **KI-Integration:** A (90/100) - Herausragend
- **Datenbank:** C+ (70/100) - **BRAUCHT OPTIMIERUNG**
- **UI/UX:** B+ (85/100) - Sehr gut

## 🚨 KRITISCHE PROBLEME (Vor Launch beheben)

### Problem 1: Fehlende Datenbank-Indizes (HÖCHSTE PRIORITÄT)

**Dateien:** `app/src/main/java/com/fantasyfoodplanner/data/Entities.kt`

**Problem:**
- Aktuell nur 1 Index auf ScannedFoodEntity.code
- Keine Indizes auf häufig abgefragten Spalten (dateEpochDay, exerciseName, etc.)
- Performance-Problem bei 10.000+ Einträgen (100-1000x langsamer)

**Zu ergänzende Indizes:**

```kotlin
// WeightEntry
@Entity(indices = [Index(value = ["dateEpochDay"])])
data class WeightEntry(...)

// WorkoutEntry  
@Entity(indices = [
    Index(value = ["dateEpochDay"]),
    Index(value = ["type"])
])
data class WorkoutEntry(...)

// ExerciseLog
@Entity(indices = [
    Index(value = ["dateEpochDay"]),
    Index(value = ["exerciseName"]),
    Index(value = ["workoutType", "dateEpochDay"])
])
data class ExerciseLog(...)

// SetLog
@Entity(
    indices = [Index(value = ["exerciseLogId"])],
    foreignKeys = [ForeignKey(
        entity = ExerciseLog::class,
        parentColumns = ["id"],
        childColumns = ["exerciseLogId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class SetLog(...)

// MealEntry
@Entity(indices = [Index(value = ["dateEpochDay"])])
data class MealEntry(...)

// ManualMealEntry
@Entity(indices = [Index(value = ["dateEpochDay"])])
data class ManualMealEntry(...)

// Recipe
@Entity(indices = [Index(value = ["name"])])
data class Recipe(...)

// Product
@Entity(indices = [Index(value = ["name"])])
data class Product(...)

// ScannedFoodEntity (bereits vorhanden, beibehalten)
@Entity(indices = [Index(value = ["code"], unique = true)])
data class ScannedFoodEntity(...)
```

**Danach: MIGRATION_7_8 erstellen**

**Datei:** `app/src/main/java/com/fantasyfoodplanner/data/AppDb.kt`

```kotlin
// Nach MIGRATION_6_7 hinzufügen:
private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Indizes für Performance-Optimierung
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

// Im AppDb.get() Builder:
@Database(
    entities = [...],
    version = 8,  // ⬆️ Version erhöhen!
    exportSchema = true
)
abstract class AppDb : RoomDatabase() {
    companion object {
        fun get(ctx: Context): AppDb = I ?: synchronized(this) {
            I ?: Room.databaseBuilder(...)
                .addMigrations(MIGRATION_6_7, MIGRATION_7_8)  // ⬅️ MIGRATION_7_8 hinzufügen
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                .also { I = it }
        }
    }
}
```

**Test nach Migration:**
1. App deinstallieren NICHT (Migration testen!)
2. App starten und prüfen ob Migration erfolgreich
3. Logcat nach "MIGRATION_7_8" durchsuchen
4. Datenbank prüfen: `adb shell run-as com.fantasyfoodplanner.fix.v4.debug sqlite3 /data/data/com.fantasyfoodplanner.fix.v4.debug/databases/app_database.db ".indexes"`

---

### Problem 2: Ineffiziente Abfrage-Muster

**Dateien:** 
- `app/src/main/java/com/fantasyfoodplanner/data/Dao.kt`
- ViewModels die `getAll().first().filter { }` verwenden

**Problem:**
- ViewModels laden ALLE Daten und filtern in Kotlin
- Beispiel: `mealDao.getAll().first()` lädt 10.000 Meals, nur um 7 zu brauchen

**Neue Queries hinzufügen zu Dao.kt:**

```kotlin
@Dao
interface MealDao {
    // ✅ Bestehende Queries beibehalten
    @Query("SELECT * FROM MealEntry ORDER BY dateEpochDay DESC")
    fun getAll(): Flow<List<MealEntry>>
    
    // ⭐ NEU: Date-Range-Queries
    @Query("SELECT * FROM MealEntry WHERE dateEpochDay = :date")
    fun getMealsForDate(date: Long): Flow<List<MealEntry>>
    
    @Query("SELECT * FROM MealEntry WHERE dateEpochDay BETWEEN :startDate AND :endDate ORDER BY dateEpochDay DESC")
    fun getMealsInRange(startDate: Long, endDate: Long): Flow<List<MealEntry>>
    
    // ⭐ NEU: Aggregations-Queries
    @Query("""
        SELECT dateEpochDay, COUNT(*) as count 
        FROM MealEntry 
        WHERE dateEpochDay BETWEEN :startDate AND :endDate
        GROUP BY dateEpochDay
    """)
    fun getMealCountsByDateRange(startDate: Long, endDate: Long): Flow<List<DayCount>>
}

@Dao
interface ManualMealDao {
    @Query("SELECT * FROM ManualMealEntry WHERE dateEpochDay = :date")
    fun getManualMealsForDate(date: Long): Flow<List<ManualMealEntry>>
    
    @Query("SELECT * FROM ManualMealEntry WHERE dateEpochDay BETWEEN :startDate AND :endDate")
    fun getManualMealsInRange(startDate: Long, endDate: Long): Flow<List<ManualMealEntry>>
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM WorkoutEntry WHERE dateEpochDay = :date")
    fun getWorkoutsForDate(date: Long): Flow<List<WorkoutEntry>>
    
    @Query("SELECT * FROM WorkoutEntry WHERE dateEpochDay BETWEEN :startDate AND :endDate ORDER BY dateEpochDay DESC")
    fun getWorkoutsInRange(startDate: Long, endDate: Long): Flow<List<WorkoutEntry>>
    
    @Query("SELECT COUNT(*) FROM WorkoutEntry WHERE dateEpochDay BETWEEN :startDate AND :endDate")
    suspend fun getWorkoutCountInRange(startDate: Long, endDate: Long): Int
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM ExerciseLog WHERE dateEpochDay = :date")
    fun getExercisesForDate(date: Long): Flow<List<ExerciseLog>>
    
    @Query("SELECT * FROM ExerciseLog WHERE exerciseName = :name ORDER BY dateEpochDay DESC LIMIT :limit")
    fun getExerciseHistory(name: String, limit: Int = 20): Flow<List<ExerciseLog>>
}

// ⭐ NEU: Data Classes für Aggregations
data class DayCount(
    val dateEpochDay: Long,
    val count: Int
)
```

**Danach: ViewModels aktualisieren**
- `DashboardViewModel.kt` - statt `getAll().filter { }` verwende `getMealsInRange()`
- Andere ViewModels entsprechend anpassen

---

### Problem 3: Accessibility Compliance (Google Play Anforderung)

**Dateien:** `app/src/main/java/com/fantasyfoodplanner/ui/FantasyKit.kt`

**Problem:**
- Keine semantics-Modifier auf Buttons/klickbaren Elementen
- Keine contentDescription auf Icons
- Touch-Targets teilweise < 48dp

**Fixes:**

```kotlin
// FantasyButton - Semantics hinzufügen
@Composable
fun FantasyButton(
    label: String,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)  // ⭐ Mindestgröße
            .clickable(enabled = enabled) { 
                if (enabled) onClick() 
            }
            .semantics {  // ⭐ NEU: Accessibility
                role = Role.Button
                contentDescription = label
                this.enabled = enabled
            }
            .background(
                color = FantasyColors.ButtonBg.copy(alpha = if (enabled) alpha else 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        FText(
            text = label,
            sizeSp = 16,
            bold = true,
            color = FantasyColors.ButtonText.copy(alpha = if (enabled) 1f else 0.6f)
        )
    }
}

// Icons mit contentDescription
Icon(
    imageVector = Icons.Default.ArrowBack,
    contentDescription = "Zurück",  // ⭐ NEU
    modifier = Modifier
        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)  // ⭐ Touch-Target
        .clickable { onBack() }
)

// Alle klickbaren Elemente
Box(
    modifier = Modifier
        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)  // ⭐ Mindestgröße
        .clickable { onClick() }
        .semantics {  // ⭐ NEU
            role = Role.Button
            contentDescription = "Beschreibung"
        }
)
```

**Dateien zu aktualisieren:**
1. `FantasyKit.kt` - Alle Komponenten (FantasyButton, FantasyCard mit onClick, etc.)
2. `Dashboard.kt` - Icon-Buttons, klickbare Cards
3. `CoachChatSheet.kt` - Send-Button, Quick-Chips
4. `WorkoutStep.kt` - Exercise-Cards, Timer-Controls
5. `Profile.kt` - Settings-Buttons

**Test mit TalkBack:**
1. Aktiviere TalkBack: Einstellungen → Bedienungshilfen → TalkBack
2. Navigiere durch die App
3. Prüfe ob alle Elemente vorgelesen werden
4. Prüfe ob Touch-Targets groß genug sind

---

### Problem 4: API-Key-Verschlüsselung

**Datei:** `app/src/main/java/com/fantasyfoodplanner/logic/SettingsManager.kt`

**Problem:**
- API-Keys in SharedPreferences als Klartext gespeichert
- Zugriff mit root/ADB möglich

**Lösung: EncryptedSharedPreferences verwenden**

```kotlin
// build.gradle.kts - Dependency hinzufügen
dependencies {
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}

// SettingsManager.kt - Refactoring
object SettingsManager {
    private const val ENCRYPTED_PREFS_NAME = "formix_secure_prefs"
    
    private fun getEncryptedPrefs(ctx: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        return EncryptedSharedPreferences.create(
            ctx,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    // API-Key speichern (verschlüsselt)
    fun setAiApiKey(ctx: Context, key: String) {
        getEncryptedPrefs(ctx).edit().putString("ai_api_key", key).apply()
    }
    
    // API-Key abrufen (entschlüsselt)
    fun getAiApiKey(ctx: Context): String {
        val encryptedKey = getEncryptedPrefs(ctx).getString("ai_api_key", null)
        if (encryptedKey.isNullOrBlank()) {
            return getBuiltInKey()  // Fallback auf BuildConfig
        }
        return encryptedKey
    }
    
    // Migration von alten Klartext-Keys
    fun migrateOldApiKey(ctx: Context) {
        val oldPrefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val oldKey = oldPrefs.getString("ai_api_key", null)
        
        if (!oldKey.isNullOrBlank()) {
            // In verschlüsselte Prefs übertragen
            setAiApiKey(ctx, oldKey)
            // Alte Klartext-Version löschen
            oldPrefs.edit().remove("ai_api_key").apply()
            Log.i("SettingsManager", "API-Key erfolgreich migriert zu EncryptedSharedPreferences")
        }
    }
    
    // Andere Settings können in normalen Prefs bleiben
    private fun getNormalPrefs(ctx: Context): SharedPreferences {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
```

**Migration beim App-Start in MainActivity:**

```kotlin
// MainActivity.onCreate()
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // API-Key Migration (nur einmal)
    SettingsManager.migrateOldApiKey(this)
    
    // Rest des Codes...
}
```

---

## ⚠️ HOHE PRIORITÄT (Woche 1 nach Launch)

### Problem 5: Fehlende Error-Handling-UI

**Problem:** Fehler werden nur geloggt, User sieht nichts

**Lösung: ErrorCard Komponente erstellen**

**Datei:** `app/src/main/java/com/fantasyfoodplanner/ui/FantasyKit.kt`

```kotlin
@Composable
fun ErrorCard(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    FantasyCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = Color(0xFFFF6B6B).copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Error Icon
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Fehler",
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(Modifier.width(12.dp))
            
            // Error Message
            Column(modifier = Modifier.weight(1f)) {
                FText(
                    text = "Fehler",
                    sizeSp = 14,
                    bold = true,
                    color = Color(0xFFFF6B6B)
                )
                Spacer(Modifier.height(4.dp))
                FText(
                    text = message,
                    sizeSp = 12,
                    color = FantasyColors.Text
                )
            }
            
            // Action Buttons
            Column {
                onRetry?.let {
                    IconButton(onClick = it) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Erneut versuchen",
                            tint = Color(0xFFFF6B6B)
                        )
                    }
                }
                onDismiss?.let {
                    IconButton(onClick = it) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Schließen",
                            tint = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
```

**ViewModels aktualisieren:**

```kotlin
// DashboardViewModel.kt
data class DashboardState(
    val userProfile: UserProfile? = null,
    val todayKcal: Int = 0,
    // ... andere Felder
    val error: String? = null,  // ⭐ NEU
    val isLoading: Boolean = false  // ⭐ NEU
)

class DashboardViewModel(...) : ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state
    
    fun refresh() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                
                // Daten laden
                val profile = userRepo.getProfile()
                // ... weitere Daten
                
                _state.value = DashboardState(
                    userProfile = profile,
                    // ... andere Daten
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                Log.e("DashboardVM", "Fehler beim Laden", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Daten konnten nicht geladen werden: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
```

**UI aktualisieren:**

```kotlin
// Dashboard.kt
@Composable
fun DashboardScreen(...) {
    val dashState by vm.state.collectAsState()
    
    Column {
        // Error anzeigen
        dashState.error?.let { errorMsg ->
            ErrorCard(
                message = errorMsg,
                onRetry = { vm.refresh() },
                onDismiss = { vm.clearError() }
            )
            Spacer(Modifier.height(12.dp))
        }
        
        // Loading-State
        if (dashState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        
        // Normaler Content
        // ...
    }
}
```

---

## 📋 MITTLERE PRIORITÄT (Sprint 2-3)

### Problem 6: Kein Typography-System

**Datei:** `app/src/main/java/com/fantasyfoodplanner/ui/FantasyKit.kt`

```kotlin
// ⭐ NEU: Typography-Scale definieren
object FantasyTypography {
    val displayLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp)
    val headlineLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
    val headlineMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
    val bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp)
    val bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp)
    val bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp)
    val labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
    val labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp)
}

// FText refactorn
@Composable
fun FText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = FantasyTypography.bodyMedium,  // ⭐ NEU: Style statt sizeSp
    color: Color = FantasyColors.Text,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow
    )
}

// Alte FText mit sizeSp als @Deprecated markieren
@Deprecated("Verwende style-Parameter statt sizeSp", ReplaceWith("FText(text, modifier, FantasyTypography.bodyMedium, color)"))
@Composable
fun FText(
    text: String,
    sizeSp: Int,
    modifier: Modifier = Modifier,
    bold: Boolean = false,
    color: Color = FantasyColors.Text,
    textAlign: TextAlign? = null
) {
    val style = when(sizeSp) {
        32 -> FantasyTypography.displayLarge
        24 -> FantasyTypography.headlineLarge
        20 -> FantasyTypography.headlineMedium
        16 -> FantasyTypography.bodyLarge
        14 -> FantasyTypography.bodyMedium
        12 -> FantasyTypography.bodySmall
        11 -> FantasyTypography.labelSmall
        else -> FantasyTypography.bodyMedium.copy(fontSize = sizeSp.sp)
    }.let { if (bold) it.copy(fontWeight = FontWeight.Bold) else it }
    
    FText(text, modifier, style, color, textAlign)
}
```

**Schrittweise Migration:**
1. Neue FText mit style-Parameter funktioniert parallel
2. Alte Aufrufe funktionieren weiter (deprecated)
3. Nach und nach umstellen auf neue Syntax
4. Später alte Funktion entfernen

---

### Problem 7: Spacing-System fehlt

```kotlin
// FantasyKit.kt
object FantasySpacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

// Verwendung:
Spacer(Modifier.height(FantasySpacing.m))
padding(FantasySpacing.l)
```

---

## 🧪 TESTING-CHECKLISTE

Nach jeder Änderung testen:

### Database Migration Test
```bash
# App NICHT deinstallieren (sonst keine Migration)
# App starten
adb logcat | grep -i migration

# Datenbank-Indizes prüfen
adb shell
run-as com.fantasyfoodplanner.fix.v4.debug
sqlite3 /data/data/com.fantasyfoodplanner.fix.v4.debug/databases/app_database.db
.indexes
.exit
```

### Performance Test
```kotlin
// Vor und nach den Optimierungen messen
val startTime = System.currentTimeMillis()
val meals = mealDao.getMealsForDate(today).first()
val duration = System.currentTimeMillis() - startTime
Log.d("Performance", "Query dauerte ${duration}ms")
```

### Accessibility Test
1. TalkBack aktivieren
2. Durch App navigieren
3. Prüfen ob alles vorgelesen wird
4. Touch-Targets testen

### Encryption Test
```kotlin
// API-Key speichern und abrufen
SettingsManager.setAiApiKey(context, "test_key_12345")
val retrieved = SettingsManager.getAiApiKey(context)
assert(retrieved == "test_key_12345")

// Prüfen ob in Klartext sichtbar
adb shell
run-as com.fantasyfoodplanner.fix.v4.debug
cat shared_prefs/formix_secure_prefs.xml
# Sollte verschlüsselt aussehen (nicht lesbar)
```

---

## 📦 BUILD & DEPLOYMENT

Nach allen Änderungen:

```bash
# Clean Build
cd D:\Entwicklung\Android\FORMIX
gradlew clean

# Debug Build
gradlew assembleDebug

# APK installieren
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Logcat überwachen
adb logcat -s "FORMIX:*" "*:E"
```

---

## 🎯 PRIORITÄTEN-REIHENFOLGE

**Tag 1 (2-3 Stunden):**
1. ✅ Datenbank-Indizes hinzufügen (Entities.kt)
2. ✅ MIGRATION_7_8 erstellen (AppDb.kt)
3. ✅ Testen: Migration erfolgreich?

**Tag 2 (3-4 Stunden):**
4. ✅ Date-Range-Queries hinzufügen (Dao.kt)
5. ✅ ViewModels aktualisieren (DashboardViewModel.kt, etc.)
6. ✅ Performance-Tests durchführen

**Tag 3 (2-3 Stunden):**
7. ✅ Accessibility zu FantasyKit (semantics, touch-targets)
8. ✅ TalkBack-Tests durchführen

**Tag 4 (2 Stunden):**
9. ✅ API-Key-Verschlüsselung (SettingsManager.kt)
10. ✅ Migration testen

**Tag 5 (3-4 Stunden):**
11. ✅ ErrorCard Komponente erstellen
12. ✅ Error-States zu ViewModels
13. ✅ UI aktualisieren mit Error-Handling

**Sprint 2:**
- Typography-System
- Spacing-System
- Unit-Tests schreiben

---

## ⚠️ WICHTIGE HINWEISE

**Backup vorhanden:**
- `D:\Backups\FORMIX_Backup_20260407_1638`
- Bei Problemen von dort wiederherstellen

**Nicht vergessen:**
- Version in build.gradle.kts erhöhen (3.0.0 → 3.1.0)
- Database-Version erhöhen (7 → 8)
- CHANGELOG.md aktualisieren
- Git-Commit nach jeder größeren Änderung

**Keine Breaking Changes:**
- Alte Datenbank-Daten müssen erhalten bleiben (Migration!)
- User-Einstellungen nicht zurücksetzen
- App sollte auch mit alten Daten funktionieren

---

## 📊 ERFOLGS-METRIKEN

Nach Optimierungen sollten folgende Werte erreicht werden:

- **Query-Performance:** < 50ms für date-range-queries (aktuell: 500-5000ms)
- **App-Start:** < 3s bis Dashboard sichtbar (aktuell: OK)
- **Accessibility-Score:** 100/100 (aktuell: ~40/100)
- **Gesamt-Bewertung:** A- (90+/100) (aktuell: B+ 83/100)

---

## 🚀 NACH ALLEN FIXES

Neue Bewertung durchführen:
- ✅ Datenbank: A- (90/100) - Mit Indizes optimiert
- ✅ Accessibility: A (95/100) - Google Play konform
- ✅ Security: B+ (85/100) - Verschlüsselte API-Keys
- ✅ UX: A- (90/100) - Error-Handling vorhanden

**Ziel erreicht: A- (90/100) Gesamt-Bewertung**

---

## ENDE DES PROMPTS

Arbeite diese Optimierungen systematisch ab. Nach jeder Änderung: Testen, Committen, Dokumentieren.
