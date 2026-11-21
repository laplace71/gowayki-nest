package com.gowayki.nesh.app.features.sign_in.presentation.StateFlow

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gowayki.nesh.app.features.sign_in.presentation.screens.LoginStatus
import com.gowayki.nesh.app.features.sign_in.presentation.screens.LoginUiState
import com.gowayki.nesh.core.session.LastDniStore
import com.gowayki.nesh.core.session.UserSession
import com.gowayki.nesh.domain.model.AuthError
import com.gowayki.nesh.domain.model.AuthPolicy
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

    // Persona activa: el login se hace contra este DNI. Se recuerda el último
    // usado para no volver siempre al DNI de prueba.
    var activeDni by mutableStateOf(
        LastDniStore.read()?.takeIf { it.length == 8 } ?: UserSession.activeDni
    )
        private set

    init {
        UserSession.activeDni = activeDni
    }

    /** Cambia de persona y limpia cualquier intento de PIN en curso. */
    fun updateDni(dni: String) {
        val clean = dni.trim()
        if (clean.isBlank()) return
        activeDni = clean
        UserSession.activeDni = clean
        runCatching { LastDniStore.write(clean) }
        loginState = LoginUiState()
    }

    /** Deja el teclado de login limpio (al volver a la pantalla). */
    fun resetLogin() {
        loginState = LoginUiState()
    }

    private var blockTimerJob: Job? = null

    // Funciones del Numpad
    fun onNumberClick(number: Int) {
        if (loginState.status == LoginStatus.BLOCKED || state.isLoading) return
        
        val currentPin = loginState.pin
        if (currentPin.length < 6) {
            val newPin = currentPin + number
            loginState = loginState.copy(pin = newPin, status = LoginStatus.NORMAL, errorMessage = null)
            
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
                status = LoginStatus.NORMAL, // Resetear error al borrar
                errorMessage = null,
            )
        }
    }

    private fun verifyPin(pin: String) {
        state = AuthUiState(isLoading = true) // Mostrar loading si hubiera un overlay general
        
        viewModelScope.launch {
            repo.loginWithDni(activeDni, pin)
                .onSuccess {
                    // Limpia el teclado para el próximo ingreso (p. ej. tras cerrar sesión).
                    loginState = LoginUiState()
                    state = AuthUiState(user = it)
                }
                .onFailure { error ->
                    aplicarFallo(error)
                }
        }
    }

    /**
     * Traduce el fallo del intento a la pantalla.
     *
     * El backend manda: si dice que quedan N intentos, se muestran esos N; si
     * dice que la cuenta está bloqueada, se bloquea 15 minutos, igual que la
     * base de datos. Un fallo de red no gasta intentos ni bloquea nada.
     */
    private fun aplicarFallo(error: Throwable) {
        val motivo = error as? AuthError ?: AuthError.SinConexion()

        when (motivo) {
            is AuthError.SinConexion -> {
                loginState = loginState.copy(
                    status = LoginStatus.ERROR,
                    pin = "",
                    errorMessage = motivo.message,
                )
            }

            is AuthError.CuentaBloqueada -> startBlockTimer(motivo.message)

            is AuthError.PinIncorrecto -> {
                val restantes = motivo.restantes
                    ?: (loginState.attemptsLeft - 1).coerceAtLeast(0)

                if (restantes <= 0) {
                    startBlockTimer(
                        "Demasiados intentos con PIN incorrecto. La cuenta quedó bloqueada por seguridad."
                    )
                } else {
                    loginState = loginState.copy(
                        status = LoginStatus.ERROR,
                        attemptsLeft = restantes,
                        pin = "",
                        errorMessage = motivo.message,
                    )
                }
            }

            is AuthError.Rechazado -> {
                loginState = loginState.copy(
                    status = LoginStatus.ERROR,
                    pin = "",
                    errorMessage = motivo.message,
                )
            }
        }

        state = AuthUiState(error = motivo.message)
    }

    private fun startBlockTimer(mensaje: String?) {
        blockTimerJob?.cancel()

        loginState = loginState.copy(
            status = LoginStatus.BLOCKED,
            blockTimerSeconds = AuthPolicy.SEGUNDOS_DE_BLOQUEO,
            attemptsLeft = 0,
            pin = "",
            errorMessage = mensaje,
        )

        blockTimerJob = viewModelScope.launch {
            while (loginState.blockTimerSeconds > 0) {
                delay(1000)
                loginState = loginState.copy(blockTimerSeconds = loginState.blockTimerSeconds - 1)
            }
            // Terminó el bloqueo: el próximo intento lo vuelve a decidir el servidor.
            loginState = loginState.copy(
                status = LoginStatus.NORMAL,
                attemptsLeft = AuthPolicy.MAX_INTENTOS,
                errorMessage = null,
            )
        }
    }

    // Registro en dos pasos: datos + PIN. El backend crea la cuenta con el PIN
    // hasheado, así que después se puede iniciar sesión con DNI + PIN.
    fun register(name: String, dni: String, phone: String, email: String, pin: String) {
        state = AuthUiState(isLoading = true)
        viewModelScope.launch {
            repo.register(name, dni, phone, email, pin)
                .onSuccess { registered ->
                    activeDni = registered.dni.ifBlank { dni.trim() }
                    UserSession.activeDni = activeDni
                    runCatching { LastDniStore.write(activeDni) }
                    state = AuthUiState(user = registered)
                }
                .onFailure {
                    state = AuthUiState(error = it.message ?: "Error inesperado")
                }
        }
    }

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

    /** Limpia el error compartido (al cambiar de pantalla o reintentar). */
    fun clearAuthError() {
        if (state.error != null) state = state.copy(error = null)
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