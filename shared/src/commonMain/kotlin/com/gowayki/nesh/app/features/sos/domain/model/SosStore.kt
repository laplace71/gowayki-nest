// Persistencia SOS multiplataforma (puntos reales del botón).
// Sobrevive al cerrar la app. Sin librerías nuevas: SharedPreferences / NSUserDefaults.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.sos.domain.model

data class SosStoredPoint(
    val id: String,
    val lat: Double,
    val lon: Double,
    val title: String,
    val timeMillis: Long,
)

expect object SosStore {
    suspend fun save(points: List<SosStoredPoint>)
    suspend fun load(): List<SosStoredPoint>
}
