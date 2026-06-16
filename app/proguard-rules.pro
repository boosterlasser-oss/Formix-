# ============================================================
# ProGuard Rules – FantasyNutritionPlanner
# ============================================================

# --- Room Database Entities (Reflection-basiert) ---
-keep class com.fantasyfoodplanner.data.UserProfile { *; }
-keep class com.fantasyfoodplanner.data.WeightEntry { *; }
-keep class com.fantasyfoodplanner.data.WorkoutEntry { *; }
-keep class com.fantasyfoodplanner.data.ExerciseLog { *; }
-keep class com.fantasyfoodplanner.data.SetLog { *; }
-keep class com.fantasyfoodplanner.data.Recipe { *; }
-keep class com.fantasyfoodplanner.data.Product { *; }
-keep class com.fantasyfoodplanner.data.MealEntry { *; }
-keep class com.fantasyfoodplanner.data.ManualMealEntry { *; }
-keep class com.fantasyfoodplanner.data.ExerciseWithSets { *; }
-keep class com.fantasyfoodplanner.data.FoodItem { *; }
-keep class com.fantasyfoodplanner.data.ScannedFoodEntity { *; }

# --- Room DAOs ---
-keep class com.fantasyfoodplanner.data.AppDb { *; }
-keep interface com.fantasyfoodplanner.data.RecipeDao { *; }
-keep interface com.fantasyfoodplanner.data.ProductDao { *; }
-keep interface com.fantasyfoodplanner.data.MealDao { *; }
-keep interface com.fantasyfoodplanner.data.UserDao { *; }
-keep interface com.fantasyfoodplanner.data.WeightDao { *; }
-keep interface com.fantasyfoodplanner.data.WorkoutDao { *; }
-keep interface com.fantasyfoodplanner.data.ManualMealDao { *; }
-keep interface com.fantasyfoodplanner.data.ScannedFoodDao { *; }

# --- Gson Serialisierung (SharedPreferences JSON) ---
-keep class com.fantasyfoodplanner.logic.LearningProfile { *; }
-keep class com.fantasyfoodplanner.logic.CheckInLog { *; }
-keep class com.fantasyfoodplanner.features.fitness.WorkoutPlan { *; }
-keep class com.fantasyfoodplanner.features.fitness.WorkoutBlock { *; }
-keep class com.fantasyfoodplanner.features.fitness.FitnessProfile { *; }
-keep class com.fantasyfoodplanner.features.fitness.DailyModifier { *; }
-keep class com.fantasyfoodplanner.ui.SetState { *; }
-keep class com.fantasyfoodplanner.logic.BackupPayload { *; }
-keep class com.fantasyfoodplanner.logic.BackupContainer { *; }

# Gson TypeToken (generics)
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# --- Room generierter Code ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# --- Lottie ---
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

# --- SceneView ---
-dontwarn io.github.sceneview.**
-keep class io.github.sceneview.** { *; }
-dontwarn com.google.android.filament.**
-keep class com.google.android.filament.** { *; }

# --- ML Kit ---
-dontwarn com.google.mlkit.**
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.android.gms.internal.**

# --- Retrofit + OkHttp ---
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**

# --- Open Food Facts DTOs (Gson) ---
-keep class com.fantasyfoodplanner.data.remote.** { *; }

# --- CoreAI / NativeLlama JNI ---
-keep class com.fantasyfoodplanner.ai.NativeLlama { *; }
-keep class com.fantasyfoodplanner.ai.ChatMessage { *; }
-keep class com.fantasyfoodplanner.ai.CoachState { *; }

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# --- Jetpack Compose (keep Composable metadata) ---
-dontwarn androidx.compose.**

# --- Enums (werden per name() serialisiert) ---
-keepclassmembers enum * { *; }

# --- Standard Android ---
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
