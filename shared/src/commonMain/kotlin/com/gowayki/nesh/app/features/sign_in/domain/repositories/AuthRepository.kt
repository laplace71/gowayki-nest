package com.gowayki.nesh.app.features.sign_in.domain.repositories

import com.gowayki.nesh.app.features.sign_in.domain.models.AccountStatus
import com.gowayki.nesh.app.features.sign_in.domain.models.CompleteProfileInput
import com.gowayki.nesh.app.features.sign_in.domain.models.CompleteProfileResult
import com.gowayki.nesh.data.entities.auth.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: AuthUser?

    fun observeSession(): Flow<AuthUser?>

    /** OAuth por navegador (fallback cuando no hay native Google). */
    suspend fun signInWithGoogleOAuth()

    suspend fun signOut()

    /** RPC `get_own_account_status` — fuente de verdad multi-app. */
    suspend fun getAccountStatus(): AccountStatus

    /** RPC `complete_own_profile` — registra/completa `usr_profiles`. */
    suspend fun completeOwnProfile(input: CompleteProfileInput): CompleteProfileResult
}
