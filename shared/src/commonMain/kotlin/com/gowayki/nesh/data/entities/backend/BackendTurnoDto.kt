package com.gowayki.nesh.data.entities.backend

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer

/**
 * Django envía `lat`/`lng`, Spring `latitud`/`longitud`. Normalizamos antes de
 * decodificar para mantener los campos de dominio no nulos.
 */
object PosicionGpsNormalizer : JsonTransformingSerializer<PosicionGpsDto>(PosicionGpsDto.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element !is JsonObject) return element
        val campos = element.toMutableMap()
        val lat = campos["lat"]
        val lng = campos["lng"]
        val sinLat = campos["latitud"] == null || campos["latitud"] is kotlinx.serialization.json.JsonNull
        val sinLng = campos["longitud"] == null || campos["longitud"] is kotlinx.serialization.json.JsonNull
        if (sinLat && lat != null) campos["latitud"] = lat
        if (sinLng && lng != null) campos["longitud"] = lng
        return JsonObject(campos)
    }
}

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
    @Serializable(with = PosicionGpsNormalizer::class)
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
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
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
