package com.gowayki.nesh.app.di

import com.gowayki.nesh.app.features.sign_in.presentation.StateFlow.AuthViewModel
import com.gowayki.nesh.data.repository.KtorAuthRepository
import com.gowayki.nesh.domain.repository.AuthRepository

object AuthDi {
    fun install() {
        AppDi.registerLazy<AuthRepository> { KtorAuthRepository() }
        AppDi.registerLazy { AuthViewModel(AppDi.get<AuthRepository>()) }
    }
}
