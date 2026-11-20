package com.gowayki.nesh

import com.gowayki.nesh.data.repository.clasificarErrorDeLogin
import com.gowayki.nesh.domain.model.AuthError
import com.gowayki.nesh.domain.model.AuthPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * El login debe mostrar el motivo real y nunca detalles técnicos (IP, códigos
 * o nombres de excepciones). El bloqueo, además, tiene que seguir las mismas
 * reglas que guarda el backend en la base de datos.
 */
class AuthErrorTest {

    @Test
    fun `pin incorrecto toma los intentos que informa el servidor`() {
        val error = clasificarErrorDeLogin(
            status = 401,
            mensaje = "PIN incorrecto. Intentos restantes antes de bloqueo temporal: 4",
        )

        val pin = assertIs<AuthError.PinIncorrecto>(error)
        assertEquals(4, pin.restantes)
    }

    @Test
    fun `el ultimo intento queda en cero`() {
        val error = clasificarErrorDeLogin(
            status = 401,
            mensaje = "PIN incorrecto. Intentos restantes antes de bloqueo temporal: 0",
        )

        val pin = assertIs<AuthError.PinIncorrecto>(error)
        assertEquals(0, pin.restantes)
    }

    @Test
    fun `la cuenta bloqueada se reconoce por el estado 423`() {
        val error = clasificarErrorDeLogin(
            status = 423,
            mensaje = "Cuenta bloqueada temporalmente por exceso de intentos fallidos. Intente más tarde.",
        )

        assertIs<AuthError.CuentaBloqueada>(error)
    }

    @Test
    fun `un dni inexistente conserva el mensaje del servidor`() {
        val error = clasificarErrorDeLogin(
            status = 401,
            mensaje = "Credenciales inválidas: No existe usuario registrado con ese identificador.",
        )

        val rechazo = assertIs<AuthError.Rechazado>(error)
        assertEquals(
            "Credenciales inválidas: No existe usuario registrado con ese identificador.",
            rechazo.message,
        )
    }

    @Test
    fun `sin mensaje del servidor se usa un texto propio`() {
        val error = clasificarErrorDeLogin(status = 500, mensaje = null)

        assertIs<AuthError.Rechazado>(error)
    }

    @Test
    fun `ningun mensaje muestra la ip ni detalles tecnicos`() {
        val mensajes = listOf(
            clasificarErrorDeLogin(423, "Cuenta bloqueada temporalmente."),
            clasificarErrorDeLogin(401, "PIN incorrecto. Intentos restantes: 2"),
            clasificarErrorDeLogin(500, null),
            AuthError.SinConexion(),
            AuthError.Rechazado("No pudimos completar la solicitud. Inténtalo otra vez."),
        ).map { it.message.orEmpty() }

        mensajes.forEach { mensaje ->
            assertFalse(mensaje.contains("31.220"), "no debe mostrar la IP: $mensaje")
            assertFalse(mensaje.contains("http"), "no debe mostrar la URL: $mensaje")
            assertFalse(mensaje.contains("Exception"), "no debe mostrar la excepción: $mensaje")
        }
    }

    @Test
    fun `el bloqueo local coincide con el backend`() {
        // LoginUseCase: 5 intentos fallidos -> 15 minutos bloqueado.
        assertEquals(5, AuthPolicy.MAX_INTENTOS)
        assertEquals(15, AuthPolicy.MINUTOS_DE_BLOQUEO)
        assertEquals(15 * 60, AuthPolicy.SEGUNDOS_DE_BLOQUEO)
        assertTrue(AuthPolicy.SEGUNDOS_DE_BLOQUEO > 0)
    }
}
