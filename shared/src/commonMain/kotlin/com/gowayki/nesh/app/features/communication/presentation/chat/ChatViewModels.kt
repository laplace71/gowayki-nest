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

    var loading by mutableStateOf(true)
        private set

    var category by mutableStateOf(0)
        private set // 0 Todos, 1 Grupos, 2 Privados

    var query by mutableStateOf("")
        private set

    init {
        viewModelScope.launch {
            repo.conversations().collect { list ->
                conversations = list
                loading = false
            }
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

    /** Elimina la conversación de la lista (funciona para grupo y privado). */
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch { repo.deleteConversation(conversationId) }
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

    init {
        viewModelScope.launch {
            repo.messages(conversationId).collect { list -> messages = list }
        }
        viewModelScope.launch { repo.markAsRead(conversationId) }
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

    /** Simula una nota de voz enviada (mismo camino que un texto). */
    fun sendVoiceNote(durationSeconds: Int) {
        if (sending) return
        sending = true
        viewModelScope.launch {
            repo.sendMessage(
                conversationId = conversationId,
                text = "Nota de voz",
                kind = MessageKind.AUDIO,
                audioDurationSeconds = durationSeconds
            )
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

    var colleagues: List<GroupMember> = emptyList()
        private set

    init {
        // FakeChatRepository expone la lista; con backend real vendrá del repo.
        (repo as? com.gowayki.nesh.data.repository.FakeChatRepository)
            ?.availableColleagues()
            ?.let { colleagues = it }
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