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
    /** PRIVADA, GRUPO o GRUPO_RUTA. */
    val tipoConversacion: String? = null,
    /** Subruta concreta cuando el tipo es GRUPO_RUTA. */
    val claveRuta: String? = null,
    val createdBy: String? = null,
    val createdAt: String? = null,
    val totalMensajes: Int = 0,
    /** Lo calcula el backend por usuario; si no viene, se mantiene el conteo local. */
    val noLeidos: Int? = null,
    val ultimoMensaje: MensajeResponseDto? = null,
    val totalParticipantes: Int? = null,
    /** Rol del usuario que pidió la lista en esta conversación (ADMIN, MIEMBRO o null). */
    val miRol: String? = null,
    /** Hay una eliminación definitiva esperando las 72 horas. */
    val eliminacionPendiente: Boolean = false,
    /** Cuándo se ejecutará esa eliminación (ISO-8601). */
    val eliminacionProgramadaPara: String? = null
)

@Serializable
data class CrearConversacionRequestDto(
    val titulo: String,
    val esGrupo: Boolean = false,
    /** PRIVADA, GRUPO o GRUPO_RUTA. */
    val tipoConversacion: String? = null,
    /** Subruta concreta para un GRUPO_RUTA. */
    val claveRuta: String? = null,
    val createdBy: String? = null,
    val participanteIds: List<String> = emptyList()
)

/** Respuesta de `POST /chat/conversaciones/{id}/eliminacion`. */
@Serializable
data class EliminacionGrupoResponseDto(
    val conversacionId: String,
    val solicitudId: String? = null,
    val estado: String? = null,
    val solicitadoEn: String? = null,
    val seEliminaraEn: String? = null,
    val horasDeEspera: Int? = null
)

@Serializable
data class TotalNoLeidosDto(
    val total: Int = 0
)

@Serializable
data class AgregarParticipantesRequestDto(
    val usuarioIds: List<String> = emptyList()
)

@Serializable
data class EnviarMensajeTextoRequestDto(
    val contenidoTexto: String
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

@Serializable
data class ParticipanteResponseDto(
    val userId: String,
    val nombre: String? = null,
    val rol: String? = null,
    val fotoUrl: String? = null,
    val isOnline: Boolean? = null
)

