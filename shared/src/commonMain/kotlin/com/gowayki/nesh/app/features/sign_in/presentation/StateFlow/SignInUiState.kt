package com.gowayki.nesh.app.features.sign_in.presentation.StateFlow

import com.gowayki.nesh.app.features.sign_in.domain.models.ProfileGender

data class SignInUiState(
    val isLoading: Boolean = false,
    val isAccountLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val needsRegistration: Boolean = false,
    val profileComplete: Boolean = false,
    val error: String? = null,
    val userEmail: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val registerName: String = "",
    val registerGender: ProfileGender = ProfileGender.Other,
    val acceptTerms: Boolean = false,
)
