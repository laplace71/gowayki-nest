package com.gowayki.nesh.data.entities.backend

import kotlinx.serialization.Serializable

@Serializable
data class BackendSubrutaDto(
    val id: Int? = null,
    val lineaCodigo: String? = null,
    val lineaNombre: String? = null,
    val codigo: String? = null,
    val nombre: String? = null,
    val operador: String? = null,
    val colorHex: String? = null,
    val routeIdIda: String? = null,
    val routeIdRetorno: String? = null,
    val nombreRetorno: String? = null,
    val activa: Boolean? = null,
)

@Serializable
data class BackendLineaCatalogoDto(
    val codigo: String? = null,
    val nombre: String? = null,
    val colorHex: String? = null,
    val subrutas: List<BackendSubrutaDto> = emptyList(),
)

@Serializable
data class BackendCatalogoRutasDto(
    val lineas: List<BackendLineaCatalogoDto> = emptyList(),
)

@Serializable
data class BackendParaderoRutaDto(
    val id: String? = null,
    val name: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val district: String? = null,
    val orden: Int? = null,
)
