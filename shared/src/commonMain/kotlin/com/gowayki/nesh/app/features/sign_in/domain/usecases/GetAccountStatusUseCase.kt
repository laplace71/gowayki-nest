package com.gowayki.nesh.app.features.sign_in.domain.usecases

import com.gowayki.nesh.app.features.sign_in.domain.models.AccountStatus
import com.gowayki.nesh.app.features.sign_in.domain.repositories.AuthRepository

class GetAccountStatusUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): AccountStatus = repository.getAccountStatus()
}
