package com.gowayki.nesh.data.repository

import com.gowayki.nesh.app.common.util.currentTimeMillis
import com.gowayki.nesh.app.common.util.parseIsoToMillis
import com.gowayki.nesh.app.features.communication.domain.model.Conversation
import com.gowayki.nesh.app.features.communication.domain.model.GroupMember
import com.gowayki.nesh.app.features.communication.domain.model.Message
import com.gowayki.nesh.app.features.communication.domain.model.MessageKind
import com.gowayki.nesh.app.features.communication.domain.model.MessageStatus
import com.gowayki.nesh.app.features.communication.domain.repository.ChatRepository
import com.gowayki.nesh.app.features.map.presentation.SelectedRouteHolder
import com.gowayki.nesh.app.features.map.presentation.SubrouteLabels
import com.gowayki.nesh.core.config.AppConfig
import com.gowayki.nesh.core.io.AudioFileUtils
import com.gowayki.nesh.core.network.HttpClientFactory
import com.gowayki.nesh.core.session.UserSession
import com.gowayki.nesh.data.entities.backend.ConversacionResponseDto
import com.gowayki.nesh.data.entities.backend.CrearConversacionRequestDto
import com.gowayki.nesh.data.entities.backend.MensajeResponseDto
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
     * Conversaciones que el usuario ocultó en ESTE dispositivo.
     *
     * El backend es la fuente de verdad sobre la visibilidad (él ya no las
     * devuelve), pero no expone un listado de ocultas: para poder restaurar una
     * hay que recordar aquí su título. Ocultar no borra mensajes ni adhesivos.
     */
    private val _hidden = MutableStateFlow<List<Conversation>>(emptyList())

    /**
     * Grupos con eliminación pedida: desaparecen de las listas normales, pero
     * se conservan aquí para poder cancelar la purga antes de las 72 horas.
     */
    private val _eliminacionesPendientes = MutableStateFlow<List<Conversation>>(emptyList())
    override fun eliminacionesPendientes(): Flow<List<Conversation>> = _eliminacionesPendientes.asStateFlow()

    private val _colleagues = MutableStateFlow<List<GroupMember>>(emptyList())
    val colleaguesFlow: Flow<List<GroupMember>> = _colleagues.asStateFlow()

    /**
     * Subruta asignada localmente a los grupos creados a mano (y a los grupos
     * "globales" heredados). El backend no siempre devuelve `claveRuta` en un
     * GRUPO normal, así que lo recordamos aquí para que dejen de aparecer en
     * todas las subrutas.
     */
    private val manualGroupSubroutes = mutableMapOf<String, String>()

    /**
     * Marca de tiempo local de los chats "limpiados" (regla 1: solo visual).
     * Los mensajes con fecha anterior a esta marca no se muestran en este
     * dispositivo; si llega uno nuevo, el chat reaparece con solo ese mensaje.
     */
    private val chatsLimpiados = mutableMapOf<String, Long>()

    /**
     * Ids de mensajes borrados en este dispositivo. El backend no tiene borrado
     * por mensaje: el texto/audio sigue en la BD hasta que se elimina el grupo
     * completo (plazo de 72 h). Guardamos esta "lápida" localmente y la aplicamos
     * en cada carga para que el polling y el WebSocket no lo traigan de vuelta.
     */
    private val mensajesBorrados = mutableSetOf<String>()

    // Conversaciones cuyas pantallas de chat están observando mensajes.
    private val watchedConversations = MutableStateFlow<Set<String>>(emptySet())
    // Subruta cuyo grupo de ruta ya se aseguró en el backend (entrar = unirse).
    @Volatile private var joinedRouteClave: String? = null
    @Volatile private var persistPending = false
    private val joinMutex = Mutex()
    private val fetchMutex = Mutex()
    private val fetchingMessages = mutableSetOf<String>()

    init {
        restore()
        refreshConversations(ensureRouteGroup = true)
        startWebSocketListener()
        startPolling()
        observeSelectedRoute()
        fetchColleagues()
    }

    fun availableColleagues(): List<GroupMember> = _colleagues.value

    override fun activeSubrouteLabel(): String? = getActiveConcreteSubroute()?.name?.trim()

    /**
     * Clave de subruta efectiva de una conversación del backend. Si el grupo es
     * manual y no trae clave, lo anclamos a la subruta activa (una sola vez) para
     * que no se muestre en las demás. Los chats privados nunca se anclan.
     */
    private fun effectiveClaveRuta(dto: ConversacionResponseDto): String? {
        dto.claveRuta?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        if (!dto.esGrupo) return null
        manualGroupSubroutes[dto.id]?.let { return it }
        val clave = getActiveConcreteSubroute()?.let(::routeGroupKey) ?: return null
        manualGroupSubroutes[dto.id] = clave
        persist()
        return clave
    }

    fun fetchColleagues() {
        val myId = UserSession.currentUserId ?: return
        scope.launch {
            val route = getActiveConcreteSubroute() ?: run {
                _colleagues.value = emptyList()
                return@launch
            }
            val byId = linkedMapOf<String, GroupMember>()

            // 1) Turnos activos: fuente simétrica. Todos los choferes consultan
            //    /turnos/activos, así que quien está en mi subruta me ve y yo lo veo,
            //    aunque todavía no haya participantes en el grupo de ruta.
            com.gowayki.nesh.core.session.TurnoSession.refreshActiveTurnos()
            com.gowayki.nesh.core.session.TurnoSession.companerosEnSubruta(route)
                .forEach { t ->
                    val userId = t.choferId!!
                    val nombre = t.choferNombre?.trim()?.takeIf { it.isNotBlank() }
                        ?: "Conductor${t.numeroPadron?.let { " · Unidad $it" } ?: ""}"
                    val initials = nombre
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercase() }
                        .ifBlank { "C" }
                    byId[userId] = GroupMember(
                        userId = userId,
                        name = nombre,
                        initials = initials,
                        isOnline = true,
                        subruta = route.name.trim()
                    )
                }

            // 2) Participantes del grupo automático de ruta (no de un grupo
            //    manual creado dentro de ella). Sobrescribe para conservar rol
            //    y estado reales del backend.
            var convId = (_conversations.value + _archived.value)
                .firstOrNull { it.tipoConversacion == "GRUPO_RUTA" && isSameRouteGroup(it, route) }
                ?.id ?: (_conversations.value + _archived.value)
                .firstOrNull { isSameRouteGroup(it, route) }
                ?.id

            if (convId == null) {
                BackendChatApi.conversaciones().onSuccess { list ->
                    convId = list.firstOrNull { dto ->
                        dto.tipoConversacion == "GRUPO_RUTA" &&
                            isRouteGroupDto(dto) && matchesRouteGroup(effectiveClaveRuta(dto), dto.titulo, route)
                    }?.id ?: list.firstOrNull { dto ->
                        isRouteGroupDto(dto) && matchesRouteGroup(effectiveClaveRuta(dto), dto.titulo, route)
                    }?.id
                }
            }

            val id = convId
            if (id != null) {
                BackendChatApi.participantes(id).onSuccess { parts ->
                    parts
                        .filter { it.userId != myId }
                        .forEach { u ->
                            val nombre = u.nombre ?: "Conductor"
                            val initials = nombre
                                .split(" ")
                                .filter { it.isNotBlank() }
                                .take(2)
                                .joinToString("") { it.first().uppercase() }
                                .ifBlank { "C" }
                            byId[u.userId] = GroupMember(
                                userId = u.userId,
                                name = nombre,
                                initials = initials,
                                isOnline = u.isOnline == true,
                                rol = u.rol,
                                subruta = route.name.trim()
                            )
                        }
                }
            }

            _colleagues.value = byId.values.toList()
        }
    }

    /**
     * Comprueba si una cadena representa un código de línea principal (ej. C2, C11, Línea C2, Canal de Línea C3).
     * En este sistema las líneas NO tienen chats: solo las subrutas concretas.
     */
    private val lineCodeRegex = Regex("""^(?:l[ií]nea\s+|canal\s+de\s+(?:l[ií]nea\s+)?)?c(?:10|11|[0-9]+)$""", RegexOption.IGNORE_CASE)

    private fun isLineCode(value: String?): Boolean {
        val clean = value?.trim().orEmpty()
        if (clean.isEmpty()) return false
        if (lineCodeRegex.matches(clean)) return true
        if (Regex("""^(?:l[ií]nea|linea)\s*\d+$""", RegexOption.IGNORE_CASE).matches(clean)) return true
        if (Regex("""^(?:l[ií]nea|linea)\s*c\d+$""", RegexOption.IGNORE_CASE).matches(clean)) return true
        if (clean.equals("general", ignoreCase = true)) return true
        return false
    }

    private fun isLineLevelChat(titulo: String?, claveRuta: String?): Boolean {
        if (isLineCode(titulo) || isLineCode(claveRuta)) return true
        val t = titulo?.trim().orEmpty()
        val c = claveRuta?.trim().orEmpty()
        if (t.startsWith("Línea C", ignoreCase = true) || t.startsWith("Linea C", ignoreCase = true)) return true
        if (t.startsWith("Canal de Línea", ignoreCase = true) || t.startsWith("Canal de Linea", ignoreCase = true)) return true
        if (c.startsWith("c", ignoreCase = true) && c.drop(1).all { it.isDigit() }) return true
        return false
    }

    /**
     * Subruta en la que el chofer se encuentra activamente.
     * C2…C11 no cuentan: dos personas solo se encuentran si entraron a la MISMA subruta.
     */
    private fun getActiveConcreteSubroute(): com.gowayki.nesh.app.features.map.domain.model.CombiRoute? {
        // 1. Prioridad: subruta concreta del turno activo del chofer (asignación oficial)
        val fromTurno = exactSubrouteFromTurno()
        if (fromTurno != null) return fromTurno

        // 2. Si el chofer eligió/entró manualmente a una subruta concreta en la app
        val selected = SelectedRouteHolder.selected.value
        if (isConcreteSubroute(selected)) return selected

        return null
    }

    private fun isConcreteSubroute(route: com.gowayki.nesh.app.features.map.domain.model.CombiRoute?): Boolean {
        if (route == null || !route.isSub) return false
        if (route.id.isBlank() || isLineCode(route.id)) return false
        if (isLineCode(route.name)) return false
        return true
    }

    /** Solo el código de subruta guardado en el turno. Nunca una línea general. */
    private fun exactSubrouteFromTurno(): com.gowayki.nesh.app.features.map.domain.model.CombiRoute? {
        val turno = com.gowayki.nesh.core.session.TurnoSession.turno ?: return null
        val code = turno.subrutaCodigo?.trim()?.takeIf { it.isNotBlank() && !isLineCode(it) }
        val name = turno.subrutaNombre?.trim()?.takeIf { it.isNotBlank() && !isLineCode(it) }

        if (code == null && name == null) return null

        val catalog = SelectedRouteHolder.catalog.value
        val allRoutes = (catalog + com.gowayki.nesh.app.features.map.domain.model.ArequipaRoutes.all)
        val matched = allRoutes.firstOrNull { it.isSub && (code != null && (it.id.equals(code, ignoreCase = true) || it.routeId == code)) }
            ?: allRoutes.firstOrNull { it.isSub && (name != null && it.name.equals(name, ignoreCase = true)) }

        if (matched != null) return matched

        // Si no está aún en el catálogo local, sintetiza la subruta con el código/nombre oficial del turno
        val fallbackId = code ?: "SUB-$name"
        val fallbackName = name ?: code ?: "Subruta"
        return com.gowayki.nesh.app.features.map.domain.model.CombiRoute(
            id = fallbackId,
            name = fallbackName,
            operator = turno.lineaNombre ?: "",
            color = androidx.compose.ui.graphics.Color(0xFFE53935),
            waypoints = emptyList(),
            isSub = true,
            parent = null
        )
    }

    /** Clave estable del grupo: el id de la subruta (SB-…), nunca C2…C11. */
    private fun routeGroupKey(route: com.gowayki.nesh.app.features.map.domain.model.CombiRoute): String =
        route.id.trim()

    /** Título legible del grupo de esa subruta. */
    private fun routeGroupTitle(route: com.gowayki.nesh.app.features.map.domain.model.CombiRoute): String =
        "GRUPO · ${route.name.trim()}"

    private fun matchesRouteGroup(
        claveRuta: String?,
        titulo: String?,
        route: com.gowayki.nesh.app.features.map.domain.model.CombiRoute
    ): Boolean {
        if (isLineLevelChat(titulo, claveRuta)) return false
        val key = routeGroupKey(route)
        val keyNorm = key.removePrefix("SB-").removePrefix("sb-").trim()

        if (!claveRuta.isNullOrBlank()) {
            val c = claveRuta.trim()
            val cNorm = c.removePrefix("SB-").removePrefix("sb-").trim()
            if (c.equals(key, ignoreCase = true) || (cNorm.isNotEmpty() && cNorm.equals(keyNorm, ignoreCase = true))) {
                return true
            }
            if (c.equals(route.name.trim(), ignoreCase = true)) return true
            if (route.routeId != null && c.equals(route.routeId, ignoreCase = true)) return true
        }

        val expectedTitle = routeGroupTitle(route)
        if (!titulo.isNullOrBlank() && titulo.trim().equals(expectedTitle, ignoreCase = true)) {
            return true
        }
        val cleanTitle = titulo?.removePrefix("GRUPO · ")?.removePrefix("GRUPO - ")?.trim()
        if (!cleanTitle.isNullOrBlank() && cleanTitle.equals(route.name.trim(), ignoreCase = true)) {
            return true
        }
        return false
    }

    private fun isSameRouteGroup(
        conv: Conversation,
        route: com.gowayki.nesh.app.features.map.domain.model.CombiRoute
    ): Boolean = isRouteGroup(conv) && matchesRouteGroup(conv.claveRuta, conv.title, route)

    private fun isRouteGroup(conv: Conversation): Boolean =
        isLineLevelChat(conv.title, conv.claveRuta) ||
        conv.tipoConversacion == "GRUPO_RUTA" ||
        (conv.isGroup && (conv.title.startsWith("GRUPO · ") || conv.title.startsWith("GRUPO - "))) ||
        !conv.claveRuta.isNullOrBlank()

    private fun isRouteGroupDto(dto: ConversacionResponseDto): Boolean =
        isLineLevelChat(dto.titulo, dto.claveRuta) ||
        dto.tipoConversacion == "GRUPO_RUTA" ||
        (dto.esGrupo && (dto.titulo.startsWith("GRUPO · ") || dto.titulo.startsWith("GRUPO - "))) ||
        !dto.claveRuta.isNullOrBlank()

    /** Un grupo de otra subruta (o de una línea) no debe verse en este dispositivo. */
    private fun belongsOnThisDevice(conv: Conversation, active: com.gowayki.nesh.app.features.map.domain.model.CombiRoute?): Boolean {
        if (isLineLevelChat(conv.title, conv.claveRuta)) return false
        if (!isRouteGroup(conv)) return true
        if (active == null) return false
        return matchesRouteGroup(conv.claveRuta, conv.title, active)
    }

    private fun observeSelectedRoute() {
        scope.launch {
            var lastId: String? = null
            SelectedRouteHolder.selected.collect { route ->
                val id = route?.id
                if (id == lastId) return@collect
                lastId = id
                joinedRouteClave = null
                refreshConversations(ensureRouteGroup = true)
                fetchColleagues()
            }
        }
    }

    fun refreshConversations(ensureRouteGroup: Boolean = false) {
        val myId = UserSession.currentUserId ?: return
        scope.launch {
            val activeRoute = getActiveConcreteSubroute()
            val clave = activeRoute?.let { routeGroupKey(it) }
            // Entrar a la subruta une al grupo de esa clave. Quien ya está ahí
            // se reencuentra; el backend saca al chofer de las otras subrutas.
            if (ensureRouteGroup && activeRoute != null && clave != null && joinedRouteClave != clave) {
                joinMutex.withLock {
                    if (joinedRouteClave != clave) {
                        val joined = BackendChatApi.crearConversacion(
                            CrearConversacionRequestDto(
                                titulo = routeGroupTitle(activeRoute),
                                esGrupo = true,
                                tipoConversacion = "GRUPO_RUTA",
                                claveRuta = clave,
                                createdBy = myId,
                                participanteIds = listOf(myId)
                            )
                        )
                        if (joined.isSuccess) joinedRouteClave = clave
                    }
                }
            } else if (clave == null) {
                joinedRouteClave = null
            }

            BackendChatApi.conversaciones().onSuccess { list ->
                filterAndEmitConversations(list)
                if (ensureRouteGroup && clave != null) fetchColleagues()
            }
        }
    }

    /**
     * Si la app estaba apagada cuando venció el plazo, pudo perderse el aviso
     * WebSocket. Anclamos la purga local a la fecha devuelta por el backend para
     * que el grupo tampoco aparezca en Archivados/Ocultos.
     */
    private fun purgeEliminacionesVencidas() {
        val ahora = currentTimeMillis()
        val vencidas = (_conversations.value + _archived.value + _hidden.value)
            .filter { it.eliminacionPendiente }
            .filter { conv ->
                val programada = conv.eliminacionProgramadaPara?.let(::parseIsoToMillis)
                programada != null && programada <= ahora
            }
            .map { it.id }
            .toSet()
        if (vencidas.isEmpty()) return
        vencidas.forEach(::quitarConversacionLocal)
        manualGroupSubroutes.keys.removeAll(vencidas)
        persist()
    }

    private fun filterAndEmitConversations(list: List<ConversacionResponseDto>) {
        purgeEliminacionesVencidas()
        // El backend ya excluye las conversaciones que el usuario ocultó.
        // Los grupos de ruta de otra subruta tampoco se muestran: cada subruta
        // es un chat aparte. C2…C11 no agrupan a nadie.
        val activeRoute = getActiveConcreteSubroute()
        val filtered = list.filter { dto ->
            // Regla 1: un chat limpiado sigue oculto mientras no llegue un
            // mensaje nuevo posterior a la marca local. La marca se conserva
            // para que fetchMessages siga ocultando los mensajes antiguos.
            val corte = chatsLimpiados[dto.id]
            if (corte != null) {
                val ultimo = parseIsoToMillis(dto.ultimoMensaje?.createdAt)
                if (ultimo == null || ultimo <= corte) return@filter false
            }
            val clave = effectiveClaveRuta(dto)
            // Descarta completamente cualquier chat de línea (C2...C11)
            if (isLineLevelChat(dto.titulo, clave)) {
                return@filter false
            }
            if (isRouteGroupDto(dto.copy(claveRuta = clave))) {
                // Solo se muestra el grupo de ruta si coincide exactamente con la subruta activa del usuario.
                // Los grupos manuales también quedan anclados a una subruta (ver effectiveClaveRuta),
                // así que dejan de aparecer en todas. Si dos personas están en la misma
                // subruta, ambas lo ven y se encuentran.
                activeRoute != null && matchesRouteGroup(clave, dto.titulo, activeRoute)
            } else {
                // Chats privados (nunca se anclan a una subruta)
                true
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
                    unreadCount = existing.unreadCount,
                    lastMessageAtMillis = marcaTiempo,
                    lastMessageId = dto.ultimoMensaje?.id ?: existing.lastMessageId,
                    // Si el servidor devuelve la conversación es porque sigues participando;
                    // al volver a entrar a una subruta se limpia el "saliste".
                    hasLeft = false
                )
            } else fresh
        }.sortedByDescending { it.lastMessageAtMillis }

        // Los grupos de los que saliste ya no los devuelve el servidor (no eres
        // participante), pero deben seguir visibles en tu dispositivo hasta que
        // los borres tú. Se conservan tal cual estaban guardados localmente.
        val serverIds = mapped.map { it.id }.toSet()
        val keptLeft = _conversations.value.filter { conv ->
            conv.hasLeft && conv.id !in serverIds && belongsOnThisDevice(conv, activeRoute)
        }
        val merged = (mapped + keptLeft)
            .distinctBy { it.id } // ids únicos: son las keys del LazyColumn
            .sortedByDescending { it.lastMessageAtMillis }

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
            val start = fetchMutex.withLock { fetchingMessages.add(conversationId) }
            if (!start) return@launch
            try {
                BackendChatApi.mensajes(conversationId).onSuccess { dtos ->
                    val prev = _messages.value[conversationId].orEmpty()
                    val prevById = prev.associateBy { it.id }
                    // El backend puede repetir filas (joins/duplicados): un id
                    // repetido rompe la key del LazyColumn y cierra la app.
                    val list = dtos.distinctBy { it.id }.map { dto ->
                        val mapped = mapDtoToMessage(dto, myId)
                        val existing = prevById[mapped.id]
                        if (existing == null) mapped
                        else mapped.copy(
                            sentAtMillis = parseIsoToMillis(dto.createdAt) ?: existing.sentAtMillis,
                            // El estado LEIDO nunca debe retroceder a ENTREGADO
                            // aunque el servidor reenvíe el mensaje cacheado.
                            status = if (existing.status == MessageStatus.READ ||
                                mapped.status == MessageStatus.READ
                            ) {
                                MessageStatus.READ
                            } else {
                                mapped.status
                            }
                        )
                    }.let { todos ->
                        // Regla 1: en un chat limpiado solo se ven los mensajes
                        // posteriores al momento en que se limpió (marca local).
                        val corte = chatsLimpiados[conversationId]
                        val visibles = if (corte != null) {
                            todos.filter { it.sentAtMillis > corte }
                        } else {
                            todos
                        }
                        // Regla 2: lo borrado aquí sigue oculto aunque el backend
                        // lo siga devolviendo (no hay borrado por mensaje allí).
                        visibles.filterNot { it.id in mensajesBorrados }
                    }
                    if (list != prev) {
                        _messages.update { it + (conversationId to list) }
                        list.lastOrNull()?.let { updateConversationPreview(conversationId, it) }
                        persist()
                    }
                }
            } finally {
                fetchMutex.withLock { fetchingMessages.remove(conversationId) }
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
        // Respaldo si el WebSocket no entrega el mensaje. 2s recargaba el historial
        // completo y trababa la UI; el socket cubre el tiempo real.
        scope.launch {
            while (true) {
                delay(15_000)
                val watched = watchedConversations.value
                if (watched.isNotEmpty() && UserSession.currentUserId != null) {
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
        val myId = UserSession.currentUserId
            ?: return Result.failure(IllegalStateException("Sesión sin usuario"))
        val myName = UserSession.displayName

        if (kind == MessageKind.AUDIO) {
            val bytes = audioFilePath?.let { AudioFileUtils.readBytes(it) }
            if (bytes == null || bytes.isEmpty()) {
                return Result.failure(IllegalArgumentException("No se pudo leer el archivo de audio"))
            }

            val apiResult = BackendChatApi.enviarAudio(
                conversacionId = conversationId,
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
                    _messages.update { all -> all.upsert(message) }
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
                    _messages.update { all -> all.upsert(message) }
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
        val myId = UserSession.currentUserId
            ?: return Result.failure(IllegalStateException("Sesión sin usuario"))

        // Un grupo creado a mano queda anclado a la subruta en la que se crea.
        // Así no aparece en las demás (el backend puede ignorar este campo en un
        // GRUPO normal; por eso también lo guardamos en manualGroupSubroutes).
        val claveSubruta = getActiveConcreteSubroute()?.let(::routeGroupKey)
        val request = CrearConversacionRequestDto(
            titulo = name,
            esGrupo = true,
            createdBy = myId,
            claveRuta = claveSubruta,
            participanteIds = listOf(myId) + members.map { it.userId }
        )

        return BackendChatApi.crearConversacion(request).map { dto ->
            if (claveSubruta != null) manualGroupSubroutes[dto.id] = claveSubruta
            val group = mapDtoToConversation(dto)
            _hidden.update { list -> list.filterNot { it.id == group.id } }
            _conversations.update { listOf(group) + it.filterNot { c -> c.id == group.id } }
            _messages.update { it + (group.id to (it[group.id].orEmpty())) }
            persist()
            group
        }
    }

    override suspend fun addMembers(conversationId: String, members: List<GroupMember>): Result<Unit> {
        if (members.isEmpty()) return Result.success(Unit)
        val myId = UserSession.currentUserId
            ?: return Result.failure(IllegalStateException("Sesión sin usuario"))
        val group = (_conversations.value + _archived.value + _hidden.value)
            .firstOrNull { it.id == conversationId }
            ?: return Result.failure(IllegalArgumentException("Grupo no encontrado"))

        // El backend reutiliza el grupo de la subruta cuando recibe la misma
        // clave: agrega a los nuevos participantes en vez de crear otro grupo.
        val request = CrearConversacionRequestDto(
            titulo = group.title,
            esGrupo = true,
            tipoConversacion = group.tipoConversacion,
            claveRuta = group.claveRuta,
            createdBy = myId,
            participanteIds = members.map { it.userId },
        )

        return BackendChatApi.crearConversacion(request)
            .onSuccess {
                refreshConversations()
                getParticipants(conversationId).onSuccess { participantes ->
                    fun update(list: List<Conversation>) = list.map { c ->
                        if (c.id == conversationId) c.copy(members = participantes) else c
                    }
                    _conversations.update(::update)
                    _archived.update(::update)
                    _hidden.update(::update)
                }
                persist()
            }
            .map { Unit }
    }

    override suspend fun asignarRol(conversationId: String, userId: String, rol: String): Result<Unit> =
        BackendChatApi.asignarRol(conversationId, userId, rol)
            .onSuccess { refreshConversations() }

    override suspend fun openPrivateChat(member: GroupMember): Result<Conversation> {
        _conversations.value.firstOrNull {
            !it.isGroup && it.title == member.name
        }?.let { return Result.success(it) }

        val myId = UserSession.currentUserId
            ?: return Result.failure(IllegalStateException("Sesión sin usuario"))
        val request = CrearConversacionRequestDto(
            titulo = member.name,
            esGrupo = false,
            createdBy = myId,
            participanteIds = listOf(myId, member.userId)
        )

        return BackendChatApi.crearConversacion(request).map { dto ->
            val chat = mapDtoToConversation(dto)
            _hidden.update { list -> list.filterNot { it.id == chat.id } }
            _conversations.update { listOf(chat) + it.filterNot { c -> c.id == chat.id } }
            _messages.update { it + (chat.id to (it[chat.id].orEmpty())) }
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
        if (UserSession.currentUserId == null) {
            return Result.failure(IllegalStateException("Sesión sin usuario"))
        }
        // La marca se guarda en el backend (sobrevive a reinstalar); el estado
        // local es solo una caché para que el efecto sea inmediato.
        BackendChatApi.limpiarConversacion(conversationId)
            .getOrElse { return Result.failure(it) }
        aplicarLimpiezaLocal(conversationId)
        return Result.success(Unit)
    }

    /**
     * Efecto visual local de limpiar un chat. Los mensajes siguen en la BD y los
     * demás participantes no notan nada. Cuando llegue un mensaje nuevo solo se
     * mostrará lo posterior a la marca.
     */
    private fun aplicarLimpiezaLocal(conversationId: String, marcaMillis: Long = currentTimeMillis()) {
        chatsLimpiados[conversationId] = marcaMillis
        _messages.update { all -> all + (conversationId to emptyList()) }
        _conversations.update { list -> list.filterNot { it.id == conversationId } }
        _archived.update { list -> list.filterNot { it.id == conversationId } }
        watchedConversations.update { it - conversationId }
        manualGroupSubroutes.remove(conversationId)
        persist()
    }

    override suspend fun ocultarConversacion(conversationId: String): Result<Unit> {
        if (UserSession.currentUserId == null) {
            return Result.failure(IllegalStateException("Sesión sin usuario"))
        }
        BackendChatApi.ocultarConversacion(conversationId).getOrElse { return Result.failure(it) }

        val conv = _conversations.value.firstOrNull { it.id == conversationId }
            ?: _archived.value.firstOrNull { it.id == conversationId }
        _conversations.update { list -> list.filterNot { it.id == conversationId } }
        _archived.update { list -> list.filterNot { it.id == conversationId } }
        if (conv != null) {
            _hidden.update { listOf(conv) + it.filterNot { c -> c.id == conversationId } }
        }
        // Los mensajes se conservan: si el usuario restaura el chat, vuelven.
        persist()
        return Result.success(Unit)
    }

    override fun hiddenConversations(): Flow<List<Conversation>> = _hidden.asStateFlow()

    override suspend fun restaurarConversacion(conversationId: String): Result<Unit> {
        if (UserSession.currentUserId == null) {
            return Result.failure(IllegalStateException("Sesión sin usuario"))
        }
        BackendChatApi.mostrarConversacion(conversationId).getOrElse { return Result.failure(it) }

        _hidden.update { list -> list.filterNot { it.id == conversationId } }
        // El refresco trae la conversación con sus datos actualizados.
        refreshConversations()
        persist()
        return Result.success(Unit)
    }

    override suspend fun solicitarEliminacionGrupo(conversationId: String): Result<Unit> {
        if (UserSession.currentUserId == null) {
            return Result.failure(IllegalStateException("Sesión sin usuario"))
        }
        val respuesta = BackendChatApi.solicitarEliminacionGrupo(conversationId)
            .getOrElse { return Result.failure(it) }

        // El grupo desaparece AHORA de las listas normales, pero conservamos una
        // copia con rol y fecha para poder cancelar la purga antes de las 72 h.
        moverAPendientes(conversationId, respuesta.seEliminaraEn)
        persist()
        return Result.success(Unit)
    }

    override suspend fun cancelarEliminacionGrupo(conversationId: String): Result<Unit> {
        if (UserSession.currentUserId == null) {
            return Result.failure(IllegalStateException("Sesión sin usuario"))
        }
        BackendChatApi.cancelarEliminacionGrupo(conversationId).getOrElse { return Result.failure(it) }

        // La cancelación hecha por este dispositivo devuelve el grupo a la lista.
        restaurarDePendientes(conversationId)
        limpiarPendienteEliminacion(conversationId)
        persist()
        refreshConversations()
        return Result.success(Unit)
    }

    private fun marcarPendienteEliminacion(conversationId: String, cuando: String?) {
        fun marcar(list: List<Conversation>) = list.map {
            if (it.id == conversationId) it.copy(
                eliminacionPendiente = true,
                eliminacionProgramadaPara = cuando ?: it.eliminacionProgramadaPara
            ) else it
        }
        _conversations.update { marcar(it) }
        _archived.update { marcar(it) }
        _hidden.update { marcar(it) }
    }

    private fun limpiarPendienteEliminacion(conversationId: String) {
        fun limpiar(list: List<Conversation>) = list.map {
            if (it.id == conversationId) it.copy(
                eliminacionPendiente = false,
                eliminacionProgramadaPara = null
            ) else it
        }
        _conversations.update { limpiar(it) }
        _archived.update { limpiar(it) }
        _hidden.update { limpiar(it) }
    }

    /** Mueve un grupo a la lista de eliminaciones pendientes (conserva rol y fecha). */
    private fun moverAPendientes(conversationId: String, cuando: String?) {
        val actual = (_conversations.value + _archived.value + _hidden.value + _eliminacionesPendientes.value)
            .firstOrNull { it.id == conversationId } ?: return
        val conv = actual.copy(
            eliminacionPendiente = true,
            eliminacionProgramadaPara = cuando ?: actual.eliminacionProgramadaPara,
        )
        _eliminacionesPendientes.update { list ->
            list.filterNot { it.id == conversationId } + conv
        }
        quitarConversacionLocal(conversationId)
    }

    /** Devuelve un grupo cancelado desde la lista de pendientes a Archivados. */
    private fun restaurarDePendientes(conversationId: String) {
        val conv = _eliminacionesPendientes.value.firstOrNull { it.id == conversationId } ?: return
        _eliminacionesPendientes.update { list -> list.filterNot { it.id == conversationId } }
        val restaurada = conv.copy(eliminacionPendiente = false, eliminacionProgramadaPara = null)
        _archived.update { list -> listOf(restaurada) + list.filterNot { c -> c.id == conversationId } }
    }

    private fun quitarConversacionLocal(conversationId: String) {
        _conversations.update { list -> list.filterNot { it.id == conversationId } }
        _archived.update { list -> list.filterNot { it.id == conversationId } }
        _hidden.update { list -> list.filterNot { it.id == conversationId } }
        // El grupo ya se borró del todo (se cumplió el plazo de 72 h): sus
        // mensajes no volverán, así que las lápidas de esos ids ya no sirven.
        _messages.value[conversationId].orEmpty().forEach { mensajesBorrados -= it.id }
        _messages.update { all -> all - conversationId }
        manualGroupSubroutes.remove(conversationId)
        chatsLimpiados.remove(conversationId)
        watchedConversations.update { it - conversationId }
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
            _conversations.update { listOf(c) + it }
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
        // El backend es la fuente de verdad: "para mí" guarda una marca personal
        // y "para todos" oculta el mensaje a todos (solo lo permite el emisor).
        // La purga física a las 72 h la hace el servidor sin avisar al usuario.
        val respuesta = if (deleteForEveryone) {
            BackendChatApi.eliminarMensajeParaTodos(conversationId, messageId)
        } else {
            BackendChatApi.ocultarMensaje(conversationId, messageId)
        }
        return respuesta.onSuccess { aplicarBorradoLocal(conversationId, messageId) }
    }

    /** Quita el mensaje de la lista ya cargada y deja el resumen al día. */
    private fun aplicarBorradoLocal(conversationId: String, messageId: String) {
        mensajesBorrados += messageId
        _messages.update { all ->
            val current = all[conversationId].orEmpty().filterNot { it.id == messageId }
            all + (conversationId to current)
        }
        recomputePreview(conversationId)
        persist()
    }

    /** Deja el resumen del chat con el último mensaje que sigue visible. */
    private fun recomputePreview(conversationId: String) {
        val last = _messages.value[conversationId].orEmpty().lastOrNull()
        fun update(list: List<Conversation>) = list.map { c ->
            if (c.id != conversationId) {
                c
            } else if (last == null) {
                c.copy(preview = "", lastMessageAtMillis = 0L, lastMessageId = null)
            } else {
                c.copy(
                    preview = if (last.kind == MessageKind.AUDIO) "Audio" else last.text,
                    lastMessageAtMillis = last.sentAtMillis,
                    lastMessageId = last.id,
                )
            }
        }
        _conversations.update(::update)
        _archived.update(::update)
    }

    override suspend fun leaveGroup(conversationId: String): Result<Unit> {
        // El backend solo quita mi fila de participante: el grupo sigue existiendo
        // para los demás. Si falla, no se toca la lista.
        if (UserSession.currentUserId == null) {
            return Result.failure(IllegalStateException("Sesión sin usuario"))
        }
        BackendChatApi.salirConversacion(conversationId).getOrElse { return Result.failure(it) }

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

    override suspend fun getParticipants(conversationId: String): Result<List<GroupMember>> {
        if (UserSession.currentUserId == null) {
            return Result.failure(IllegalStateException("Sesión sin usuario"))
        }
        val subruta = SubrouteLabels.nameOf(
            (_conversations.value + _archived.value)
                .firstOrNull { it.id == conversationId }?.claveRuta
        )
        return BackendChatApi.participantes(conversationId).map { list ->
            list.map { p ->
                val nombre = p.nombre?.takeIf { it.isNotBlank() } ?: "Conductor"
                val initials = nombre
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
                    .ifBlank { "C" }
                GroupMember(
                    userId = p.userId,
                    name = nombre,
                    initials = initials,
                    isOnline = p.isOnline == true,
                    rol = p.rol,
                    subruta = subruta
                )
            }
        }
    }

    /**
     * Inserta o reemplaza un mensaje por id.
     *
     * El WebSocket y la respuesta REST entregan el MISMO mensaje (eco del
     * servidor) sin orden garantizado: si el eco llega primero y luego se
     * añade la respuesta de `enviarTexto`, el id queda repetido y el
     * LazyColumn del chat lanza "Key ... was already used" y cierra la app.
     */
    private fun Map<String, List<Message>>.upsert(message: Message): Map<String, List<Message>> {
        if (message.id in mensajesBorrados) return this
        val current = this[message.conversationId].orEmpty()
        val index = current.indexOfFirst { it.id == message.id }
        val updated = if (index >= 0) {
            current.toMutableList().also { it[index] = message }
        } else {
            current + message
        }
        return this + (message.conversationId to updated)
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
            sentAtMillis = parseIsoToMillis(dto.createdAt) ?: currentTimeMillis(),
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
            lastMessageAtMillis = parseIsoToMillis(dto.ultimoMensaje?.createdAt) ?: currentTimeMillis(),
            unreadCount = 0,
            avatarText = dto.titulo.take(2).uppercase(),
            avatarDark = dto.esGrupo,
            lastMessageId = dto.ultimoMensaje?.id,
            members = membersList,
            tipoConversacion = dto.tipoConversacion,
            claveRuta = effectiveClaveRuta(dto),
            createdBy = dto.createdBy,
            miRol = dto.miRol,
            eliminacionPendiente = dto.eliminacionPendiente,
            eliminacionProgramadaPara = dto.eliminacionProgramadaPara
        )
    }

    private fun updateConversationPreview(conversationId: String, message: Message) {
        _conversations.update { list ->
            list.map { c ->
                if (c.id != conversationId) c
                else c.copy(
                    preview = if (message.kind == MessageKind.AUDIO) "Audio" else message.text,
                    lastMessageAtMillis = message.sentAtMillis,
                    lastMessageId = message.id
                )
            }
        }
    }

    @Volatile
    private var activeSocket: WebSocketSession? = null

    /**
     * Cierra el socket y vacía la caché al cerrar sesión o cambiar de cuenta.
     * Sin esto, el socket del usuario anterior seguía metiendo mensajes en el
     * repositorio compartido bajo la sesión nueva.
     */
    fun cerrarSesion() {
        scope.launch {
            runCatching {
                activeSocket?.close(CloseReason(CloseReason.Codes.NORMAL, "logout"))
            }
            activeSocket = null
            _messages.value = emptyMap()
            _conversations.value = emptyList()
            _archived.value = emptyList()
            _hidden.value = emptyList()
            _eliminacionesPendientes.value = emptyList()
            mensajesBorrados.clear()
            chatsLimpiados.clear()
            manualGroupSubroutes.clear()
            watchedConversations.value = emptySet()
            persist()
        }
    }

    private fun startWebSocketListener() {
        scope.launch {
            while (true) {
                try {
                    val client = HttpClientFactory.wsClient
                    val wsUrl = HttpClientFactory.wsChatUrl
                    val token = com.gowayki.nesh.core.session.AuthTokenState.authorizationHeader
                    if (token == null) {
                        delay(5000)
                        continue
                    }
                    val tokenConexion = token
                    client.webSocket(
                        urlString = wsUrl,
                        request = { header(HttpHeaders.Authorization, tokenConexion) }
                    ) {
                        activeSocket = this
                        try {
                            for (frame in incoming) {
                                // Si el token cambió (logout o cambio de cuenta),
                                // cortamos esta conexión antes de seguir leyendo.
                                if (com.gowayki.nesh.core.session.AuthTokenState.authorizationHeader != tokenConexion) {
                                    break
                                }
                                if (frame is Frame.Text) {
                                    handleIncomingWebSocketMessage(frame.readText())
                                }
                            }
                        } finally {
                            if (activeSocket === this) activeSocket = null
                        }
                    }
                } catch (_: Throwable) {
                }
                delay(5000)
            }
        }
    }

    private fun handleIncomingWebSocketMessage(text: String) {
        val evento = runCatching { chatJson.parseToJsonElement(text).jsonObject }.getOrNull()
        val tipo = evento?.get("tipo")?.jsonPrimitive?.content
        when (tipo) {
            // Ocultar/mostrar son personales: solo afectan a este usuario.
            "CONVERSACION_OCULTA" -> {
                val id = evento["conversacionId"]?.jsonPrimitive?.content ?: return
                val conv = _conversations.value.firstOrNull { it.id == id }
                    ?: _archived.value.firstOrNull { it.id == id }
                _conversations.update { list -> list.filterNot { it.id == id } }
                _archived.update { list -> list.filterNot { it.id == id } }
                if (conv != null) {
                    _hidden.update { listOf(conv) + it.filterNot { c -> c.id == id } }
                }
                persist()
                return
            }
            "CONVERSACION_MOSTRADA" -> {
                val id = evento["conversacionId"]?.jsonPrimitive?.content ?: return
                _hidden.update { list -> list.filterNot { it.id == id } }
                refreshConversations()
                persist()
                return
            }
            // Otro dispositivo del mismo usuario limpió el chat: sync de la marca.
            "CONVERSACION_LIMPIADA" -> {
                val id = evento["conversacionId"]?.jsonPrimitive?.content ?: return
                val marca = evento["limpiadoEn"]?.jsonPrimitive?.content?.let(::parseIsoToMillis)
                    ?: currentTimeMillis()
                aplicarLimpiezaLocal(id, marca)
                return
            }
            "GRUPO_ELIMINACION_PENDIENTE" -> {
                val id = evento["conversacionId"]?.jsonPrimitive?.content ?: return
                val cuando = evento["ejecutarDespues"]?.jsonPrimitive?.content
                moverAPendientes(id, cuando)
                persist()
                return
            }
            "GRUPO_ELIMINACION_CANCELADA" -> {
                val id = evento["conversacionId"]?.jsonPrimitive?.content ?: return
                restaurarDePendientes(id)
                limpiarPendienteEliminacion(id)
                // Se deshizo el soft delete en BD: traer el grupo de vuelta.
                refreshConversations()
                persist()
                return
            }
            // Purga definitiva cumplidas las 72 horas (aviso a los participantes).
            "CONVERSACION_ELIMINADA" -> {
                val id = evento["conversacionId"]?.jsonPrimitive?.content ?: return
                _eliminacionesPendientes.update { list -> list.filterNot { it.id == id } }
                quitarConversacionLocal(id)
                persist()
                return
            }
            // Otro dispositivo del mismo usuario ocultó este mensaje para sí.
            "MENSAJE_OCULTO" -> {
                val id = evento["conversacionId"]?.jsonPrimitive?.content ?: return
                val mensajeId = evento["mensajeId"]?.jsonPrimitive?.content ?: return
                aplicarBorradoLocal(id, mensajeId)
                return
            }
            // El emisor lo borró para todos: desaparece en todas las apps.
            "MENSAJE_ELIMINADO" -> {
                val id = evento["conversacionId"]?.jsonPrimitive?.content ?: return
                val mensajeId = evento["mensajeId"]?.jsonPrimitive?.content ?: return
                aplicarBorradoLocal(id, mensajeId)
                return
            }
        }

        val dto = runCatching { chatJson.decodeFromString<MensajeResponseDto>(text) }.getOrNull()
        if (dto != null) {
            val myId = UserSession.currentUserId ?: ""
            val message = mapDtoToMessage(dto, myId)

            // Borrado en este dispositivo: ni el WebSocket lo devuelve a la lista.
            if (message.id in mensajesBorrados) return

            // La conversación está oculta: el mensaje no debe hacerla reaparecer.
            if (_hidden.value.any { it.id == message.conversationId }) return

            var appended = false
            _messages.update { all ->
                val existing = all[message.conversationId].orEmpty()
                if (existing.any { it.id == message.id }) {
                    // Un ack de lectura puede llegar por WebSocket después del
                    // eco del envío: nunca lo dejamos volver a ENTREGADO.
                    val actual = existing.first { it.id == message.id }
                    if (actual.status != MessageStatus.READ && message.status == MessageStatus.READ) {
                        all + (message.conversationId to existing.map { m ->
                            if (m.id == message.id) m.copy(status = MessageStatus.READ) else m
                        })
                    } else {
                        all
                    }
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
        // Estados guardados por versiones anteriores pueden traer ids repetidos
        // (WS + REST). Se limpian al cargar para no reventar las keys de Compose.
        _conversations.value = saved.conversations.distinctBy { it.id }
        _archived.value = saved.archived.distinctBy { it.id }
        _hidden.value = saved.hidden.distinctBy { it.id }
        _eliminacionesPendientes.value = saved.eliminacionesPendientes.distinctBy { it.id }
        _messages.value = saved.messages.mapValues { (_, list) -> list.distinctBy { it.id } }
        manualGroupSubroutes.clear()
        manualGroupSubroutes.putAll(saved.manualGroupSubroutes)
        chatsLimpiados.clear()
        chatsLimpiados.putAll(saved.chatsLimpiados)
        mensajesBorrados.clear()
        mensajesBorrados.addAll(saved.mensajesBorrados)
        // Por si una versión anterior guardó el mensaje antes de borrarlo.
        _messages.value = _messages.value.mapValues { (_, list) ->
            list.filterNot { it.id in mensajesBorrados }.distinctBy { it.id }
        }
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
                val cappedMessages = _messages.value.mapValues { (_, list) -> list.takeLast(30) }
                ChatStore.write(
                    chatJson.encodeToString(
                        PersistedChatState(
                            conversations = _conversations.value,
                            archived = _archived.value,
                            hidden = _hidden.value,
                            eliminacionesPendientes = _eliminacionesPendientes.value,
                            messages = cappedMessages,
                            manualGroupSubroutes = manualGroupSubroutes.toMap(),
                            chatsLimpiados = chatsLimpiados.toMap(),
                            mensajesBorrados = mensajesBorrados.toSet()
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
        /** Conversaciones que el usuario ocultó: se guardan para poder restaurarlas. */
        val hidden: List<Conversation> = emptyList(),
        /** Grupos con purga pendiente: se conservan para poder cancelarla. */
        val eliminacionesPendientes: List<Conversation> = emptyList(),
        val messages: Map<String, List<Message>> = emptyMap(),
        /** Subruta anclada a cada grupo manual, para no mostrarlo en todas partes. */
        val manualGroupSubroutes: Map<String, String> = emptyMap(),
        /** Marca local de los chats limpiados (solo visual, nunca se envía al backend). */
        val chatsLimpiados: Map<String, Long> = emptyMap(),
        /** Textos y audios borrados en este dispositivo; la BD los conserva hasta borrar el grupo. */
        val mensajesBorrados: Set<String> = emptySet()
    )
}

