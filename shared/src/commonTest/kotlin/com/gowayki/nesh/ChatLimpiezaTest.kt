package com.gowayki.nesh

import com.gowayki.nesh.app.features.communication.domain.model.Conversation
import com.gowayki.nesh.data.repository.conversacionTrasLimpiar
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatLimpiezaTest {

    private val grupo = Conversation(
        id = "grupo-1",
        title = "GRUPO · Mi ruta",
        isGroup = true,
        preview = "jean: hola",
        lastMessageAtMillis = 100L,
        unreadCount = 2,
        avatarText = "GR",
        lastMessageId = "m-1",
    )

    @Test
    fun `limpiar conserva el grupo y solo oculta sus mensajes`() {
        val resultado = conversacionTrasLimpiar(grupo, "grupo-1")

        assertEquals("grupo-1", resultado.id)
        assertEquals("GRUPO · Mi ruta", resultado.title)
        assertEquals(true, resultado.isGroup)
        assertEquals("Sin mensajes", resultado.preview)
        assertEquals(0, resultado.unreadCount)
        assertEquals(null, resultado.lastMessageId)
    }

    @Test
    fun `limpiar otro chat no modifica este grupo`() {
        assertEquals(grupo, conversacionTrasLimpiar(grupo, "otro-grupo"))
    }
}
