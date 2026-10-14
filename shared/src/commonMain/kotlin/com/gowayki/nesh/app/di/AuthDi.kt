package com.gowayki.nesh.app.di

import com.gowayki.nesh.app.features.sign_in.presentation.StateFlow.AuthViewModel
import com.gowayki.nesh.data.repository.FakeAuthRepository
import com.gowayki.nesh.domain.repository.AuthRepository

/**
 * Mdulo DI de autenticacin.
 */
object AuthDi {
    fun install() {
        // Registramos el Mock (Fase MVP API-First)
        AppDi.registerLazy<AuthRepository> { FakeAuthRepository() }
        
        // Registramos el ViewModel inyectando el Repositorio
        AppDi.registerLazy { AuthViewModel(AppDi.get<AuthRepository>()) }
    }
}