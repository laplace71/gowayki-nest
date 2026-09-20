package com.gowayki.nesh.data.entities.auth

data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null,
)
