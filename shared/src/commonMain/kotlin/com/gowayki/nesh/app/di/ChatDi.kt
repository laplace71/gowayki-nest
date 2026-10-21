package com.gowayki.nesh.app.di

import com.gowayki.nesh.app.features.communication.domain.repository.ChatRepository
import com.gowayki.nesh.app.features.communication.presentation.chat.ChatViewModel
import com.gowayki.nesh.app.features.communication.presentation.chat.ConversationsViewModel
import com.gowayki.nesh.app.features.communication.presentation.chat.NewGroupViewModel
import com.gowayki.nesh.data.repository.FakeChatRepository

/**
 * Módulo DI de Comunicación.
 *
 * Sigue el patrón de AuthDi: se registra la interfaz del dominio y el
 * repositorio la resuelve. Para cambiar a backend basta con reemplazar
 * `FakeChatRepository` por la implementación real.
 */
object ChatDi {
    fun install() {
        AppDi.registerLazy<ChatRepository> { FakeChatRepository() }

        AppDi.registerLazy { ConversationsViewModel(AppDi.get<ChatRepository>()) }
        AppDi.registerLazy { NewGroupViewModel(AppDi.get<ChatRepository>()) }
    }

    /** El ChatViewModel depende de la conversación, así que no se cachea. */
    fun chatViewModel(conversationId: String): ChatViewModel =
        ChatViewModel(AppDi.get<ChatRepository>(), conversationId)
}