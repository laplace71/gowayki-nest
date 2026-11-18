package com.gowayki.nesh.data.repository

import com.gowayki.nesh.app.common.util.currentTimeMillis
import com.gowayki.nesh.app.features.communication.domain.model.Conversation
import com.gowayki.nesh.app.features.communication.domain.model.GroupMember
import com.gowayki.nesh.app.features.communication.domain.model.Message
import com.gowayki.nesh.app.features.communication.domain.model.MessageKind
import com.gowayki.nesh.app.features.communication.domain.model.MessageStatus
import com.gowayki.nesh.app.features.communication.domain.repository.ChatRepository
import com.gowayki.nesh.app.features.map.presentation.SelectedRouteHolder
import com.gowayki.nesh.core.config.AppConfig
import com.gowayki.nesh.core.io.AudioFileUtils
import com.gowayki.nesh.core.network.HttpClientFactory
import com.gowayki.nesh.core.session.UserSession
import com.gowayki.nesh.data.entities.backend.ConversacionResponseDto
import com.gowayki.nesh.data.entities.backend.CrearConversacionRequestDto
import com.gowayki.nesh.data.entities.backend.MensajeResponseDto
import io.ktor.client.plugins.websocket.ws
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class KtorChatRepository : ChatRepository {

    private val chatJson = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    private val _archived = MutableStateFlow<List<Conversation>>(emptyList())
    private val _messages = MutableStateFlow<Map<String, List<Message>>>(emptyMap())

    /**
     * Conversaciones que el usuario eliminó. El backend las borra de verdad, así
     * que el refresco ya no las trae; esta lista solo evita que reaparezcan en
     * los segundos que tarda en llegar el primer refresco o el evento del
     * WebSocket, y queda persistida por si el borrado fue offline.
     */
    private val _deleted = MutableStateFlow<Set<String>>(emptySet())

    private val _colleagues = MutableStateFlow<List<GroupMember>>(emptyList())
    val colleaguesFlow: Flow<List<GroupMember>> = _colleagues.asStateFlow()

    // Conversaciones cuyas pantallas de chat están observando mensajes.
    private val watchedConversations = MutableStateFlow<Set<String>>(emptySet())
    // Control para no crear el grupo de ruta en cada poll.
    @Volatile private var ensuredRouteGroup = false
    @Volatile private var persistPending = false

    init {
        restore()
        refreshConversations(ensureRouteGroup = true)
        startWebSocketListener()
        startPolling()
        observeSelectedRoute()
        fetchColleagues()
    }

    fun availableColleagues(): List<GroupMember> = _colleagues.value

    fun fetchColleagues() {
        val myId = UserSession.currentUserId
        scope.launch {
            BackendChatApi.usuarios().onSuccess { users ->
                val list = users
                    .filter { it.id != myId }
                    .map { u ->
                        val initials = u.nombre
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .joinToString("") { it.first().uppercase() }
                            .ifBlank { "C" }
                        GroupMember(
                            userId = u.id,
                            name = u.nombre,
                            initials = initials,
                            isOnline = u.isOnline
                        )
                    }
                _colleagues.value = list
            }
        }
    }

    private fun getActiveRouteGroupTitle(): String {
        val turnoLinea = com.gowayki.nesh.core.session.TurnoSession.turno?.lineaNombre?.takeIf { it.isNotBlank() }
        val route = SelectedRouteHolder.selected.value
        val name = turnoLinea ?: route?.name?.takeIf { it.isNotBlank() } ?: "Línea C2"
        return "GRUPO · $name"
    }

    private fun observeSelectedRoute() {
        scope.launch {
            SelectedRouteHolder.selected.collect {
                ensuredRouteGroup = false
                refreshConversations(ensureRouteGroup = true)
            }
        }
    }

    fun refreshConversations(ensureRouteGroup: Boolean = false) {
        val myId = UserSession.currentUserId ?: return
        val expectedTitle = getActiveRouteGroupTitle()
        scope.launch {
            BackendChatApi.conversaciones(myId).onSuccess { list ->
                val hasActiveRouteGroup = list.any { it.esGrupo && it.titulo == expectedTitle }
                if (ensureRouteGroup && !ensuredRouteGroup && !hasActiveRouteGroup) {
                    BackendChatApi.crearConversacion(
                        CrearConversacionRequestDto(
                            titulo = expectedTitle,
                            esGrupo = true,
                            createdBy = myId,
                            participanteIds = listOf(myId)
                        )
                    ).onSuccess {
                        ensuredRouteGroup = true
                        BackendChatApi.conversaciones(myId).onSuccess { updatedList ->
                            filterAndEmitConversations(updatedList, expectedTitle)
                        }
                    }.onFailure {
                        filterAndEmitConversations(list, expectedTitle)
                    }
                } else {
                    if (hasActiveRouteGroup) ensuredRouteGroup = true
                    filterAndEmitConversations(list, expectedTitle)
                }
            }
        }
    }

    private fun filterAndEmitConversations(list: List<ConversacionResponseDto>, activeTitle: String) {
        val deleted = _deleted.value
        val filtered = list.filter { dto ->
            when {
                // Grupos que el usuario eliminó: no vuelven aunque sigan en el backend.
                dto.id in deleted -> false
                !dto.esGrupo -> true
                dto.titulo.startsWith("GRUPO · ") -> dto.titulo == activeTitle
                else -> true
            }
        }
        val existingById = (_conversations.value + _archived.value).associateBy { it.id }
        val mapped = filtered.map { dto ->
            val fresh = mapDtoToConversation(dto)
            val existing = existingById[dto.id]
            if (existing != null) {
                val lastLocal = _messages.value[dto.id]?.lastOrNull()?.sentAtMillis ?: 0L
                // Actividad nueva = el backend tiene otro último mensaje que el
                // que ya conocíamos. Solo entonces el chat sube en la lista.
                val hayActividadNueva = dto.ultimoMensaje?.id != null &&
                    dto.ultimoMensaje.id != existing.lastMessageId
                val marcaTiempo = when {
                    lastLocal > existing.lastMessageAtMillis -> lastLocal
                    hayActividadNueva -> currentTimeMillis()
                    else -> existing.lastMessageAtMillis
                }
                fresh.copy(
                    photoPath = existing.photoPath,
                    hiddenFromAll = existing.hiddenFromAll,
                    unreadCount = existing.unreadCount,
                    lastMessageAtMillis = marcaTiempo,
                    lastMessageId = dto.ultimoMensaje?.id ?: existing.lastMessageId,
                    hasLeft = existing.hasLeft
                )
            } else fresh
        }.sortedByDescending { it.lastMessageAtMillis }

        // Los grupos de los que saliste ya no los devuelve el servidor (no eres
        // participante), pero deben seguir visibles en tu dispositivo hasta que
        // los borres tú. Se conservan tal cual estaban guardados localmente.
        val serverIds = mapped.map { it.id }.toSet()
        val keptLeft = _conversations.value.filter { it.hasLeft && it.id !in serverIds && it.id !in deleted }
        val merged = (mapped + keptLeft).sortedByDescending { it.lastMessageAtMillis }

        val prev = _conversations.value
        if (merged != prev) {
            _conversations.value = merged
            persist()
        }
    }

    override fun conversations(): Flow<List<Conversation>> {
        refreshConversations(ensureRouteGroup = false)
        return _conversations.asStateFlow()
    }

    override fun archived(): Flow<List<Conversation>> = _archived.asStateFlow()

    override fun messages(conversationId: String): Flow<List<Message>> {
        fetchMessages(conversationId)
        return _messages
            .map { all -> all[conversationId].orEmpty() }
            .distinctUntilChanged()
            .onStart { watchedConversations.update { it + conversationId } }
            .onCompletion { watchedConversations.update { it - conversationId } }
    }

    private fun fetchMessages(conversationId: String) {
        val myId = UserSession.currentUserId ?: ""
        scope.launch {
            BackendChatApi.mensajes(conversationId).onSuccess { dtos ->
                val prev = _messages.value[conversationId].orEmpty()
                val prevById = prev.associateBy { it.id }
                val list = dtos.map { dto ->
                    val mapped = mapDtoToMessage(dto, myId)
                    val existing = prevById[mapped.id]
                    // Preserva timestamp/estado local estable para evitar parpadeos en cada poll.
                    if (existing != null) mapped.copy(sentAtMillis = existing.sentAtMillis) else mapped
                }
                if (list != prev) {
                    _messages.update { it + (conversationId to list) }
                    // Actualiza preview/timestamp de la conversación desde el último mensaje real.
                    list.lastOrNull()?.let { updateConversationPreview(conversationId, it) }
                    persist()
                }
            }
        }
    }

    private fun startPolling() {
        // Polling de respaldo para lista de chats: intervalo moderado para no cargar la app.
        scope.launch {
            while (true) {
                delay(45_000)
                if (UserSession.currentUserId != null) {
                    refreshConversations(ensureRouteGroup = false)
                }
            }
        }
        // Polling de mensajes en chats abiertos: intervalo corto para sensación de tiempo real
        // (fallback si el WebSocket no entrega eventos).
        scope.launch {
            while (true) {
                delay(2_000)
                val watched = watchedConversations.value
                if (watched.isNotEmpty()) {
                    watched.forEach { fetchMessages(it) }
                }
            }
        }
    }

    override suspend fun sendMessage(
        conversationId: String,
        text: String,
        kind: MessageKind,
        audioDurationSeconds: Int,
        audioFilePath: String?
    ): Result<Message> {
        val myId = UserSession.currentUserId ?: "9239c8be-22e7-4373-a952-e82f41abc308"
        val myName = UserSession.displayName

        if (kind == MessageKind.AUDIO) {
            val bytes = audioFilePath?.let { AudioFileUtils.readBytes(it) }
            if (bytes == null || bytes.isEmpty()) {
                return Result.failure(IllegalArgumentException("No se pudo leer el archivo de audio"))
            }

            val apiResult = BackendChatApi.enviarAudio(
                conversacionId = conversationId,
                emisorId = myId,
                audioBytes = bytes,
                duracionSegundos = audioDurationSeconds
            )

            return apiResult.fold(
                onSuccess = { dto ->
                    val fullAudioUrl = dto.audioUrl?.let { url ->
                        if (url.startsWith("http")) url
                        else "${AppConfig.BACKEND_URL.removeSuffix("/api")}$url"
                    } ?: audioFilePath

                    val message = Message(
                        id = dto.id,
                        conversationId = conversationId,
                        senderId = myId,
                        senderName = myName,
                        text = text,
                        kind = MessageKind.AUDIO,
                        audioDurationSeconds = audioDurationSeconds,
                        audioFilePath = fullAudioUrl,
                        sentAtMillis = currentTimeMillis(),
                        status = MessageStatus.DELIVERED,
                        isMine = true
                    )
                    _messages.update { all ->
                        all + (conversationId to (all[conversationId].orEmpty() + message))
                    }
                    updateConversationPreview(conversationId, message)
                    persist()
                    Result.success(message)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } else {
            if (text.isBlank()) {
                return Result.failure(IllegalArgumentException("El mensaje está vacío"))
            }

            val apiResult = BackendChatApi.enviarTexto(
                conversacionId = conversationId,
                emisorId = myId,
                contenido = text
            )

            return apiResult.fold(
                onSuccess = { dto ->
                    val message = Message(
                        id = dto.id,
                        conversationId = conversationId,
                        senderId = myId,
                        senderName = myName,
                        text = text,
                        kind = MessageKind.TEXT,
                        sentAtMillis = currentTimeMillis(),
                        status = MessageStatus.DELIVERED,
                        isMine = true
                    )
                    _messages.update { all ->
                        all + (conversationId to (all[conversationId].orEmpty() + message))
                    }
                    updateConversationPreview(conversationId, message)
                    persist()
                    Result.success(message)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        }
    }

    override suspend fun markAsRead(conversationId: String): Result<Unit> {
        _conversations.update { list ->
            list.map { if (it.id == conversationId) it.copy(unreadCount = 0) else it }
        }
        _messages.update { all ->
            val current = all[conversationId].orEmpty().map { m ->
                if (!m.isMine && m.status != MessageStatus.READ) {
                    m.copy(status = MessageStatus.READ)
                } else m
            }
            all + (conversationId to current)
        }
        persist()
        return Result.success(Unit)
    }

    override suspend fun createGroup(name: String, members: List<GroupMember>): Result<Conversation> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("El grupo necesita nombre"))
        val myId = UserSession.currentUserId ?: "9239c8be-22e7-4373-a952-e82f41abc308"

        val request = CrearConversacionRequestDto(
            titulo = name,
            esGrupo = true,
            createdBy = myId,
            participanteIds = listOf(myId) + members.map { it.userId }
        )

        return BackendChatApi.crearConversacion(request).map { dto ->
            val group = mapDtoToConversation(dto)
            _deleted.update { it - group.id }
            _conversations.update { listOf(group) + it }
            _messages.update { it + (group.id to emptyList()) }
            persist()
            group
        }
    }

    override suspend fun openPrivateChat(member: GroupMember): Result<Conversation> {
        _conversations.value.firstOrNull {
            !it.isGroup && it.title == member.name
        }?.let { return Result.success(it) }

        val myId = UserSession.currentUserId ?: "9239c8be-22e7-4373-a952-e82f41abc308"
        val request = CrearConversacionRequestDto(
            titulo = member.name,
            esGrupo = false,
            createdBy = myId,
            participanteIds = listOf(myId, member.userId)
        )

        return BackendChatApi.crearConversacion(request).map { dto ->
            val chat = mapDtoToConversation(dto)
            _deleted.update { it - chat.id }
            _conversations.update { listOf(chat) + it }
            _messages.update { it + (chat.id to emptyList()) }
            persist()
            chat
        }
    }

    override suspend fun markMessageRead(messageId: String): Result<Unit> {
        _messages.update { all ->
            all.mapValues { (_, list) ->
                list.map { if (it.id == messageId && !it.isMine) it.copy(status = MessageStatus.READ) else it }
            }
        }
        persist()
        return Result.success(Unit)
    }

    override suspend fun clearMessages(conversationId: String): Result<Unit> {
        _messages.update { all -> all + (conversationId to emptyList()) }
        _conversations.update { list ->
            list.map { if (it.id == conversationId) it.copy(preview = "Sin mensajes") else it }
        }
        persist()
        return Result.success(Unit)
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> {
        val conv = _conversations.value.firstOrNull { it.id == conversationId }
            ?: _archived.value.firstOrNull { it.id == conversationId }

        if (conv?.isGroup == true) {
            if (conv.hasLeft) {
                // Ya saliste del grupo: el servidor ya no te tiene como participante.
                // Borrar aquí es solo quitar la copia local (para ti), sin afectar a nadie.
                _deleted.update { it + conversationId }
                _conversations.update { list -> list.filterNot { it.id == conversationId } }
                _archived.update { list -> list.filterNot { it.id == conversationId } }
                _messages.update { all -> all - conversationId }
            } else {
                // Grupo activo: "eliminar chat" lo oculta de "Todos" pero sigue
                // disponible en "Grupos" (el grupo y sus miembros no se tocan).
                _messages.update { all -> all + (conversationId to emptyList()) }
                _conversations.update { list ->
                    list.map { c ->
                        if (c.id == conversationId) c.copy(preview = "Sin mensajes", lastMessageAtMillis = 0L, unreadCount = 0, hiddenFromAll = true)
                        else c
                    }
                }
                _archived.update { list ->
                    list.map { c ->
                        if (c.id == conversationId) c.copy(preview = "Sin mensajes", lastMessageAtMillis = 0L, unreadCount = 0, hiddenFromAll = true)
                        else c
                    }
                }
            }
        } else {
            // Chat privado: se elimina de verdad en el backend, no solo en el teléfono.
            val myId = UserSession.currentUserId
                ?: return Result.failure(IllegalStateException("Sesión sin usuario"))
            BackendChatApi.eliminarConversacion(conversationId, myId).getOrElse { return Result.failure(it) }

            _deleted.update { it + conversationId }
            _conversations.update { list -> list.filterNot { it.id == conversationId } }
            _archived.update { list -> list.filterNot { it.id == conversationId } }
            _messages.update { all -> all - conversationId }
        }
        persist()
        return Result.success(Unit)
    }

    override suspend fun archiveConversation(conversationId: String): Result<Unit> {
        val idx = _conversations.value.indexOfFirst { it.id == conversationId }
        if (idx >= 0) {
            val c = _conversations.value[idx]
            _conversations.update { it.filterNot { x -> x.id == conversationId } }
            _archived.update { listOf(c) + it }
            persist()
        }
        return Result.success(Unit)
    }

    override suspend fun unarchiveConversation(conversationId: String): Result<Unit> {
        val idx = _archived.value.indexOfFirst { it.id == conversationId }
        if (idx >= 0) {
            val c = _archived.value[idx]
            _archived.update { it.filterNot { x -> x.id == conversationId } }
            _conversations.update { listOf(c.copy(hiddenFromAll = false)) + it }
            persist()
        }
        return Result.success(Unit)
    }

    override suspend fun updateConversationPhoto(conversationId: String, path: String?): Result<Unit> {
        _conversations.update { list ->
            list.map { if (it.id == conversationId) it.copy(photoPath = path) else it }
        }
        _archived.update { list ->
            list.map { if (it.id == conversationId) it.copy(photoPath = path) else it }
        }
        persist()
        return Result.success(Unit)
    }

    override suspend fun deleteMessage(conversationId: String, messageId: String, deleteForEveryone: Boolean): Result<Unit> {
        _messages.update { all ->
            val current = all[conversationId].orEmpty().filterNot { m ->
                if (deleteForEveryone) m.id == messageId else (m.id == messageId && m.isMine)
            }
            all + (conversationId to current)
        }
        persist()
        return Result.success(Unit)
    }

    override suspend fun leaveGroup(conversationId: String): Result<Unit> {
        // El backend solo quita mi fila de participante: el grupo sigue existiendo
        // para los demás. Si falla, no se toca la lista.
        val myId = UserSession.currentUserId
            ?: return Result.failure(IllegalStateException("Sesión sin usuario"))
        BackendChatApi.salirConversacion(conversationId, myId).getOrElse { return Result.failure(it) }

        // Como en WhatsApp: al salir sigues viendo el chat (solo lectura) hasta
        // que lo borres tú. Los demás conservan todo porque siguen en el grupo.
        _conversations.update { list ->
            list.map { if (it.id == conversationId) it.copy(hasLeft = true, unreadCount = 0, miRol = null) else it }
        }
        _archived.update { list ->
            list.map { if (it.id == conversationId) it.copy(hasLeft = true, unreadCount = 0, miRol = null) else it }
        }
        persist()
        return Result.success(Unit)
    }

    override suspend fun deleteGroup(conversationId: String): Result<Unit> {
        // Se pide el borrado real al backend. Si falla, no se toca la lista:
        // el grupo sigue visible antes que parecer borrado y no estarlo.
        val myId = UserSession.currentUserId
            ?: return Result.failure(IllegalStateException("Sesión sin usuario"))
        BackendChatApi.eliminarConversacion(conversationId, myId).getOrElse { return Result.failure(it) }

        _deleted.update { it + conversationId }
        _conversations.update { list -> list.filterNot { it.id == conversationId } }
        _archived.update { list -> list.filterNot { it.id == conversationId } }
        _messages.update { all -> all - conversationId }
        persist()
        return Result.success(Unit)
    }

    private fun mapDtoToMessage(dto: MensajeResponseDto, myId: String): Message {
        val isMine = dto.emisorId == myId
        val fullUrl = dto.audioUrl?.let { url ->
            if (url.startsWith("http")) url
            else "${AppConfig.BACKEND_URL.removeSuffix("/api")}$url"
        }
        return Message(
            id = dto.id,
            conversationId = dto.conversacionId,
            senderId = dto.emisorId,
            senderName = dto.emisorNombre ?: if (isMine) UserSession.displayName else "Conductor",
            text = dto.contenidoTexto ?: if (dto.tipo == "AUDIO") "Nota de voz" else "",
            kind = if (dto.tipo == "AUDIO") MessageKind.AUDIO else MessageKind.TEXT,
            audioDurationSeconds = dto.audioDuracionSegundos ?: 0,
            audioFilePath = fullUrl,
            sentAtMillis = currentTimeMillis(),
            status = when (dto.estado) {
                "LEIDO" -> MessageStatus.READ
                "ENTREGADO" -> MessageStatus.DELIVERED
                else -> MessageStatus.DELIVERED
            },
            isMine = isMine
        )
    }

    private fun mapDtoToConversation(dto: ConversacionResponseDto): Conversation {
        val previewText = when {
            dto.ultimoMensaje?.tipo == "AUDIO" -> "Audio · ${dto.ultimoMensaje.audioDuracionSegundos ?: 0}s"
            dto.ultimoMensaje?.contenidoTexto != null -> dto.ultimoMensaje.contenidoTexto
            else -> "Sin mensajes"
        }
        val count = (dto.totalParticipantes ?: 1).coerceAtLeast(1)
        val membersList = if (dto.esGrupo) {
            List(count) { i ->
                GroupMember("u-$i", "Conductor ${i + 1}", "C", isOnline = true)
            }
        } else {
            emptyList()
        }
        return Conversation(
            id = dto.id,
            title = dto.titulo,
            isGroup = dto.esGrupo,
            preview = previewText,
            lastMessageAtMillis = currentTimeMillis(),
            unreadCount = 0,
            avatarText = dto.titulo.take(2).uppercase(),
            avatarDark = dto.esGrupo,
            lastMessageId = dto.ultimoMensaje?.id,
            members = membersList,
            hiddenFromAll = false,
            miRol = dto.miRol
        )
    }

    private fun updateConversationPreview(conversationId: String, message: Message) {
        _conversations.update { list ->
            list.map { c ->
                if (c.id != conversationId) c
                else c.copy(
                    preview = if (message.kind == MessageKind.AUDIO) "Audio" else message.text,
                    lastMessageAtMillis = message.sentAtMillis,
                    lastMessageId = message.id,
                    hiddenFromAll = false
                )
            }
        }
    }

    private fun startWebSocketListener() {
        scope.launch {
            while (true) {
                try {
                    val client = HttpClientFactory.wsClient
                    val wsUrl = HttpClientFactory.wsChatUrl
                    client.ws(urlString = wsUrl) {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                handleIncomingWebSocketMessage(text)
                            }
                        }
                    }
                } catch (_: Throwable) {
                }
                delay(5000)
            }
        }
    }

    private fun handleIncomingWebSocketMessage(text: String) {
        // El backend avisa cuando otro dispositivo eliminó una conversación.
        val evento = runCatching { chatJson.parseToJsonElement(text).jsonObject }.getOrNull()
        val tipo = evento?.get("tipo")?.jsonPrimitive?.content
        if (tipo == "CONVERSACION_ELIMINADA") {
            val id = evento["conversacionId"]?.jsonPrimitive?.content
            if (!id.isNullOrBlank()) {
                _deleted.update { it + id }
                _conversations.update { list -> list.filterNot { it.id == id } }
                _archived.update { list -> list.filterNot { it.id == id } }
                _messages.update { all -> all - id }
            }
            return
        }

        val dto = runCatching { chatJson.decodeFromString<MensajeResponseDto>(text) }.getOrNull()
        if (dto != null) {
            val myId = UserSession.currentUserId ?: ""
            val message = mapDtoToMessage(dto, myId)

            var appended = false
            _messages.update { all ->
                val existing = all[message.conversationId].orEmpty()
                if (existing.any { it.id == message.id }) {
                    all
                } else {
                    appended = true
                    all + (message.conversationId to (existing + message))
                }
            }
            if (appended) {
                updateConversationPreview(message.conversationId, message)
                // Si no es mi mensaje y nadie está viendo ese chat, marcar como no leído.
                if (!message.isMine && !watchedConversations.value.contains(message.conversationId)) {
                    _conversations.update { list ->
                        list.map { c ->
                            if (c.id == message.conversationId) c.copy(unreadCount = c.unreadCount + 1) else c
                        }
                    }
                }
                if (_conversations.value.none { it.id == message.conversationId } &&
                    _archived.value.none { it.id == message.conversationId }
                ) {
                    refreshConversations()
                }
                persist()
            }
            return
        }

        val convDto = runCatching { chatJson.decodeFromString<ConversacionResponseDto>(text) }.getOrNull()
        if (convDto != null) {
            refreshConversations()
            return
        }

        val posDto = runCatching { chatJson.decodeFromString<com.gowayki.nesh.data.entities.backend.PosicionGpsDto>(text) }.getOrNull()
        if (posDto != null && posDto.unidadId != null) {
            com.gowayki.nesh.app.features.map.data.ActiveDriversStore.updateRemoteDriver(
                posDto,
                com.gowayki.nesh.core.session.TurnoSession.turno?.unidadId
            )
            return
        }
    }

    private fun restore(): Boolean = runCatching {
        val raw = ChatStore.read() ?: return false
        val saved = chatJson.decodeFromString<PersistedChatState>(raw)
        _conversations.value = saved.conversations
        _archived.value = saved.archived
        _messages.value = saved.messages
        _deleted.value = saved.deleted
        true
    }.getOrDefault(false)

    private fun persist() {
        // Debounce: coalesces rapid updates (p. ej. ráfaga de mensajes WS) en
        // una sola serialización/escritura para no bloquear con JSON grande.
        if (persistPending) return
        persistPending = true
        scope.launch {
            delay(600)
            persistPending = false
            runCatching {
                ChatStore.write(
                    chatJson.encodeToString(
                        PersistedChatState(
                            _conversations.value,
                            _archived.value,
                            _messages.value,
                            _deleted.value
                        )
                    )
                )
            }
        }
    }

    @Serializable
    private data class PersistedChatState(
        val conversations: List<Conversation>,
        val archived: List<Conversation> = emptyList(),
        val messages: Map<String, List<Message>>,
        /** Conversaciones eliminadas: el backend no las borra, se filtran aquí. */
        val deleted: Set<String> = emptySet()
    )
}

