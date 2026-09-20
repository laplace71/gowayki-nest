package com.gowayki.nesh.infra.auth

import com.gowayki.nesh.app.features.sign_in.domain.models.AccountStatus
import com.gowayki.nesh.app.features.sign_in.domain.models.CompleteProfileInput
import com.gowayki.nesh.app.features.sign_in.domain.models.CompleteProfileResult
import com.gowayki.nesh.app.features.sign_in.domain.repositories.AuthRepository
import com.gowayki.nesh.data.entities.auth.AuthUser
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

class SupabaseAuthRepository(
    private val client: SupabaseClient,
) : AuthRepository {

    override val currentUser: AuthUser?
        get() = client.auth.currentUserOrNull()?.toAuthUser()

    override fun observeSession(): Flow<AuthUser?> =
        client.auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> status.session.user?.toAuthUser()
                else -> null
            }
        }

    override suspend fun signInWithGoogleOAuth() {
        client.auth.signInWith(Google)
    }

    override suspend fun signOut() {
        client.auth.signOut()
    }

    override suspend fun getAccountStatus(): AccountStatus =
        client.postgrest.rpc("get_own_account_status").decodeAs()

    override suspend fun completeOwnProfile(input: CompleteProfileInput): CompleteProfileResult {
        val avatar = input.avatarUrl?.trim()?.takeIf { it.isNotBlank() }
        val params = buildJsonObject {
            put("p_full_name", input.fullName.trim())
            if (avatar == null) put("p_avatar_url", JsonNull) else put("p_avatar_url", avatar)
            put("p_gender", input.gender.toApi())
            put("p_accept_terms", input.acceptTerms)
        }
        return client.postgrest.rpc("complete_own_profile", params).decodeAs()
    }

    private fun UserInfo.toAuthUser(): AuthUser =
        AuthUser(
            id = id,
            email = email.orEmpty(),
            displayName = metaString("full_name") ?: metaString("name"),
            photoUrl = metaString("avatar_url") ?: metaString("picture"),
        )

    private fun UserInfo.metaString(key: String): String? {
        val element: JsonElement = userMetadata?.get(key) ?: return null
        return (element as? JsonPrimitive)?.contentOrNull
    }
}
