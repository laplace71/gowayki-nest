package com.gowayki.nesh.app.features.sign_in.domain.usecases

import com.gowayki.nesh.app.features.sign_in.domain.models.CompleteProfileInput
import com.gowayki.nesh.app.features.sign_in.domain.models.CompleteProfileResult
import com.gowayki.nesh.app.features.sign_in.domain.repositories.AuthRepository

class CompleteOwnProfileUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(input: CompleteProfileInput): CompleteProfileResult =
        repository.completeOwnProfile(input)
}
