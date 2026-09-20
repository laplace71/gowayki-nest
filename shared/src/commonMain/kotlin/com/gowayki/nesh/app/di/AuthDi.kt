package com.gowayki.nesh.app.di

import com.gowayki.nesh.app.features.sign_in.domain.repositories.AuthRepository
import com.gowayki.nesh.app.features.sign_in.domain.usecases.CompleteOwnProfileUseCase
import com.gowayki.nesh.app.features.sign_in.domain.usecases.GetAccountStatusUseCase
import com.gowayki.nesh.app.features.sign_in.domain.usecases.ObserveAuthSessionUseCase
import com.gowayki.nesh.app.features.sign_in.domain.usecases.SignInWithGoogleOAuthUseCase
import com.gowayki.nesh.app.features.sign_in.domain.usecases.SignOutUseCase
import com.gowayki.nesh.infra.auth.SupabaseAuthRepository

/**
 * Módulo DI de autenticación.
 *
 * Expone [AuthRepository] y use cases para ViewModels / pantallas:
 * ```
 * val auth = AuthDi.repository()
 * // o
 * val auth = AppDi.get<AuthRepository>()
 * ```
 */
object AuthDi {
    fun install() {
        AppDi.registerLazy<AuthRepository> {
            SupabaseAuthRepository(SupabaseDi.client())
        }
        AppDi.registerLazy<ObserveAuthSessionUseCase> {
            ObserveAuthSessionUseCase(AppDi.get())
        }
        AppDi.registerLazy<SignOutUseCase> {
            SignOutUseCase(AppDi.get())
        }
        AppDi.registerLazy<SignInWithGoogleOAuthUseCase> {
            SignInWithGoogleOAuthUseCase(AppDi.get())
        }
        AppDi.registerLazy<GetAccountStatusUseCase> {
            GetAccountStatusUseCase(AppDi.get())
        }
        AppDi.registerLazy<CompleteOwnProfileUseCase> {
            CompleteOwnProfileUseCase(AppDi.get())
        }
    }

    fun repository(): AuthRepository = AppDi.get()

    fun observeSession(): ObserveAuthSessionUseCase = AppDi.get()

    fun signOut(): SignOutUseCase = AppDi.get()

    fun accountStatus(): GetAccountStatusUseCase = AppDi.get()

    fun completeProfile(): CompleteOwnProfileUseCase = AppDi.get()

    /** Útil en tests: sustituye el repositorio. */
    fun register(repository: AuthRepository) {
        AppDi.register(repository)
    }
}
