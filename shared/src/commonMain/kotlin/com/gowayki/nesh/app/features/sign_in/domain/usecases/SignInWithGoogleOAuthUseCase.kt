package com.gowayki.nesh.app.features.sign_in.domain.usecases

import com.gowayki.nesh.app.features.sign_in.domain.repositories.AuthRepository

class SignInWithGoogleOAuthUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke() = repository.signInWithGoogleOAuth()
}
