// ViewModel de Comunicación: traduce la interfaz ChatRepository a estado de UI.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.communication.presentation.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gowayki.nesh.app.features.communication.domain.model.Conversation
import com.gowayki.nesh.app.features.communication.domain.model.GroupMember
import com.gowayki.nesh.app.features.communication.domain.model.Message
import com.gowayki.nesh.app.features.communication.domain.model.MessageKind
import com.gowayki.nesh.app.features.communication.domain.repository.ChatRepository
import kotlinx.coroutines.launch

/** Estado de la lista de conversaciones. */
class ConversationsViewModel(private val repo: ChatRepository) : ViewModel() {

    var conversations by mutableStateOf<List<Conversation>>(emptyList())
        private set

    var archived by mutableStateOf<List<Conversation>>(emptyList())
        private set

    var hidden by mutableStateOf<List<Conversation>>(emptyList())
        private set

    var loading by mutableStateOf(true)
        private set

    var category by mutableStateOf(0)
        private set // 0 Todos, 1 Grupos, 2 Privados

    var query by mutableStateOf("")
        private set

    init {
        viewModelScope.launch {
            // Los ids son las keys del LazyColumn: nunca emitir repetidos.
            repo.conversations().collect { list ->
                conversations = list.distinctBy { it.id }
                loading = false
            }
        }
        viewModelScope.launch {
            repo.archived().collect { list -> archived = list.distinctBy { it.id } }
        }
        viewModelScope.launch {
            repo.hiddenConversations().collect { list -> hidden = list.distinctBy { it.id } }
        }
    }

    /** Conversaciones ya filtradas por categoría y búsqueda. */
    val visible: List<Conversation>
        get() = conversations.filter { c ->
            val byCategory = when (category) {
                1 -> c.isGroup
                2 -> !c.isGroup
                else -> true
            }
            val byText = query.isBlank() ||
                c.title.contains(query, ignoreCase = true) ||
                c.preview.contains(query, ignoreCase = true)
            byCategory && byText
        }

    fun onCategoryChange(index: Int) { category = index }

    fun onQueryChange(value: String) { query = value }

    fun open(conversationId: String) {
        viewModelScope.launch { repo.markAsRead(conversationId) }
    }

    /** Vacía el historial de una conversación. */
    fun clearChat(conversationId: String) {
        viewModelScope.launch { repo.clearMessages(conversationId) }
    }

    /** Oculta la conversación solo para este usuario (no borra nada para los demás). */
    fun hideConversation(conversationId: String) {
        viewModelScope.launch { repo.ocultarConversacion(conversationId) }
    }

    /** Restaura un chat que el usuario había ocultado. */
    fun restoreConversation(conversationId: String) {
        viewModelScope.launch { repo.restaurarConversacion(conversationId) }
    }

    /** Elimina el grupo de la UI al instante; purga física en BD a las 72 horas. */
    fun requestGroupDeletion(conversationId: String) {
        viewModelScope.launch { repo.solicitarEliminacionGrupo(conversationId) }
    }

    /** Cancela la eliminación definitiva antes de que se cumpla el plazo. */
    fun cancelGroupDeletion(conversationId: String) {
        viewModelScope.launch { repo.cancelarEliminacionGrupo(conversationId) }
    }

    fun archiveConversation(conversationId: String) {
        viewModelScope.launch { repo.archiveConversation(conversationId) }
    }

    fun unarchiveConversation(conversationId: String) {
        viewModelScope.launch { repo.unarchiveConversation(conversationId) }
    }

    fun updateConversationPhoto(conversationId: String, path: String?) {
        viewModelScope.launch { repo.updateConversationPhoto(conversationId, path) }
    }
}

/** Estado de la pantalla de chat de una conversación. */
class ChatViewModel(
    private val repo: ChatRepository,
    private val conversationId: String
) : ViewModel() {

    var messages by mutableStateOf<List<Message>>(emptyList())
        private set

    var draft by mutableStateOf("")
        private set

    var sending by mutableStateOf(false)
        private set

    var participants by mutableStateOf<List<GroupMember>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            // Blindaje: si el repositorio alguna vez emite ids repetidos, el
            // LazyColumn del chat revienta por key duplicada. La UI nunca debe
            // recibir dos mensajes con el mismo id.
            repo.messages(conversationId).collect { list -> messages = list.distinctBy { it.id } }
        }
        viewModelScope.launch { repo.markAsRead(conversationId) }
        loadParticipants()
    }

    fun loadParticipants() {
        viewModelScope.launch {
            repo.getParticipants(conversationId).onSuccess { participants = it }
        }
    }

    fun onDraftChange(value: String) { draft = value }

    fun send() {
        val text = draft.trim()
        if (text.isEmpty() || sending) return
        sending = true
        draft = ""
        viewModelScope.launch {
            repo.sendMessage(conversationId, text, MessageKind.TEXT)
                .onFailure { draft = text } // devuelve el texto si falla
            sending = false
        }
    }

}

/** Estado de la pantalla "crear grupo". */
class NewGroupViewModel(private val repo: ChatRepository) : ViewModel() {

    var name by mutableStateOf("")
        private set

    var selected by mutableStateOf<Set<String>>(emptySet())
        private set

    var creating by mutableStateOf(false)
        private set

    var colleagues: List<GroupMember> by mutableStateOf(emptyList())
        private set

    /** Se calcula al leer: la subruta puede cambiar mientras el formulario está abierto. */
    val subrouteLabel: String?
        get() = repo.activeSubrouteLabel()

    init {
        (repo as? com.gowayki.nesh.data.repository.FakeChatRepository)?.availableColleagues()?.let { colleagues = it }
        val ktorRepo = repo as? com.gowayki.nesh.data.repository.KtorChatRepository
        if (ktorRepo != null) {
            colleagues = ktorRepo.availableColleagues()
            ktorRepo.fetchColleagues()
            viewModelScope.launch {
                ktorRepo.colleaguesFlow.collect { colleagues = it }
            }
        }
    }

    fun onNameChange(value: String) { name = value }

    fun toggle(userId: String) {
        selected = if (selected.contains(userId)) selected - userId else selected + userId
    }

    /** Crea el grupo y devuelve el id de la conversación creada. */
    fun create(onCreated: (String) -> Unit) {
        val members = colleagues.filter { selected.contains(it.userId) }
        if (name.isBlank() || members.isEmpty() || creating) return
        creating = true
        viewModelScope.launch {
            repo.createGroup(name.trim(), members)
                .onSuccess {
                    creating = false
                    name = ""
                    selected = emptySet()
                    onCreated(it.id)
                }
                .onFailure { creating = false }
        }
    }
}