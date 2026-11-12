package com.gowayki.nesh.data.repository

import com.gowayki.nesh.core.network.HttpClientFactory
import com.gowayki.nesh.data.entities.backend.ConversacionResponseDto
import com.gowayki.nesh.data.entities.backend.CrearConversacionRequestDto
import com.gowayki.nesh.data.entities.backend.EnviarMensajeTextoRequestDto
import com.gowayki.nesh.data.entities.backend.MensajeResponseDto
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

object BackendChatApi {
    private val client = HttpClientFactory.client
    private val uploadClient = HttpClientFactory.uploadClient
    private val baseUrl: String get() = HttpClientFactory.baseUrl
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun usuarios(): Result<List<com.gowayki.nesh.data.entities.backend.BackendUsuarioDto>> = runCatching {
        val response = client.get("$baseUrl/usuarios")
        if (response.status.isSuccess()) {
            response.body<List<com.gowayki.nesh.data.entities.backend.BackendUsuarioDto>>()
        } else {
            emptyList()
        }
    }

    suspend fun conversaciones(usuarioId: String): Result<List<ConversacionResponseDto>> = runCatching {
        val response = client.get("$baseUrl/chat/usuarios/$usuarioId/conversaciones")
        if (response.status.isSuccess()) {
            response.body<List<ConversacionResponseDto>>()
        } else {
            emptyList()
        }
    }

    suspend fun crearConversacion(request: CrearConversacionRequestDto): Result<ConversacionResponseDto> = runCatching {
        val response = client.post("$baseUrl/chat/conversaciones") {
            setBody(request)
        }
        if (response.status.isSuccess()) {
            response.body<ConversacionResponseDto>()
        } else {
            throw Exception("Error al crear conversación: ${response.status.value}")
        }
    }

    suspend fun mensajes(conversacionId: String): Result<List<MensajeResponseDto>> = runCatching {
        val response = client.get("$baseUrl/chat/conversaciones/$conversacionId/mensajes")
        if (response.status.isSuccess()) {
            response.body<List<MensajeResponseDto>>()
        } else {
            emptyList()
        }
    }

    suspend fun enviarTexto(
        conversacionId: String,
        emisorId: String,
        contenido: String
    ): Result<MensajeResponseDto> = runCatching {
        val response = client.post("$baseUrl/chat/conversaciones/$conversacionId/mensajes/texto") {
            setBody(EnviarMensajeTextoRequestDto(emisorId = emisorId, contenidoTexto = contenido))
        }
        if (response.status.isSuccess()) {
            response.body<MensajeResponseDto>()
        } else {
            throw Exception("Error al enviar mensaje: ${response.status.value}")
        }
    }

    suspend fun enviarAudio(
        conversacionId: String,
        emisorId: String,
        audioBytes: ByteArray,
        duracionSegundos: Int
    ): Result<MensajeResponseDto> = runCatching {
        val response = uploadClient.submitFormWithBinaryData(
            url = "$baseUrl/chat/conversaciones/$conversacionId/mensajes/audio",
            formData = formData {
                append("emisorId", emisorId)
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
            throw Exception("Error al subir audio: ${response.status.value}")
        }
    }
}

