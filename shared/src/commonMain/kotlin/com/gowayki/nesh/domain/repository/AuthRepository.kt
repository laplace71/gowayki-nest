package com.gowayki.nesh.domain.repository

import com.gowayki.nesh.domain.model.User

// Contrato de autenticación (domain). Las pantallas/ViewModel dependen de
// esto, nunca de la base de datos. Implementaciones en data/repository.
interface AuthRepository {
    suspend fun login(pin: String): Result<User>

    /** Login con una persona concreta; por defecto usa el login simple. */
    suspend fun loginWithDni(dni: String, pin: String): Result<User> = login(pin)

    suspend fun register(name: String, dni: String, phone: String, email: String, pin: String): Result<User>

    /** La app envía una solicitud de chofer; el jefe la aprueba en el panel. */
    suspend fun registrarSolicitud(
        name: String,
        dni: String,
        phone: String,
        email: String,
        pin: String,
        subrutaId: String,
    ): Result<Unit> = Result.failure(UnsupportedOperationException("No disponible."))

    /** Borrado lógico: sale de los chats y desactiva la cuenta, sin borrar mensajes. */
    suspend fun deleteAccount(): Result<Unit> =
        Result.failure(UnsupportedOperationException("Esta implementación no soporta eliminar cuenta."))
    suspend fun google(idToken: String): Result<User>
    suspend fun savePin(phone: String, pin: String): Result<User>

    /** Cambia el PIN de la cuenta autenticada (exige el PIN actual). */
    suspend fun cambiarPin(pinActual: String, pinNuevo: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Esta implementación no soporta cambiar el PIN."))
}
