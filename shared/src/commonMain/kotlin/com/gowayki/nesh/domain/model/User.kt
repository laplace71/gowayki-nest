package com.gowayki.nesh.domain.model

// Usuario de dominio (lo que usan pantallas y ViewModel).
// No depende de Retrofit ni del JSON: si el backend cambia, solo se toca el mapper.
data class User(
    val id: String,
    val name: String,
    // Alineado con AuthUser (infra): el email viene del proveedor (Supabase/Google).
    val email: String = "",
    val dni: String = "",
    val phone: String = "",
)