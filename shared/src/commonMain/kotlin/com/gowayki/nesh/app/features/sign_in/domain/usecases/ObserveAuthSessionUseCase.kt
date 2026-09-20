package com.gowayki.nesh.app.features.sign_in.domain.usecases

import com.gowayki.nesh.app.features.sign_in.domain.repositories.AuthRepository
import com.gowayki.nesh.data.entities.auth.AuthUser
import kotlinx.coroutines.flow.Flow

class ObserveAuthSessionUseCase(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthUser?> = repository.observeSession()
}
