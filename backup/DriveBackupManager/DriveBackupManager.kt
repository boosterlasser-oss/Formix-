package com.fantasyfoodplanner.logic

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.fantasyfoodplanner.data.AppDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * DriveBackupManager – Cloud Backup & Sync fuer PRO-Nutzer via Google Drive.
 *
 * Strategie:
 * - Backup wird als "formix_cloud_backup.json" im versteckten App-Ordner gespeichert
 *   (Scope: DRIVE_APPDATA = nicht im normalen Drive sichtbar, datenschutzkonform)
 * - Upload: Backup-JSON in-memory erstellen -> Drive hochladen (ueberschreibt vorherige Datei)
 * - Download: Drive-Datei herunterladen -> direkt in Datenbank importieren
 *
 * Auth:
 * - Google Sign-In mit DRIVE_APPDATA Scope
 * - getSignInIntent() fuer Activity-Start des Sign-In-Flows
 * - isSignedIn() prueft ob Google-Konto bereits verbunden
 */
object DriveBackupManager {

    private const val BACKUP_FILENAME = "formix_cloud_backup.json"
    private const val APP_NAME = "FORMIX"

    // ──────────────────────────────────────────────────────────
    // Google Sign-In
    // ──────────────────────────────────────────────────────────

    fun getSignInClient(ctx: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        return GoogleSignIn.getClient(ctx, gso)
    }

    fun getSignInIntent(ctx: Context): Intent = getSignInClient(ctx).signInIntent

    fun isSignedIn(ctx: Context): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(ctx) ?: return false
        return account.grantedScopes.any { it.scopeUri == DriveScopes.DRIVE_APPDATA }
    }

    fun getSignedInEmail(ctx: Context): String? =
        GoogleSignIn.getLastSignedInAccount(ctx)?.email

    suspend fun signOut(ctx: Context) = withContext(Dispatchers.IO) {
        try { getSignInClient(ctx).signOut() } catch (_: Exception) {}
    }

    // ──────────────────────────────────────────────────────────
    // Drive-Service aufbauen
    // ──────────────────────────────────────────────────────────

    private fun buildDriveService(ctx: Context, account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            ctx, listOf(DriveScopes.DRIVE_APPDATA)
        ).apply { selectedAccount = account.account }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName(APP_NAME).build()
    }

    // ──────────────────────────────────────────────────────────
    // Upload (Backup in Cloud speichern)
    // ──────────────────────────────────────────────────────────

    /**
     * Erstellt Backup-JSON in-memory und laedt es in den Google Drive App-Ordner hoch.
     */
    suspend fun uploadBackup(ctx: Context, db: AppDb): Result<String> = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(ctx)
                ?: return@withContext Result.failure(
                    Exception("Nicht angemeldet. Bitte zuerst mit Google anmelden.")
                )

            val json = buildBackupJson(ctx, db)
                ?: return@withContext Result.failure(
                    Exception("Backup-Daten konnten nicht erstellt werden.")
                )

            val drive = buildDriveService(ctx, account)
            val existingId = findBackupFileId(drive)
            val content = ByteArrayContent("application/json", json.toByteArray(Charsets.UTF_8))

            if (existingId != null) {
                drive.files().update(existingId, null, content).execute()
            } else {
                val metadata = File().apply {
                    name = BACKUP_FILENAME
                    parents = listOf("appDataFolder")
                }
                drive.files().create(metadata, content).setFields("id").execute()
            }

            val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            Result.success("Cloud-Backup erstellt: $ts")

        } catch (e: Exception) {
            Result.failure(Exception("Upload fehlgeschlagen: ${e.message}"))
        }
    }

    // ──────────────────────────────────────────────────────────
    // Download (Cloud-Backup wiederherstellen)
    // ──────────────────────────────────────────────────────────

    /**
     * Laedt Cloud-Backup herunter und importiert es direkt in die Datenbank.
     */
    suspend fun downloadAndRestoreBackup(ctx: Context, db: AppDb): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(ctx)
                    ?: return@withContext Result.failure(
                        Exception("Nicht angemeldet. Bitte zuerst mit Google anmelden.")
                    )

                val drive = buildDriveService(ctx, account)
                val fileId = findBackupFileId(drive)
                    ?: return@withContext Result.failure(
                        Exception("Kein Cloud-Backup gefunden. Bitte zuerst ein Backup hochladen.")
                    )

                val bytes = drive.files().get(fileId).executeMediaAsInputStream().use { it.readBytes() }
                val json = String(bytes, Charsets.UTF_8)

                importBackupFromJson(ctx, db, json)

            } catch (e: Exception) {
                Result.failure(Exception("Download fehlgeschlagen: ${e.message}"))
            }
        }

    // ──────────────────────────────────────────────────────────
    // Info: Datum des letzten Cloud-Backups
    // ──────────────────────────────────────────────────────────

    suspend fun getLastBackupInfo(ctx: Context): String? = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(ctx) ?: return@withContext null
            val drive = buildDriveService(ctx, account)
            val result = drive.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name, modifiedTime)")
                .setQ("name = '$BACKUP_FILENAME'")
                .execute()
            val file = result.files?.firstOrNull() ?: return@withContext null
            val modified = file.modifiedTime?.value ?: return@withContext null
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
            "Letztes Backup: ${sdf.format(Date(modified))}"
        } catch (_: Exception) {
            null
        }
    }

    // ──────────────────────────────────────────────────────────
    // Interne Hilfsfunktionen
    // ──────────────────────────────────────────────────────────

    private fun findBackupFileId(drive: Drive): String? {
        return try {
            val result = drive.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name)")
                .setQ("name = '$BACKUP_FILENAME'")
                .execute()
            result.files?.firstOrNull()?.id
        } catch (_: Exception) {
            null
        }
    }

    /** Erstellt Backup-JSON in-memory – wiederverwendet BackupPayload aus BackupManager. */
    private suspend fun buildBackupJson(ctx: Context, db: AppDb): String? =
        withContext(Dispatchers.IO) {
            try {
                val gson = GsonBuilder().setPrettyPrinting().create()
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
                gson.toJson(BackupContainer(payload = payload))
            } catch (_: Exception) {
                null
            }
        }

    /** Importiert Backup direkt aus JSON-String (kein Uri erforderlich). */
    private suspend fun importBackupFromJson(ctx: Context, db: AppDb, json: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val gson = GsonBuilder().create()
                val root = gson.fromJson(json, JsonObject::class.java)
                val schemaVer = root.get("schemaVersion")?.asInt ?: 1

                val container = if (schemaVer < 3) {
                    val legacy = gson.fromJson(root, LegacyBackupContainer::class.java)
                    BackupContainer(
                        schemaVersion = 3,
                        payload = BackupPayload(
                            profile = legacy.profile?.copy(id = "default_user"),
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
                    )
                } else {
                    gson.fromJson(json, BackupContainer::class.java)
                }

                db.runInTransaction {
                    db.recipeDao().clearAllSync()
                    db.productDao().clearAllSync()
                    db.mealDao().clearAllSync()
                    db.userDao().clearAllSync()
                    db.weightDao().clearAllSync()
                    db.workoutDao().clearAllWorkoutsSync()
                    db.workoutDao().clearAllLogsSync()
                    db.workoutDao().clearAllSetsSync()
                    db.manualMealDao().clearAllSync()

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

                    p.moduleSelection?.let {
                        try { SettingsManager.setModuleSelection(ctx, ModuleSelection.valueOf(it)) } catch (_: Exception) {}
                    }
                    p.trainingType?.let {
                        try { SettingsManager.setTrainingType(ctx, TrainingType.valueOf(it)) } catch (_: Exception) {}
                    }
                    p.learningProfile?.let { SettingsManager.saveLearningProfile(ctx, it) }
                    p.checkInLogs?.let { logs ->
                        ctx.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                            .edit().putString("checkin_logs", Gson().toJson(logs)).apply()
                    }
                }
                Result.success("Cloud-Backup erfolgreich wiederhergestellt!")
            } catch (e: Exception) {
                Result.failure(Exception("Wiederherstellung fehlgeschlagen: ${e.message}"))
            }
        }
}
