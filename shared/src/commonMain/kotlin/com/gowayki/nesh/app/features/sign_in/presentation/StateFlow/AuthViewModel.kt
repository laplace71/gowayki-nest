package com.gowayki.nesh.app.features.sign_in.presentation.StateFlow

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gowayki.nesh.app.features.sign_in.presentation.screens.LoginStatus
import com.gowayki.nesh.app.features.sign_in.presentation.screens.LoginUiState
import com.gowayki.nesh.domain.model.User
import com.gowayki.nesh.domain.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Estado nico general (Mantenido por compatibilidad si otras pantallas lo usan)
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val user: User? = null,
)

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    // Estado general (para Welcome / Register)
    var state by mutableStateOf(AuthUiState())
        private set

    // Estado especifico para el Numpad (Login PIN)
    var loginState by mutableStateOf(LoginUiState())
        private set

    private var blockTimerJob: Job? = null

    // Funciones del Numpad
    fun onNumberClick(number: Int) {
        if (loginState.status == LoginStatus.BLOCKED || state.isLoading) return
        
        val currentPin = loginState.pin
        if (currentPin.length < 6) {
            val newPin = currentPin + number
            loginState = loginState.copy(pin = newPin, status = LoginStatus.NORMAL)
            
            if (newPin.length == 6) {
                verifyPin(newPin)
            }
        }
    }

    fun onDeleteClick() {
        if (loginState.status == LoginStatus.BLOCKED || state.isLoading) return
        
        val currentPin = loginState.pin
        if (currentPin.isNotEmpty()) {
            loginState = loginState.copy(
                pin = currentPin.dropLast(1),
                status = LoginStatus.NORMAL // Resetear error al borrar
            )
        }
    }

    private fun verifyPin(pin: String) {
        state = AuthUiState(isLoading = true) // Mostrar loading si hubiera un overlay general
        
        viewModelScope.launch {
            repo.login(pin)
                .onSuccess { 
                    state = AuthUiState(user = it) 
                    // Exito!
                }
                .onFailure {
                    val newAttempts = loginState.attemptsLeft - 1
                    if (newAttempts <= 0) {
                        startBlockTimer()
                    } else {
                        loginState = loginState.copy(
                            status = LoginStatus.ERROR,
                            attemptsLeft = newAttempts,
                            pin = "" // Opcional: limpiar pin al fallar
                        )
                    }
                    state = AuthUiState(error = it.message ?: "Error inesperado")
                }
        }
    }

    private fun startBlockTimer() {
        loginState = loginState.copy(
            status = LoginStatus.BLOCKED,
            blockTimerSeconds = 30, // 30 segundos segun el Figma
            attemptsLeft = 0,
            pin = ""
        )
        
        blockTimerJob?.cancel()
        blockTimerJob = viewModelScope.launch {
            while (loginState.blockTimerSeconds > 0) {
                delay(1000)
                loginState = loginState.copy(blockTimerSeconds = loginState.blockTimerSeconds - 1)
            }
            // Termino el castigo
            loginState = loginState.copy(
                status = LoginStatus.NORMAL,
                attemptsLeft = 3
            )
        }
    }

    // Compatibilidad antigua
    fun register(name: String, dni: String, phone: String) =
        exec { repo.register(name, dni, phone) }

    fun sendRecoveryCode(dni: String) {
        state = AuthUiState(isLoading = true)
        viewModelScope.launch {
            delay(1500) // Simulate network call
            state = AuthUiState(isLoading = false, error = null)
            // TODO: Navigate to OTP verification screen or handle success
        }
    }

    fun savePin(phone: String, pin: String) =
        exec { repo.savePin(phone, pin) }

    fun google(idToken: String) = exec { repo.google(idToken) }

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