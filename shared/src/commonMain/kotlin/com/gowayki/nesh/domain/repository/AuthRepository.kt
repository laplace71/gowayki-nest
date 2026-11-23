package com.gowayki.nesh.domain.repository

import com.gowayki.nesh.domain.model.User

// Contrato de autenticación (domain). Las pantallas/ViewModel dependen de
// esto, nunca de la base de datos. Implementaciones en data/repository.
interface AuthRepository {
    suspend fun login(pin: String): Result<User>

    /** Login con una persona concreta; por defecto usa el login simple. */
    suspend fun loginWithDni(dni: String, pin: String): Result<User> = login(pin)

    suspend fun register(name: String, dni: String, phone: String, email: String, pin: String): Result<User>

    /** Borrado lógico: sale de los chats y desactiva la cuenta, sin borrar mensajes. */
    suspend fun deleteAccount(): Result<Unit> =
        Result.failure(UnsupportedOperationException("Esta implementación no soporta eliminar cuenta."))
    suspend fun google(idToken: String): Result<User>
    suspend fun savePin(phone: String, pin: String): Result<User>
}
