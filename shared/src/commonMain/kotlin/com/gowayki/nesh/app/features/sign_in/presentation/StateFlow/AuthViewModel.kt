package com.gowayki.nesh.app.features.sign_in.presentation.StateFlow

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gowayki.nesh.domain.model.User
import com.gowayki.nesh.domain.repository.AuthRepository
import kotlinx.coroutines.launch

// Estado único de auth que observan las pantallas.
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val user: User? = null,
)

// Toda la lógica de login/registro/PIN vive aquí, no en los composables.
// Las pantallas solo muestran state y reenvían eventos.
class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    var state by mutableStateOf(AuthUiState())
        private set

    fun login(pin: String) = exec { repo.login(pin) }

    fun register(name: String, dni: String, phone: String) =
        exec { repo.register(name, dni, phone) }

    fun savePin(phone: String, pin: String) =
        exec { repo.savePin(phone, pin) }

    fun google(idToken: String) = exec { repo.google(idToken) }

    // Se llama tras navegar para no re-navegar al recomponer.
    fun consumeUser() {
        state = state.copy(user = null)
    }

    private fun exec(block: suspend () -> Result<User>) {
        state = AuthUiState(isLoading = true)
        viewModelScope.launch {
            block()
                .onSuccess { state = AuthUiState(user = it) }
                .onFailure { state = AuthUiState(error = it.message ?: "Error inesperado") }
        }
    }
}