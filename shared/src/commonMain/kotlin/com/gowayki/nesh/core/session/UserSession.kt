package com.gowayki.nesh.core.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gowayki.nesh.domain.model.User
import com.gowayki.nesh.data.repository.DisplayNameState

/**
 * Sesión de la persona que inició sesión (login o registro).
 *
 * `currentUser` es un estado observable: cuando el login/registro guarda al
 * usuario, cualquier pantalla que lea el nombre (Home, menú lateral, perfil…)
 * se refresca sola.
 */
object UserSession {
    var currentUser: User? by mutableStateOf(null)
        private set

    var activeDni: String = "71234567"

    val currentUserId: String?
        get() = currentUser?.id

    /** Nombre para mostrar en la UI, con un reemplazo si aún no hay sesión. */
    val displayName: String
        get() {
            val override = DisplayNameState.value.value?.takeIf { it.isNotBlank() }
            if (override != null) return override
            val backend = currentUser?.name?.trim().orEmpty()
            if (backend.isBlank() || backend.contains("@")) return "Conductor"
            return backend
        }

    /** Iniciales para el avatar (p. ej. "Juan Pérez" -> "JP"). */
    val displayInitials: String
        get() = displayName
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "W" }

    fun saveUser(user: User) {
        currentUser = user
        if (user.dni.isNotBlank()) {
            activeDni = user.dni
        }
    }

    fun clear() {
        currentUser = null
        TurnoSession.clear()
    }
}
