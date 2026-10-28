package com.gowayki.nesh.app.di

import com.gowayki.nesh.app.features.communication.domain.repository.ChatRepository
import com.gowayki.nesh.app.features.communication.presentation.chat.ChatViewModel
import com.gowayki.nesh.data.repository.FakeChatRepository

/**
 * Módulo DI de Comunicación.
 *
 * Solo se registra el repositorio como singleton. Los ViewModels
 * (ConversationsViewModel, NewGroupViewModel) NO se registran aquí
 * porque extienden ViewModel: su viewModelScope se cancela al salir
 * de la pantalla y un singleton muerto no puede ejecutar coroutines.
 * Se crean frescos con `viewModel { ... }` en cada pantalla.
 */
object ChatDi {
    fun install() {
        AppDi.registerLazy<ChatRepository> { FakeChatRepository() }
    }

    /** El ChatViewModel depende de la conversación, así que no se cachea. */
    fun chatViewModel(conversationId: String): ChatViewModel =
        ChatViewModel(AppDi.get<ChatRepository>(), conversationId)
}