package com.fantasyfoodplanner.data.repository

import com.fantasyfoodplanner.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Repository für Benutzerprofil und Gewichtsdaten.
 * Kapselt den Datenbankzugriff und stellt eine saubere API bereit.
 */
class UserRepository(private val db: AppDb) {

    fun profileFlow(): Flow<UserProfile?> = db.userDao().profile()

    suspend fun getProfile(): UserProfile? = withContext(Dispatchers.IO) {
        db.userDao().profile().first()
    }

    suspend fun saveProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        db.userDao().save(profile)
    }

    fun weightEntriesFlow(): Flow<List<WeightEntry>> = db.weightDao().getAll()

    suspend fun saveWeight(entry: WeightEntry) = withContext(Dispatchers.IO) {
        db.weightDao().save(entry)
    }
}

