package com.gowayki.nesh.data.entities.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val dni: String,
    val pin: String
)

@Serializable
data class RegisterRequestDto(
    val nombre: String,
    val dni: String,
    val telefono: String,
    val email: String? = null,
    val pin: String,
    val fotoUrl: String? = null,
    val rol: String? = null,
)

@Serializable
data class UsuarioResponseDto(
    val id: String,
    val nombre: String,
    val dni: String,
    val telefono: String,
    val email: String? = null,
    val fotoUrl: String? = null,
    val rol: String,
    val isOnline: Boolean? = null
)

@Serializable
data class LoginResponseDto(
    val exito: Boolean,
    val mensaje: String? = null,
    val usuario: UsuarioResponseDto? = null
)

