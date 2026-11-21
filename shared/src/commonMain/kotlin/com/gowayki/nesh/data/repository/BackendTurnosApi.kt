package com.gowayki.nesh.data.repository

import com.gowayki.nesh.core.network.HttpClientFactory
import com.gowayki.nesh.data.entities.backend.ActualizarPosicionGpsRequestDto
import com.gowayki.nesh.data.entities.backend.AsignarTurnoRequestDto
import com.gowayki.nesh.data.entities.backend.BackendMensajeDto
import com.gowayki.nesh.data.entities.backend.BackendTurnoDto
import com.gowayki.nesh.data.entities.backend.BackendUnidadDto
import com.gowayki.nesh.data.entities.backend.PosicionGpsDto
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * API de turnos y unidades.
 *
 * Un turno vincula al chofer con una unidad; la unidad pertenece a una línea.
 * Es la fuente para saber qué ruta/combi tiene asignada cada persona.
 *
 * Toda petición se ejecuta en IO: estas funciones se llaman también desde
 * `LaunchedEffect`, que corre en Main. Si la petición se cancela al salir de la
 * pantalla, Ktor cierra el stream en el hilo de la corrutina; en Main eso
 * lanza `NetworkOnMainThreadException`.
 */
object BackendTurnosApi {
    private val client = HttpClientFactory.client
    private val baseUrl get() = HttpClientFactory.baseUrl

    /** Turno activo del chofer, o null si todavía no le asignaron unidad. */
    suspend fun turnoActivo(choferId: String): BackendTurnoDto? = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$baseUrl/turnos/chofer/$choferId/activo")
            when {
                response.status.value == 204 -> null
                response.status.isSuccess() -> response.body<BackendTurnoDto>()
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Todos los turnos en curso (para saber qué unidades están ocupadas). */
    suspend fun turnosActivos(): List<BackendTurnoDto> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$baseUrl/turnos/activos")
            if (response.status.isSuccess()) response.body<List<BackendTurnoDto>>() else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Todas las unidades del sistema, con su línea. */
    suspend fun unidadesTodas(): List<BackendUnidadDto> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$baseUrl/unidades")
            if (response.status.isSuccess()) response.body<List<BackendUnidadDto>>() else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Unidades que pertenecen a una línea. */
    suspend fun unidadesPorLinea(lineaId: Int): List<BackendUnidadDto> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$baseUrl/lineas/$lineaId/unidades")
            if (response.status.isSuccess()) response.body<List<BackendUnidadDto>>() else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun actualizarPosicion(
        unidadId: Int,
        latitud: Double,
        longitud: Double,
        velocidadKmh: Double = 0.0,
        rumboGrados: Int = 0
    ): Result<PosicionGpsDto> = withContext(Dispatchers.IO) {
        runCatching {
            val response: HttpResponse = client.put("$baseUrl/unidades/$unidadId/posicion") {
                setBody(
                    ActualizarPosicionGpsRequestDto(
                        latitud = latitud,
                        longitud = longitud,
                        velocidadKmh = velocidadKmh,
                        rumboGrados = rumboGrados
                    )
                )
            }
            if (response.status.isSuccess()) {
                response.body<PosicionGpsDto>()
            } else {
                throw Exception("HTTP ${response.status.value}")
            }
        }
    }

    /** Crea el turno: deja al chofer en ruta con la unidad indicada (y subruta concreta). */
    suspend fun asignar(
        choferId: String,
        unidadId: Int,
        subrutaId: Int? = null,
        subrutaCodigo: String? = null,
    ): Result<BackendTurnoDto> = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = client.post("$baseUrl/turnos/asignar") {
                setBody(
                    AsignarTurnoRequestDto(
                        choferId = choferId,
                        unidadId = unidadId,
                        subrutaId = subrutaId,
                        subrutaCodigo = subrutaCodigo,
                    )
                )
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<BackendTurnoDto>())
            } else {
                Result.failure(Exception(mensajeDeError(response, "No se pudo asignar la ruta")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "No se pudo conectar con el servidor"))
        }
    }

    /** Finaliza el turno actual para poder elegir otra unidad. */
    suspend fun finalizar(turnoId: String): Result<BackendTurnoDto> = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = client.post("$baseUrl/turnos/$turnoId/finalizar")
            if (response.status.isSuccess()) {
                Result.success(response.body<BackendTurnoDto>())
            } else {
                Result.failure(Exception(mensajeDeError(response, "No se pudo finalizar el turno")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "No se pudo conectar con el servidor"))
        }
    }

    private suspend fun mensajeDeError(response: HttpResponse, fallback: String): String =
        runCatching { response.body<BackendMensajeDto>().mensaje }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "$fallback (${response.status.value})"
}
