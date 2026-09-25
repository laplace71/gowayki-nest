package com.gowayki.nesh.data.entities.auth

// Entidad de dominio usada por el flujo Supabase (dormido).
// En la fase JSON/fake la app usa domain/model/User + FakeAuthRepository.
data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null,
)