package com.gowayki.nesh.data.entities.backend

import kotlinx.serialization.Serializable

/**
 * Turno (jornada) de un chofer con una unidad concreta.
 *
 * El backend relaciona al chofer con la línea de forma indirecta:
 * chofer -> asignacion_turno -> unidad -> linea.
 */
@Serializable
data class BackendTurnoDto(
    val id: String,
    val choferId: String? = null,
    val choferNombre: String? = null,
    val unidadId: Int? = null,
    val numeroPadron: String? = null,
    val placa: String? = null,
    val lineaId: Int? = null,
    val lineaNombre: String? = null,
    val lineaColorHex: String? = null,
    val subrutaId: Int? = null,
    val subrutaCodigo: String? = null,
    val subrutaNombre: String? = null,
    val fecha: String? = null,
    val horaInicio: String? = null,
    val horaFin: String? = null,
    val estado: String? = null,
    val porcentajeCumplimiento: Double? = null,
)

/** Unidad (combi/bus) que pertenece a una línea. */
@Serializable
data class BackendUnidadDto(
    val id: Int,
    val numeroPadron: String? = null,
    val placa: String? = null,
    val lineaId: Int? = null,
    val lineaNombre: String? = null,
    val subrutaId: Int? = null,
    val subrutaCodigo: String? = null,
    val subrutaNombre: String? = null,
    val estado: String? = null,
    val posicionActual: PosicionGpsDto? = null,
)

@Serializable
data class ActualizarPosicionGpsRequestDto(
    val latitud: Double,
    val longitud: Double,
    val velocidadKmh: Double? = 0.0,
    val rumboGrados: Int? = 0,
)

@Serializable
data class PosicionGpsDto(
    val unidadId: Int? = null,
    val lineaId: Int? = null,
    val numeroPadron: String? = null,
    val latitud: Double,
    val longitud: Double,
    val velocidadKmh: Double? = null,
    val rumboGrados: Int? = null,
    val ultimaActualizacion: String? = null,
)

/** Cuerpo para POST /api/turnos/asignar. */
@Serializable
data class AsignarTurnoRequestDto(
    val choferId: String,
    val unidadId: Int,
    val subrutaId: Int? = null,
    val subrutaCodigo: String? = null,
    val fecha: String? = null,
    val horaInicio: String? = null,
    val horaFin: String? = null,
)

/** Respuesta de error simple del backend: { "exito": false, "mensaje": "..." }. */
@Serializable
data class BackendMensajeDto(
    val exito: Boolean? = null,
    val mensaje: String? = null,
)
