package com.fantasyfoodplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fantasyfoodplanner.data.*
import com.fantasyfoodplanner.data.repository.UserRepository
import com.fantasyfoodplanner.utils.NutrientCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel für den Profil-Screen.
 * Verwaltet Benutzerprofil-Daten und Speicherung.
 */
class ProfileViewModel(
    application: Application,
    private val userRepo: UserRepository
) : AndroidViewModel(application) {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _userProfile.value = userRepo.getProfile()
        }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            // Automatisch TDEE und Protein-Ziel neu berechnen
            val updatedProfile = profile.copy(
                dailyKcalTarget = NutrientCalculator.tdee(profile),
                dailyProteinTarget = NutrientCalculator.targetProtein(profile)
            )
            userRepo.saveProfile(updatedProfile)
            _userProfile.value = updatedProfile
        }
    }

    fun refresh() {
        loadProfile()
    }

    class Factory(
        private val application: Application,
        private val db: AppDb
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(
                application = application,
                userRepo = UserRepository(db)
            ) as T
        }
    }
}

