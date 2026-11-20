// Modelos de dominio de Comunicación (contratos independientes de la UI).
// Patrón igual que AuthRepository: las pantallas dependen de la interfaz,
// nunca de la implementación (hoy FakeChatRepository, mañana Supabase).
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.communication.domain.model

import kotlinx.serialization.Serializable

/** Rol del emisor dentro de una conversación. */
@Serializable
enum class MessageStatus {
    /** Enviándose. */
    SENDING,

    /** Entregado al dispositivo del receptor. */
    DELIVERED,

    /** Leído por el receptor. */
    READ
}

/** Tipo de mensaje: texto simple o audio (el diseño ya usa notas de voz). */
@Serializable
enum class MessageKind {
    TEXT, AUDIO
}

/**
 * Mensaje de una conversación.
 *
 * Para audio: [text] actúa como la transcripción/descripción y
 * [audioDurationSeconds] guarda la duración (0 si es texto).
 * [audioFilePath] apunta al archivo físico de la grabación local.
 */
@Serializable
data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val kind: MessageKind = MessageKind.TEXT,
    val audioDurationSeconds: Int = 0,
    val audioFilePath: String? = null,
    val sentAtMillis: Long,
    val status: MessageStatus = MessageStatus.SENDING,
    val isMine: Boolean
)

/** Miembro de un grupo (no se usa en conversaciones privadas de a dos). */
@Serializable
data class GroupMember(
    val userId: String,
    val name: String,
    /** Iniciales para el avatar. */
    val initials: String,
    val isOnline: Boolean = false,
    /** Rol dentro del grupo: ADMIN o MIEMBRO (null si no aplica/se desconoce). */
    val rol: String? = null,
    /** Subruta concreta donde se encuentra este conductor (para etiquetas en la UI). */
    val subruta: String? = null
)

/**
 * Conversación: puede ser privada (a dos) o de grupo.
 *
 * Para privado: [members] tiene 2 entradas (yo y el otro) y
 * [title] es el nombre del otro conductor.
 */
@Serializable
data class Conversation(
    val id: String,
    val title: String,
    val isGroup: Boolean,
    /** Texto del último mensaje para el listado ("Audio · 00:18" o el texto). */
    val preview: String,
    val lastMessageAtMillis: Long,
    val unreadCount: Int,
    val avatarText: String,
    /** Fondo oscuro del avatar (estilo del diseño actual). */
    val avatarDark: Boolean = false,
    /** Id del último mensaje conocido, para detectar actividad nueva al refrescar. */
    val lastMessageId: String? = null,
    val members: List<GroupMember> = emptyList(),
    val photoPath: String? = null,
    /** PRIVADA, GRUPO o GRUPO_RUTA (lo decide el backend, no el título). */
    val tipoConversacion: String? = null,
    /** Subruta concreta cuando [tipoConversacion] es GRUPO_RUTA. */
    val claveRuta: String? = null,
    /** Id del creador del grupo: también puede pedir su eliminación definitiva. */
    val createdBy: String? = null,
    /**
     * Rol del usuario actual dentro del grupo, según el backend. Si es
     * "ADMIN" puede eliminarlo; si no, solo puede salirse.
     */
    val miRol: String? = null,
    /**
     * Hay una eliminación definitiva pedida: el grupo desaparecerá cuando
     * pasen las 72 horas, salvo que alguien la cancele.
     */
    val eliminacionPendiente: Boolean = false,
    /** Cuándo se ejecutará la eliminación definitiva (ISO-8601), si hay una. */
    val eliminacionProgramadaPara: String? = null,
    /**
     * El usuario salió del grupo: ya no es participante. El chat se conserva en
     * su dispositivo (solo lectura) hasta que lo elimine; los demás siguen en el
     * grupo con todo el contenido.
     */
    val hasLeft: Boolean = false
)
