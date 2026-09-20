package com.gowayki.nesh.app.features.sign_in.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class ProfileGender {
    Male,
    Female,
    Other,
    ;

    fun toApi(): String = when (this) {
        Male -> "male"
        Female -> "female"
        Other -> "other"
    }

    companion object {
        fun fromApi(raw: String?): ProfileGender = when (raw?.lowercase()) {
            "male" -> Male
            "female" -> Female
            else -> Other
        }
    }
}

data class CompleteProfileInput(
    val fullName: String,
    val avatarUrl: String? = null,
    val gender: ProfileGender = ProfileGender.Other,
    val acceptTerms: Boolean = true,
)

@Serializable
data class AccountStatus(
    val authenticated: Boolean = false,
    @SerialName("has_sys_user") val hasSysUser: Boolean = false,
    @SerialName("has_profile") val hasProfile: Boolean = false,
    @SerialName("profile_complete") val profileComplete: Boolean = false,
    @SerialName("needs_registration") val needsRegistration: Boolean = false,
    val user: AccountUserSnapshot? = null,
    val profile: AccountProfileSnapshot? = null,
    val error: String? = null,
)

@Serializable
data class AccountUserSnapshot(
    val id: String,
    val email: String? = null,
    val phone: String? = null,
    val role: String? = null,
    val status: String? = null,
)

@Serializable
data class AccountProfileSnapshot(
    val id: String,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val gender: String? = null,
    @SerialName("terms_accepted_at") val termsAcceptedAt: String? = null,
    val points: Int? = null,
)

@Serializable
data class CompleteProfileResult(
    val success: Boolean = false,
    val error: String? = null,
)
