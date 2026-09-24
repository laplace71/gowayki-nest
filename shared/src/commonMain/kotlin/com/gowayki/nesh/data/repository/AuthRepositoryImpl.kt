package com.gowayki.nesh.data.repository

import com.gowayki.nesh.domain.model.User
import com.gowayki.nesh.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

// Simula el backend: espera 1 s y devuelve un usuario demo.
// Úsalo para probar el flujo login/registro/PIN sin internet.
// FASE JSON: aquí irá la lectura/escritura a archivos JSON locales.
class FakeAuthRepository : AuthRepository {

    private fun validDniPhone(dni: String, phone: String): Result<Unit> {
        if (dni.length != 8) {
            return Result.failure(IllegalArgumentException("El DNI debe tener 8 dígitos"))
        }
        if (phone.length != 9) {
            return Result.failure(IllegalArgumentException("El celular debe tener 9 dígitos"))
        }
        return Result.success(Unit)
    }

    override suspend fun login(pin: String): Result<User> {
        delay(1.seconds)
        if (pin.length != 6) {
            return Result.failure(IllegalArgumentException("El PIN debe tener 6 dígitos"))
        }
        return Result.success(User(id = "demo-1", name = "Demo"))
    }

    override suspend fun register(name: String, dni: String, phone: String): Result<User> {
        delay(1.seconds)
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Ingresa tu nombre"))
        }
        validDniPhone(dni, phone).onFailure { return Result.failure(it) }
        return Result.success(User(id = "demo-1", name = name, dni = dni, phone = phone))
    }

    override suspend fun google(idToken: String): Result<User> {
        delay(1.seconds)
        if (idToken.isBlank()) {
            return Result.failure(IllegalArgumentException("No se pudo obtener la cuenta de Google"))
        }
        return Result.success(User(id = "demo-google", name = "Google"))
    }

    override suspend fun savePin(phone: String, pin: String): Result<User> {
        delay(1.seconds)
        if (pin.length != 6) {
            return Result.failure(IllegalArgumentException("El PIN debe tener 6 dígitos"))
        }
        // Demo: acepta cualquier PIN de 6 dígitos.
        return Result.success(User(id = "demo-1", name = "Demo", phone = phone))
    }
}

/*
 * FASE BACKEND — Hablaba con el backend real a través de Retrofit (mapper DTO -> dominio).
 * Retrofit no existe en el KMP (esto es commonMain); para backend real o JSON usa
 * Ktor (ya está en las dependencias) o archivos locales. Se comenta hasta definir la fase JSON.
 *
 * class RealAuthRepository(private val api: AuthApi) : AuthRepository {
 *     override suspend fun login(pin: String): Result<User> =
 *         runCatching { api.login(LoginRequest(pin)).user.toDomain() }
 *
 *     override suspend fun register(name: String, dni: String, phone: String): Result<User> =
 *         runCatching { api.register(RegisterRequest(name, dni, phone)).user.toDomain() }
 *
 *     override suspend fun google(idToken: String): Result<User> =
 *         runCatching { api.google(GoogleRequest(idToken)).user.toDomain() }
 *
 *     override suspend fun savePin(phone: String, pin: String): Result<User> =
 *         runCatching { api.setPin(PinRequest(phone, pin)).user.toDomain() }
 * }
 */