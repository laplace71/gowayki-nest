package com.gowayki.nesh.data.entities.backend

import kotlinx.serialization.Serializable

/** Cuerpo tipado para `POST /api/unidades`: evita mapas con tipos mezclados. */
@Serializable
data class CrearUnidadRequestDto(
    val numeroPadron: String,
    val placa: String,
    val lineaId: Int,
    val subrutaId: Int? = null,
)
