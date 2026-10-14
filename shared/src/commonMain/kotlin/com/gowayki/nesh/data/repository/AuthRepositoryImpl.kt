package com.gowayki.nesh.data.repository

import com.gowayki.nesh.domain.model.User
import com.gowayki.nesh.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

// Simula el backend: espera 1 s y valida que el PIN sea 123456 para pruebas.
class FakeAuthRepository : AuthRepository {

    private fun validDniPhone(dni: String, phone: String): Result<Unit> {
        if (dni.length != 8) return Result.failure(IllegalArgumentException("El DNI debe tener 8 digitos"))
        if (phone.length != 9) return Result.failure(IllegalArgumentException("El celular debe tener 9 digitos"))
        return Result.success(Unit)
    }

    override suspend fun login(pin: String): Result<User> {
        delay(1.seconds)
        if (pin.length != 6) {
            return Result.failure(IllegalArgumentException("El PIN debe tener 6 digitos"))
        }
        if (pin != "123456") {
            return Result.failure(IllegalArgumentException("PIN incorrecto. Te quedan pocos intentos."))
        }
        return Result.success(User(id = "demo-1", name = "Demo"))
    }

    override suspend fun register(name: String, dni: String, phone: String): Result<User> {
        delay(1.seconds)
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Ingresa tu nombre"))
        validDniPhone(dni, phone).onFailure { return Result.failure(it) }
        return Result.success(User(id = "demo-1", name = name, dni = dni, phone = phone))
    }

    override suspend fun google(idToken: String): Result<User> {
        delay(1.seconds)
        if (idToken.isBlank()) return Result.failure(IllegalArgumentException("No se pudo obtener la cuenta de Google"))
        return Result.success(User(id = "demo-google", name = "Google", email = "demo@wayki.com"))
    }

    override suspend fun savePin(phone: String, pin: String): Result<User> {
        delay(1.seconds)
        if (pin.length != 6) return Result.failure(IllegalArgumentException("El PIN debe tener 6 digitos"))
        return Result.success(User(id = "demo-1", name = "Demo", phone = phone))
    }
}