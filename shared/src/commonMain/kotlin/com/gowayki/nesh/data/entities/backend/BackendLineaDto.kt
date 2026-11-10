package com.gowayki.nesh.data.entities.backend

import kotlinx.serialization.Serializable

@Serializable
data class BackendPuntoDto(
    val id: Int? = null,
    val nombre: String? = null,
    val orden: Int? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val radioToleranciaMetros: Int? = null,
)

@Serializable
data class BackendLineaDto(
    val id: Int,
    val nombre: String? = null,
    val origen: String? = null,
    val destino: String? = null,
    val corredor: String? = null,
    val colorHex: String? = null,
    val paraderos: List<BackendPuntoDto>? = null,
)
