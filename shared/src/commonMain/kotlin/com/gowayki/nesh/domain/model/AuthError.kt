package com.gowayki.nesh.domain.model

/**
 * Motivo por el que falló un intento de sesión, ya traducido a un texto que se
 * le puede mostrar al chofer: sin IPs, sin nombres de excepciones ni detalles
 * técnicos.
 *
 * El backend es la única autoridad sobre los intentos y el bloqueo
 * ([AuthPolicy]), así que la app solo refleja lo que él responde.
 */
sealed class AuthError(mensaje: String) : Exception(mensaje) {

    /** No se pudo llegar al servidor: sin internet, servidor caído o timeout. */
    class SinConexion : AuthError(
        "No pudimos conectarnos. Revisa tu internet e inténtalo otra vez."
    )

    /**
     * El servidor rechazó el PIN.
     *
     * @param restantes intentos que quedan según el servidor, o null si no los
     *   informa (en ese caso la app descuenta uno).
     */
    class PinIncorrecto(
        mensaje: String,
        val restantes: Int?,
    ) : AuthError(mensaje)

    /** La cuenta quedó bloqueada temporalmente (el servidor responde 423). */
    class CuentaBloqueada(mensaje: String) : AuthError(mensaje)

    /** Otro rechazo con mensaje del servidor (DNI inexistente, datos inválidos…). */
    class Rechazado(mensaje: String) : AuthError(mensaje)
}
