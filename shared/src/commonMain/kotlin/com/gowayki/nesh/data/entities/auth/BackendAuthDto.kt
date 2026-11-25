package com.gowayki.nesh.data.entities.auth

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Django devuelve ids numéricos (`"subrutaId": 7`) y Spring strings (`"7"`).
 * El dominio siempre trabaja con String?, así que normalizamos en la entrada.
 */
@OptIn(ExperimentalSerializationApi::class)
object TolerantIdStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("TolerantIdString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) encoder.encodeNull() else encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String? {
        val element = JsonElement.serializer().deserialize(decoder)
        return when (element) {
            JsonNull -> null
            is JsonPrimitive -> element.content.takeIf { it.isNotBlank() }
            else -> null
        }
    }
}

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
data class CambiarPinRequestDto(
    val pinActual: String,
    val pinNuevo: String
)

@Serializable
data class RegistroSolicitudRequestDto(
    val nombre: String,
    val dni: String,
    val telefono: String,
    val email: String? = null,
    val pin: String,
    val subrutaId: Int,
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
    val isOnline: Boolean? = null,
    @Serializable(with = TolerantIdStringSerializer::class)
    val subrutaId: String? = null,
    val subrutaNombre: String? = null
)

@Serializable
data class LoginResponseDto(
    val exito: Boolean,
    val mensaje: String? = null,
    val usuario: UsuarioResponseDto? = null,
    /** JWT para el chat: se manda como `Authorization: Bearer <token>`. */
    val token: String? = null
)

