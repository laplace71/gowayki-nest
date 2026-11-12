package com.gowayki.nesh.data.entities.backend

import kotlinx.serialization.Serializable

@Serializable
data class MensajeResponseDto(
    val id: String,
    val conversacionId: String,
    val emisorId: String,
    val emisorNombre: String? = null,
    val tipo: String,
    val contenidoTexto: String? = null,
    val audioUrl: String? = null,
    val audioDuracionSegundos: Int? = null,
    val estado: String? = null,
    val createdAt: String? = null
)

@Serializable
data class ConversacionResponseDto(
    val id: String,
    val titulo: String,
    val esGrupo: Boolean = false,
    val createdBy: String? = null,
    val createdAt: String? = null,
    val totalMensajes: Int = 0,
    val ultimoMensaje: MensajeResponseDto? = null,
    val totalParticipantes: Int? = null
)

@Serializable
data class CrearConversacionRequestDto(
    val titulo: String,
    val esGrupo: Boolean = false,
    val createdBy: String,
    val participanteIds: List<String> = emptyList()
)

@Serializable
data class EnviarMensajeTextoRequestDto(
    val emisorId: String,
    val contenido: String
)

@Serializable
data class BackendUsuarioDto(
    val id: String,
    val nombre: String,
    val dni: String? = null,
    val telefono: String? = null,
    val email: String? = null,
    val fotoUrl: String? = null,
    val rol: String? = null,
    val isOnline: Boolean = false
)

