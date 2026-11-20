package com.gowayki.nesh.data.repository

import com.gowayki.nesh.core.network.HttpClientFactory
import com.gowayki.nesh.data.entities.backend.ConversacionResponseDto
import com.gowayki.nesh.data.entities.backend.CrearConversacionRequestDto
import com.gowayki.nesh.data.entities.backend.EliminacionGrupoResponseDto
import com.gowayki.nesh.data.entities.backend.EnviarMensajeTextoRequestDto
import com.gowayki.nesh.data.entities.backend.MensajeResponseDto
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Llamadas al backend del chat.
 *
 * El usuario ya no se manda en la URL ni en el cuerpo: el backend lo saca del
 * JWT, que `HttpClientFactory` añade como `Authorization`.
 */
object BackendChatApi {
    private val client = HttpClientFactory.client
    private val uploadClient = HttpClientFactory.uploadClient
    private val baseUrl: String get() = HttpClientFactory.baseUrl
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun usuarios(): Result<List<com.gowayki.nesh.data.entities.backend.BackendUsuarioDto>> = runCatching {
        withContext(Dispatchers.Default) {
            val response = client.get("$baseUrl/usuarios")
            if (response.status.isSuccess()) {
                response.body<List<com.gowayki.nesh.data.entities.backend.BackendUsuarioDto>>()
            } else {
                emptyList()
            }
        }
    }

    suspend fun participantes(conversacionId: String): Result<List<com.gowayki.nesh.data.entities.backend.ParticipanteResponseDto>> = runCatching {
        withContext(Dispatchers.Default) {
            val response = client.get("$baseUrl/chat/conversaciones/$conversacionId/participantes")
            if (response.status.isSuccess()) {
                response.body<List<com.gowayki.nesh.data.entities.backend.ParticipanteResponseDto>>()
            } else {
                throw errorDeRespuesta(response, "No se pudieron listar los participantes")
            }
        }
    }

    /** Conversaciones del usuario autenticado (el backend ya descarta las que ocultó). */
    suspend fun conversaciones(): Result<List<ConversacionResponseDto>> = runCatching {
        withContext(Dispatchers.Default) {
            val response = client.get("$baseUrl/chat/conversaciones")
            if (response.status.isSuccess()) {
                response.body<List<ConversacionResponseDto>>()
            } else {
                throw errorDeRespuesta(response, "No se pudieron cargar las conversaciones")
            }
        }
    }

    suspend fun crearConversacion(request: CrearConversacionRequestDto): Result<ConversacionResponseDto> = runCatching {
        withContext(Dispatchers.Default) {
            val response = client.post("$baseUrl/chat/conversaciones") {
                setBody(request)
            }
            if (response.status.isSuccess()) {
                response.body<ConversacionResponseDto>()
            } else {
                throw errorDeRespuesta(response, "No se pudo crear la conversación")
            }
        }
    }

    /**
     * Saca al usuario de la conversación (POST .../salir). El grupo y sus
     * mensajes siguen existiendo para los demás.
     */
    suspend fun salirConversacion(conversacionId: String): Result<Unit> = runCatching {
        withContext(Dispatchers.Default) {
            val response = client.post("$baseUrl/chat/conversaciones/$conversacionId/salir")
            if (!response.status.isSuccess()) {
                throw errorDeRespuesta(response, "No se pudo salir del grupo")
            }
        }
    }

    /**
     * Oculta la conversación SOLO para el usuario autenticado. No borra
     * mensajes, audios ni la conversación: los demás no notan nada.
     */
    suspend fun ocultarConversacion(conversacionId: String): Result<Unit> = runCatching {
        withContext(Dispatchers.Default) {
            val response = client.delete("$baseUrl/chat/conversaciones/$conversacionId")
            if (!response.status.isSuccess()) {
                throw errorDeRespuesta(response, "No se pudo quitar el chat")
            }
        }
    }

    /**
     * Guarda la marca de "limpiar chat" en el backend (por usuario). No borra
     * mensajes ni avisa a los demás; sobrevive a reinstalar la app.
     */
    suspend fun limpiarConversacion(conversacionId: String): Result<Unit> = runCatching {
        withContext(Dispatchers.Default) {
            val response = client.post("$baseUrl/chat/conversaciones/$conversacionId/limpiar")
            if (!response.status.isSuccess()) {
                throw errorDeRespuesta(response, "No se pudo limpiar el chat")
            }
        }
    }

    /** Deshace la ocultación: la conversación vuelve a la lista. */
    suspend fun mostrarConversacion(conversacionId: String): Result<Unit> = runCatching {
        withContext(Dispatchers.Default) {
            val response = client.post("$baseUrl/chat/conversaciones/$conversacionId/mostrar")
            if (!response.status.isSuccess()) {
                throw errorDeRespuesta(response, "No se pudo restaurar el chat")
            }
        }
    }

    /**
     * Pide la eliminación definitiva de un grupo. No borra al momento: el
     * backend espera 72 horas antes de purgarlo.
     */
    suspend fun solicitarEliminacionGrupo(conversacionId: String): Result<EliminacionGrupoResponseDto> = runCatching {
        withContext(Dispatchers.Default) {
            val response = client.post("$baseUrl/chat/conversaciones/$conversacionId/eliminacion")
            if (response.status.isSuccess()) {
                response.body<EliminacionGrupoResponseDto>()
            } else {
                throw errorDeRespuesta(response, "No se pudo pedir la eliminación del grupo")
            }
        }
    }

    /** Cancela la eliminación definitiva antes de que se cumplan las 72 horas. */
    suspend fun cancelarEliminacionGrupo(conversacionId: String): Result<Unit> = runCatching {
        withContext(Dispatchers.Default) {
            val response = client.delete("$baseUrl/chat/conversaciones/$conversacionId/eliminacion")
            if (!response.status.isSuccess()) {
                throw errorDeRespuesta(response, "No se pudo cancelar la eliminación")
            }
        }
    }

    suspend fun mensajes(conversacionId: String): Result<List<MensajeResponseDto>> = runCatching {
        withContext(Dispatchers.Default) {
            val response = client.get("$baseUrl/chat/conversaciones/$conversacionId/mensajes")
            if (response.status.isSuccess()) {
                response.body<List<MensajeResponseDto>>()
            } else {
                throw errorDeRespuesta(response, "No se pudieron cargar los mensajes")
            }
        }
    }

    /** Cambia el rol de un participante. El backend valida que quien pide sea ADMIN. */
    suspend fun asignarRol(conversacionId: String, userId: String, rol: String): Result<Unit> = runCatching {
        withContext(Dispatchers.Default) {
            val response = client.put(
                "$baseUrl/chat/conversaciones/$conversacionId/participantes/$userId/rol?rol=$rol"
            )
            if (!response.status.isSuccess()) {
                throw errorDeRespuesta(response, "No se pudo cambiar el rol")
            }
        }
    }

    /** "Eliminar para mí": marca personal en el backend, no afecta a los demás. */
    suspend fun ocultarMensaje(conversacionId: String, mensajeId: String): Result<Unit> = runCatching {
        withContext(Dispatchers.Default) {
            val response = client.delete(
                "$baseUrl/chat/conversaciones/$conversacionId/mensajes/$mensajeId"
            )
            if (!response.status.isSuccess()) {
                throw errorDeRespuesta(response, "No se pudo eliminar el mensaje")
            }
        }
    }

    /** "Eliminar para todos": solo el emisor; el backend purga el archivo a las 72 h. */
    suspend fun eliminarMensajeParaTodos(conversacionId: String, mensajeId: String): Result<Unit> = runCatching {
        withContext(Dispatchers.Default) {
            val response = client.delete(
                "$baseUrl/chat/conversaciones/$conversacionId/mensajes/$mensajeId/todos"
            )
            if (!response.status.isSuccess()) {
                throw errorDeRespuesta(response, "No se pudo eliminar el mensaje")
            }
        }
    }

    suspend fun enviarTexto(
        conversacionId: String,
        contenido: String
    ): Result<MensajeResponseDto> = runCatching {
        withContext(Dispatchers.Default) {
            val response = client.post("$baseUrl/chat/conversaciones/$conversacionId/mensajes/texto") {
                setBody(EnviarMensajeTextoRequestDto(contenidoTexto = contenido))
            }
            if (response.status.isSuccess()) {
                response.body<MensajeResponseDto>()
            } else {
                throw errorDeRespuesta(response, "No se pudo enviar el mensaje")
            }
        }
    }

    suspend fun enviarAudio(
        conversacionId: String,
        audioBytes: ByteArray,
        duracionSegundos: Int
    ): Result<MensajeResponseDto> = runCatching {
        withContext(Dispatchers.Default) {
            val response = uploadClient.submitFormWithBinaryData(
                url = "$baseUrl/chat/conversaciones/$conversacionId/mensajes/audio",
                formData = formData {
                    append("duracionSegundos", duracionSegundos.toString())
                    append("audio", audioBytes, Headers.build {
                        append(HttpHeaders.ContentType, "audio/m4a")
                        append(HttpHeaders.ContentDisposition, "filename=\"audio.m4a\"")
                    })
                }
            )
            if (response.status.isSuccess()) {
                val bodyText = response.bodyAsText()
                json.decodeFromString<MensajeResponseDto>(bodyText)
            } else {
                throw errorDeRespuesta(response, "No se pudo subir el audio")
            }
        }
    }

    /**
     * Traduce la respuesta de error del backend a una excepción con el mensaje
     * que el usuario puede entender. El 401 se marca aparte: significa que la
     * sesión venció y hay que volver a entrar.
     */
    private suspend fun errorDeRespuesta(response: HttpResponse, contexto: String): Exception {
        val detalle = runCatching {
            json.parseToJsonElement(response.bodyAsText()).jsonObject["mensaje"]?.jsonPrimitive?.content
        }.getOrNull()

        val mensaje = when {
            response.status == HttpStatusCode.Unauthorized ->
                detalle ?: "Tu sesión venció. Vuelve a iniciar sesión."
            response.status == HttpStatusCode.Forbidden ->
                detalle ?: "No tienes permiso para esta acción."
            else -> detalle ?: "$contexto (${response.status.value})"
        }
        return Exception(mensaje)
    }
}
