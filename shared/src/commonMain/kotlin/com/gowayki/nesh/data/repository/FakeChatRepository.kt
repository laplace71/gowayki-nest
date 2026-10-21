// Implementación en memoria de ChatRepository.
// Datos simulados: no hay red, pero todo el flujo (enviar, crear grupo,
// leer) funciona y se puede navegar. Cuando haya backend se cambia solo esta
// clase por una que hable con Supabase; la UI no cambia.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.data.repository

import com.gowayki.nesh.app.common.util.currentTimeMillis
import com.gowayki.nesh.app.features.communication.domain.model.Conversation
import com.gowayki.nesh.app.features.communication.domain.model.GroupMember
import com.gowayki.nesh.app.features.communication.domain.model.Message
import com.gowayki.nesh.app.features.communication.domain.model.MessageKind
import com.gowayki.nesh.app.features.communication.domain.model.MessageStatus
import com.gowayki.nesh.app.features.communication.domain.repository.ChatRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Chat simulado en memoria.
 *
 * Simula la latencia de red: al enviar, el mensaje se publica como SENDING y
 * se actualiza a DELIVERED/READ tras una pausa, para que las animaciones
 * se vean como en un chat real.
 */
class FakeChatRepository(
    /** Id del usuario actual (yo). */
    private val myUserId: String = MY_ID,
    private val myName: String = "Carlos Mendoza"
) : ChatRepository {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    private val _messages = MutableStateFlow<Map<String, List<Message>>>(emptyMap())

    /** Colegas que se pueden invitar a un grupo. */
    private val colleagues: List<GroupMember> = listOf(
        GroupMember("u-l12", "Marco Rojas", "MR", isOnline = true),
        GroupMember("u-03", "Ana Quispe", "AQ", isOnline = true),
        GroupMember("u-15", "Luis reparte", "LR", isOnline = false),
        GroupMember("u-21", "Sofía Chávez", "SC", isOnline = true),
        GroupMember("u-09", "Diego Ramos", "DR", isOnline = false)
    )

    init {
        seed()
    }

    override fun conversations(): Flow<List<Conversation>> = _conversations.asStateFlow()

    override fun messages(conversationId: String): Flow<List<Message>> =
        _messages
            .map { all -> all[conversationId].orEmpty() }
            .distinctUntilChanged()

    override suspend fun sendMessage(
        conversationId: String,
        text: String,
        kind: MessageKind,
        audioDurationSeconds: Int
    ): Result<Message> {
        if (text.isBlank() && kind == MessageKind.TEXT) {
            return Result.failure(IllegalArgumentException("El mensaje está vacío"))
        }
        val now = currentTimeMillis()
        val message = Message(
            id = "m-$now",
            conversationId = conversationId,
            senderId = myUserId,
            senderName = myName,
            text = text,
            kind = kind,
            audioDurationSeconds = audioDurationSeconds,
            sentAtMillis = now,
            status = MessageStatus.SENDING,
            isMine = true
        )
        _messages.update { all ->
            all + (conversationId to (all[conversationId].orEmpty() + message))
        }
        updateConversationPreview(conversationId, message)
        // Simula la confirmación del servidor.
        delay(400)
        setStatus(conversationId, message.id, MessageStatus.DELIVERED)
        delay(600)
        setStatus(conversationId, message.id, MessageStatus.READ)
        return Result.success(message)
    }

    override suspend fun markAsRead(conversationId: String): Result<Unit> {
        _conversations.update { list ->
            list.map { if (it.id == conversationId) it.copy(unreadCount = 0) else it }
        }
        // Marca como leídos los mensajes ajenos de esa conversación.
        _messages.update { all ->
            val current = all[conversationId].orEmpty().map { m ->
                if (!m.isMine && m.status != MessageStatus.READ) {
                    m.copy(status = MessageStatus.READ)
                } else m
            }
            all + (conversationId to current)
        }
        return Result.success(Unit)
    }

    override suspend fun createGroup(name: String, members: List<GroupMember>): Result<Conversation> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("El grupo necesita nombre"))
        if (members.isEmpty()) return Result.failure(IllegalArgumentException("Elige al menos un conductor"))
        val now = currentTimeMillis()
        val id = "g-$now"
        val allMembers = listOf(GroupMember(myUserId, myName, "CM", isOnline = true)) + members
        val group = Conversation(
            id = id,
            title = name,
            isGroup = true,
            preview = "Grupo creado",
            lastMessageAtMillis = now,
            unreadCount = 0,
            avatarText = name.take(2).uppercase(),
            avatarDark = false,
            members = allMembers
        )
        _conversations.update { listOf(group) + it }
        _messages.update { it + (id to emptyList()) }
        return Result.success(group)
    }

    override suspend fun openPrivateChat(member: GroupMember): Result<Conversation> {
        _conversations.value.firstOrNull {
            !it.isGroup && it.members.any { m -> m.userId == member.userId }
        }?.let { return Result.success(it) }

        val now = currentTimeMillis()
        val id = "p-$now"
        val chat = Conversation(
            id = id,
            title = member.name,
            isGroup = false,
            preview = "Toca para escribir",
            lastMessageAtMillis = now,
            unreadCount = 0,
            avatarText = member.initials,
            avatarDark = false,
            members = listOf(GroupMember(myUserId, myName, "CM", isOnline = true), member)
        )
        _conversations.update { listOf(chat) + it }
        _messages.update { it + (id to emptyList()) }
        return Result.success(chat)
    }

    override suspend fun markMessageRead(messageId: String): Result<Unit> {
        _messages.update { all ->
            all.mapValues { (_, list) ->
                list.map { if (it.id == messageId && !it.isMine) it.copy(status = MessageStatus.READ) else it }
            }
        }
        return Result.success(Unit)
    }

    override suspend fun clearMessages(conversationId: String): Result<Unit> {
        _messages.update { all -> all + (conversationId to emptyList()) }
        _conversations.update { list ->
            list.map { if (it.id == conversationId) it.copy(preview = "Sin mensajes") else it }
        }
        return Result.success(Unit)
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> {
        _conversations.update { list -> list.filterNot { it.id == conversationId } }
        _messages.update { all -> all - conversationId }
        return Result.success(Unit)
    }

    override suspend fun deleteGroup(conversationId: String): Result<Unit> =
        deleteConversation(conversationId)

    // ── Utilidades internas ──────────────────────────────────────────────

    private fun setStatus(conversationId: String, messageId: String, status: MessageStatus) {
        _messages.update { all ->
            val list = all[conversationId].orEmpty().map {
                if (it.id == messageId) it.copy(status = status) else it
            }
            all + (conversationId to list)
        }
    }

    private fun updateConversationPreview(conversationId: String, message: Message) {
        _conversations.update { list ->
            list.map { c ->
                if (c.id != conversationId) c
                else c.copy(
                    preview = if (message.kind == MessageKind.AUDIO) "Audio" else message.text,
                    lastMessageAtMillis = message.sentAtMillis
                )
            }
        }
    }

    /** Conductores disponibles para crear grupos. */
    fun availableColleagues(): List<GroupMember> = colleagues

    // ── Datos iniciales ──────────────────────────────────────────────────

    private fun seed() {
        val now = currentTimeMillis()
        val marco = colleagues[0]
        val ana = colleagues[1]
        val juan = GroupMember("u-08", "Juan Pérez", "JP", isOnline = true)

        val grupoLinea = Conversation(
            id = "g-linea7",
            title = "GRUPO · Línea 7 · Central",
            isGroup = true,
            preview = "“Tránsito fluido en Av. Dolores. Mantengan distancia.”",
            lastMessageAtMillis = now - 1000,
            unreadCount = 3,
            avatarText = "L7",
            avatarDark = true,
            members = listOf(GroupMember(myUserId, myName, "CM"), marco, ana, juan)
        )
        val privadoMarco = Conversation(
            id = "p-marco",
            title = "Privado · Unidad 03 · Marco",
            isGroup = false,
            preview = "“Confirmo paso por Plaza de Armas.”",
            lastMessageAtMillis = now - 5000,
            unreadCount = 1,
            avatarText = "MR",
            members = listOf(GroupMember(myUserId, myName, "CM"), marco)
        )
        val privadoJefe = Conversation(
            id = "p-jefe",
            title = "Privado · Jefe de línea",
            isGroup = false,
            preview = "“Revisa el cumplimiento de la unidad 08.”",
            lastMessageAtMillis = now - 20000,
            unreadCount = 1,
            avatarText = "JL",
            members = listOf(GroupMember(myUserId, myName, "CM"), GroupMember("jefe", "Jefatura", "JL"))
        )
        val grupoOperativos = Conversation(
            id = "g-operativos",
            title = "GRUPO · Operativos · Línea 7",
            isGroup = true,
            preview = "“Hay operativo por la av...”",
            lastMessageAtMillis = now - 40000,
            unreadCount = 4,
            avatarText = "OP",
            members = listOf(GroupMember(myUserId, myName, "CM"), ana, juan)
        )

        _conversations.value = listOf(grupoLinea, privadoMarco, grupoOperativos, privadoJefe)

        _messages.value = mapOf(
            "g-linea7" to listOf(
                Message("1", "g-linea7", "u-jefe", "Jefatura", "Buenos días equipo. Salida normal 07:45.", sentAtMillis = now - 300000, status = MessageStatus.READ, isMine = false),
                Message("2", "g-linea7", marco.userId, marco.name, "Recibido. Salgo de la central ahora.", sentAtMillis = now - 240000, status = MessageStatus.READ, isMine = false),
                Message("3", "g-linea7", myUserId, myName, "Voy por Av. Dolores, todo fluido.", sentAtMillis = now - 180000, status = MessageStatus.READ, isMine = true),
                Message("4", "g-linea7", ana.userId, ana.name, "Tránsito fluido en Av. Dolores. Mantengan la distancia entre unidades.", sentAtMillis = now - 90000, status = MessageStatus.DELIVERED, isMine = false),
                Message("5", "g-linea7", myUserId, myName, "Entendido, gracias.", sentAtMillis = now - 60000, status = MessageStatus.READ, isMine = true)
            ),
            "p-marco" to listOf(
                Message("6", "p-marco", myUserId, myName, "¿Vas por el paradero del cercado?", sentAtMillis = now - 60000, status = MessageStatus.READ, isMine = true),
                Message("7", "p-marco", marco.userId, marco.name, "Confirmo paso por Plaza de Armas.", sentAtMillis = now - 50000, status = MessageStatus.DELIVERED, isMine = false)
            ),
            "g-operativos" to listOf(
                Message("8", "g-operativos", ana.userId, ana.name, "Hay operativo por la av. Dolores. Estan esperando a las unidades.", sentAtMillis = now - 40000, status = MessageStatus.DELIVERED, isMine = false)
            ),
            "p-jefe" to listOf(
                Message("9", "p-jefe", "jefe", "Jefatura", "Revisa el cumplimiento de la unidad 08.", sentAtMillis = now - 20000, status = MessageStatus.DELIVERED, isMine = false)
            )
        )
    }

    companion object {
        const val MY_ID = "me"
    }
}