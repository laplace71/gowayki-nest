package com.gowayki.nesh

import com.gowayki.nesh.data.entities.backend.BackendUnidadDto
import com.gowayki.nesh.data.entities.backend.PosicionGpsNormalizer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertTrue

class UnidadesJsonTest {
    private val jsonReal = """
[{"id":1,"numeroPadron":"08","placa":"V1A-954","lineaId":1,"lineaNombre":"Cono Norte","subrutaId":259,"subrutaCodigo":"SB-17620010","subrutaNombre":"4A: Ciudad de Dios → Terminal Terrestre","estado":"OPERATIVA","posicionActual":{"unidadId":null,"lineaId":null,"numeroPadron":null,"latitud":-16.42956,"longitud":-71.51926833333333,"velocidadKmh":0.0,"rumboGrados":0,"ultimaActualizacion":"2026-11-23T13:15:36.020767Z"}},{"id":3,"numeroPadron":"15","placa":"V4C-889","lineaId":2,"lineaNombre":"Transcayma","subrutaId":286,"subrutaCodigo":"SB-17659544","subrutaNombre":"Embajada: Los Pioneros → Terminal Terrestre","estado":"OPERATIVA","posicionActual":{"unidadId":null,"lineaId":null,"numeroPadron":null,"latitud":-16.3609782,"longitud":-71.525276,"velocidadKmh":0.0,"rumboGrados":100,"ultimaActualizacion":"2026-11-23T04:39:42.121091Z"}},{"id":8,"numeroPadron":"unidad 10","placa":"v-h200","lineaId":8,"lineaNombre":"Emarsistran","subrutaId":355,"subrutaCodigo":"SB-17654231","subrutaNombre":"Socabaya: B: Horacio Zevallos → Pachacutec","estado":"OPERATIVA","posicionActual":null},{"id":10,"numeroPadron":"unidad 11","placa":"v-h300","lineaId":8,"lineaNombre":"Emarsistran","subrutaId":355,"subrutaCodigo":"SB-17654231","subrutaNombre":"Socabaya: B: Horacio Zevallos → Pachacutec","estado":"OPERATIVA","posicionActual":null}]
    """.trimIndent()

    @Test
    fun `el JSON real de unidades se convierte en DTO`() {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }
        val unidades = json.decodeFromString<List<BackendUnidadDto>>(jsonReal)
        assertTrue(unidades.size >= 2, "Se esperaban al menos 2 unidades, vinieron ${unidades.size}")
        assertTrue(unidades.any { it.subrutaId == 355 }, "Ninguna unidad tiene subruta 355")
    }

    @Test
    fun `el normalizador acepta lat y lng sin recursion`() {
        val json = Json { ignoreUnknownKeys = true }
        val pos = json.decodeFromString(PosicionGpsNormalizer, """{"lat":-16.4,"lng":-71.5}""")
        assertTrue(pos.latitud == -16.4 && pos.longitud == -71.5, "lat=${pos.latitud} lng=${pos.longitud}")
    }
}
