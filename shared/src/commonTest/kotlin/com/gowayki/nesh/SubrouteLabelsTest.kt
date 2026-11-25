package com.gowayki.nesh

import com.gowayki.nesh.app.features.map.presentation.SubrouteLabels
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Una clave corrupta nunca debe aparecer como "Subruta: …" en la UI. */
class SubrouteLabelsTest {

    @Test
    fun clavesQueNoSonSubrutaNoSeMuestran() {
        assertNull(SubrouteLabels.nameOf(null))
        assertNull(SubrouteLabels.nameOf(""))
        assertNull(SubrouteLabels.nameOf("GRUPO_RUTA"))
        assertNull(SubrouteLabels.nameOf("GRUPO - 4C: Villa Milagros"))
        assertNull(SubrouteLabels.nameOf("C2"))
    }

    @Test
    fun unaClaveConFormatoDeSubrutaSeConserva() {
        val resultado = SubrouteLabels.nameOf("SB-17620010")
        assertNotNull(resultado)
        assertTrue(resultado.isNotBlank())
    }
}
