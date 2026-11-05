package com.gowayki.nesh.data.repository

import com.gowayki.nesh.core.network.HttpClientFactory
import com.gowayki.nesh.core.session.UserSession
import com.gowayki.nesh.data.entities.auth.LoginRequestDto
import com.gowayki.nesh.data.entities.auth.LoginResponseDto
import com.gowayki.nesh.domain.model.User
import com.gowayki.nesh.domain.repository.AuthRepository
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

class KtorAuthRepository : AuthRepository {

    private val client = HttpClientFactory.client
    private val baseUrl = HttpClientFactory.baseUrl

    override suspend fun login(pin: String): Result<User> {
        return loginWithDni(UserSession.activeDni, pin)
    }

    suspend fun loginWithDni(dni: String, pin: String): Result<User> {
        return try {
            val response: HttpResponse = client.post("$baseUrl/auth/login") {
                setBody(LoginRequestDto(dni = dni.trim(), pin = pin.trim()))
            }

            if (response.status.isSuccess()) {
                val body = response.body<LoginResponseDto>()
                if (body.exito && body.usuario != null) {
                    val user = User(
                        id = body.usuario.id,
                        name = body.usuario.nombre,
                        email = body.usuario.email ?: "",
                        dni = body.usuario.dni,
                        phone = body.usuario.telefono
                    )
                    UserSession.saveUser(user)
                    Result.success(user)
                } else {
                    Result.failure(Exception(body.mensaje ?: "Error de autenticación"))
                }
            } else {
                val errorMsg = try {
                    val errorBody = response.body<LoginResponseDto>()
                    errorBody.mensaje ?: "Credenciales o PIN incorrectos"
                } catch (_: Exception) {
                    "Error de autenticación (${response.status.value})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "No se pudo conectar con el servidor"))
        }
    }

    override suspend fun register(name: String, dni: String, phone: String): Result<User> {
        val user = User(
            id = "pending-id",
            name = name,
            dni = dni,
            phone = phone
        )
        UserSession.saveUser(user)
        return Result.success(user)
    }

    override suspend fun google(idToken: String): Result<User> {
        return Result.failure(UnsupportedOperationException("Autenticación con Google no disponible"))
    }

    override suspend fun savePin(phone: String, pin: String): Result<User> {
        val currentUser = UserSession.currentUser ?: User(id = "user-1", name = "Chofer", phone = phone)
        UserSession.saveUser(currentUser)
        return Result.success(currentUser)
    }
}
