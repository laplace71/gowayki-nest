package com.gowayki.nesh

import com.gowayki.nesh.app.features.home.presentation.components.unidadEsDeSubruta
import com.gowayki.nesh.data.entities.backend.BackendUnidadDto
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnidadSubrutaTest {

    private val combiDeLaSubruta355 = BackendUnidadDto(
        id = 10,
        numeroPadron = "unidad 11",
        placa = "v-h300",
        subrutaId = 355,
        subrutaCodigo = "SB-17654231",
        subrutaNombre = "Socabaya: B: Horacio Zevallos → Pachacutec",
        estado = "OPERATIVA",
    )

    @Test
    fun `la combi aparece por el id de subruta que trae el turno`() {
        assertTrue(unidadEsDeSubruta(combiDeLaSubruta355, codigo = null, subrutaDbId = 355))
        assertTrue(unidadEsDeSubruta(combiDeLaSubruta355, codigo = "SB-17654231", subrutaDbId = null))
        assertFalse(unidadEsDeSubruta(combiDeLaSubruta355, codigo = null, subrutaDbId = 254))
    }
}
