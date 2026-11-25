package com.gowayki.nesh.data.repository

import com.gowayki.nesh.core.network.HttpClientFactory
import com.gowayki.nesh.core.session.AuthTokenState
import com.gowayki.nesh.core.session.UserSession
import com.gowayki.nesh.data.entities.auth.CambiarPinRequestDto
import com.gowayki.nesh.data.entities.auth.LoginRequestDto
import com.gowayki.nesh.data.entities.auth.LoginResponseDto
import com.gowayki.nesh.data.entities.auth.RegisterRequestDto
import com.gowayki.nesh.data.entities.auth.RegistroSolicitudRequestDto
import com.gowayki.nesh.domain.model.AuthError
import com.gowayki.nesh.domain.model.User
import com.gowayki.nesh.domain.repository.AuthRepository
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KtorAuthRepository : AuthRepository {

    private val client = HttpClientFactory.client
    private val baseUrl = HttpClientFactory.baseUrl

    override suspend fun login(pin: String): Result<User> {
        return loginWithDni(UserSession.activeDni, pin)
    }

    override suspend fun loginWithDni(dni: String, pin: String): Result<User> = withContext(Dispatchers.Default) {
        try {
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
                        phone = body.usuario.telefono,
                        rol = body.usuario.rol,
                        subrutaId = body.usuario.subrutaId,
                        subrutaNombre = body.usuario.subrutaNombre
                    )
                    // Sin token el chat respondería 401: se guarda antes de dar
                    // la sesión por iniciada.
                    AuthTokenState.save(body.token)
                    UserSession.saveUser(user)
                    Result.success(user)
                } else {
                    Result.failure(
                        AuthError.Rechazado(body.mensaje ?: "No pudimos iniciar sesión. Inténtalo otra vez.")
                    )
                }
            } else {
                val mensaje = try {
                    response.body<LoginResponseDto>().mensaje
                } catch (_: Exception) {
                    null
                }
                Result.failure(clasificarErrorDeLogin(response.status.value, mensaje))
            }
        } catch (e: AuthError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(desdeExcepcion(e))
        }
    }

    override suspend fun register(name: String, dni: String, phone: String, email: String, pin: String): Result<User> =
        withContext(Dispatchers.Default) {
        try {
            val response: HttpResponse = client.post("$baseUrl/auth/registro") {
                setBody(
                    RegisterRequestDto(
                        nombre = name.trim(),
                        dni = dni.trim(),
                        telefono = phone.trim(),
                        email = email.trim().takeIf { it.isNotBlank() },
                        pin = pin.trim(),
                    )
                )
            }

            val body: LoginResponseDto? = try {
                response.body<LoginResponseDto>()
            } catch (_: Exception) {
                null
            }
            val dto = body?.usuario

            if (response.status.isSuccess() && body?.exito == true && dto != null) {
                val user = User(
                    id = dto.id,
                    name = dto.nombre,
                    email = dto.email ?: "",
                    dni = dto.dni,
                    phone = dto.telefono,
                    rol = dto.rol,
                    subrutaId = dto.subrutaId,
                    subrutaNombre = dto.subrutaNombre,
                )
                AuthTokenState.save(body.token)
                UserSession.saveUser(user)
                Result.success(user)
            } else {
                Result.failure(
                    AuthError.Rechazado(
                        body?.mensaje ?: "No pudimos crear la cuenta. Inténtalo otra vez."
                    )
                )
            }
        } catch (e: AuthError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(desdeExcepcion(e))
        }
    }

    override suspend fun registrarSolicitud(
        name: String,
        dni: String,
        phone: String,
        email: String,
        pin: String,
        subrutaId: String,
    ): Result<Unit> = withContext(Dispatchers.Default) {
        val subrutaEntera = subrutaId.toIntOrNull()
            ?: return@withContext Result.failure(
                IllegalArgumentException("Selecciona una subruta válida."))
        try {
            val response: HttpResponse = client.post("$baseUrl/auth/registro-solicitud") {
                setBody(RegistroSolicitudRequestDto(
                    nombre = name.trim(),
                    dni = dni.trim(),
                    telefono = phone.trim(),
                    email = email.trim().takeIf { it.isNotBlank() },
                    pin = pin.trim(),
                    subrutaId = subrutaEntera,
                ))
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else {
                val mensaje = try {
                    response.body<LoginResponseDto>().mensaje
                } catch (_: Exception) { null }
                Result.failure(AuthError.Rechazado(mensaje ?: "No se pudo enviar la solicitud."))
            }
        } catch (e: Exception) {
            Result.failure(desdeExcepcion(e))
        }
    }

    override suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val response: HttpResponse = client.post("$baseUrl/auth/eliminar-cuenta")
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("No se pudo eliminar la cuenta (HTTP ${response.status.value})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cambiarPin(pinActual: String, pinNuevo: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val response: HttpResponse = client.post("$baseUrl/auth/cambiar-pin") {
                setBody(CambiarPinRequestDto(pinActual = pinActual, pinNuevo = pinNuevo))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val mensaje = try {
                    response.body<LoginResponseDto>().mensaje
                } catch (_: Exception) {
                    null
                }
                Result.failure(AuthError.Rechazado(mensaje ?: "No se pudo cambiar el PIN."))
            }
        } catch (e: Exception) {
            Result.failure(desdeExcepcion(e))
        }
    }

    override suspend fun google(idToken: String): Result<User> {
        return Result.failure(UnsupportedOperationException("Autenticación con Google no disponible"))
    }

    override suspend fun savePin(phone: String, pin: String): Result<User> {
        val currentUser = UserSession.currentUser ?: User(id = "user-1", name = "Chofer", phone = phone)
        UserSession.saveUser(currentUser)
        return Result.success(currentUser)
    }

    // ---------- Traducción de errores ----------

    /**
     * Cualquier fallo que no venga del servidor es de red o de la propia app.
     * Nunca se muestra el mensaje original: trae la IP y detalles internos.
     */
    private fun desdeExcepcion(e: Exception): AuthError =
        if (esFalloDeRed(e)) {
            AuthError.SinConexion()
        } else {
            AuthError.Rechazado("No pudimos completar la solicitud. Inténtalo otra vez.")
        }

    private fun esFalloDeRed(e: Throwable): Boolean {
        var actual: Throwable? = e
        while (actual != null) {
            if (actual::class.simpleName.orEmpty() in FALLOS_DE_RED) return true
            actual = actual.cause
        }
        return false
    }

    private companion object {
        val FALLOS_DE_RED = setOf(
            "IOException",
            "ConnectException",
            "SocketTimeoutException",
            "UnknownHostException",
            "UnresolvedAddressException",
            "HttpRequestTimeoutException",
            "ConnectTimeoutException",
        )
    }
}

/** "…Intentos restantes antes de bloqueo temporal: 3" → 3. */
private val RESTANTES = Regex("""restantes[^0-9]*(\d+)""", RegexOption.IGNORE_CASE)

/**
 * Convierte la respuesta de error del backend en un motivo legible.
 *
 * El backend responde 423 cuando la cuenta está bloqueada y 401 con
 * "PIN incorrecto. Intentos restantes…: N" cuando el PIN falla. De ese texto
 * sale el número exacto de intentos que guarda la base de datos.
 */
internal fun clasificarErrorDeLogin(status: Int, mensaje: String?): AuthError {
    val texto = mensaje?.trim()?.takeIf { it.isNotBlank() }

    if (status == 423 || texto?.contains("bloqueada", ignoreCase = true) == true) {
        return AuthError.CuentaBloqueada(
            texto ?: "La cuenta está bloqueada temporalmente. Inténtalo más tarde."
        )
    }

    val restantes = RESTANTES.find(texto.orEmpty())
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()

    if (texto != null && texto.contains("PIN incorrecto", ignoreCase = true)) {
        return AuthError.PinIncorrecto(texto, restantes)
    }

    return AuthError.Rechazado(texto ?: "No pudimos iniciar sesión. Inténtalo otra vez.")
}
