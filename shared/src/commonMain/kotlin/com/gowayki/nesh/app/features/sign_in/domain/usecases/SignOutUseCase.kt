package com.gowayki.nesh.app.features.sign_in.domain.usecases

import com.gowayki.nesh.app.features.sign_in.domain.repositories.AuthRepository

class SignOutUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke() = repository.signOut()
}
