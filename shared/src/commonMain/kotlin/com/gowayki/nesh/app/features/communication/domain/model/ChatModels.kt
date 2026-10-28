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
    val isOnline: Boolean = false
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
    val members: List<GroupMember> = emptyList()
)
