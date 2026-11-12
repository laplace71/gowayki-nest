package com.gowayki.nesh.app.di

import com.gowayki.nesh.app.features.communication.domain.repository.ChatRepository
import com.gowayki.nesh.app.features.communication.presentation.chat.ChatViewModel
import com.gowayki.nesh.data.repository.KtorChatRepository

object ChatDi {
    fun install() {
        AppDi.registerLazy<ChatRepository> { KtorChatRepository() }
    }

    fun chatViewModel(conversationId: String): ChatViewModel =
        ChatViewModel(AppDi.get<ChatRepository>(), conversationId)
}