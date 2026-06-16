package com.fantasyfoodplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CoachMessage(
    val id: String,
    val role: String,
    val text: String
)

data class CoachUiState(
    val messages: List<CoachMessage> = listOf(
        CoachMessage(id = "welcome", role = "assistant", text = "Hallo! Ich bin dein Fitness-Coach. Aktuell ist der KI-Coach deaktiviert. Du kannst mich über das Upgrade-Menü wieder freischalten.")
    ),
    val isLoading: Boolean = false
)

class CoachViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(CoachUiState())
    val state: StateFlow<CoachUiState> = _state.asStateFlow()

    fun ensureInitialized() {
        // No-op: Coach is ready without AI
    }

    fun sendMessage(text: String) {
        val msg = CoachMessage(
            id = "user_${System.currentTimeMillis()}",
            role = "user",
            text = text
        )
        _state.value = _state.value.copy(
            messages = _state.value.messages + msg,
            isLoading = true
        )
        viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            val response = CoachMessage(
                id = "resp_${System.currentTimeMillis()}",
                role = "assistant",
                text = "Der KI-Coach ist nicht verfügbar. Bitte aktiviere ihn im Upgrade-Menü, um persönliche Beratung zu erhalten."
            )
            _state.value = _state.value.copy(
                messages = _state.value.messages + response,
                isLoading = false
            )
        }
    }

    fun quickAddFood() = sendMessage("Ich möchte eine Mahlzeit erfassen")
    fun quickLogTraining() = sendMessage("Ich möchte mein Training loggen")
    fun quickDailySummary() = sendMessage("Zeig mir meine heutige Zusammenfassung")
    fun quickHelp() = sendMessage("Hilfe")

    fun clearHistory() {
        _state.value = CoachUiState()
    }

    override fun onCleared() {
        super.onCleared()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CoachViewModel(application) as T
        }
    }
}
