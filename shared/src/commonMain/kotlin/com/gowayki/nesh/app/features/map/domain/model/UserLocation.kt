package com.gowayki.nesh.app.features.map.domain.model

/**
 * Posición real del usuario sobre el mapa.
 *
 * @param bearingDeg rumbo en grados (0 = norte) si el GPS lo conoce.
 * @param speedMps velocidad en metros por segundo, si está disponible.
 * @param accuracyM precisión estimada en metros.
 */
data class UserLocation(
    val lat: Double,
    val lon: Double,
    val bearingDeg: Float? = null,
    val speedMps: Float? = null,
    val accuracyM: Float? = null,
)
