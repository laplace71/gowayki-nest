// Modelo del historial de reportes: puntos geográficos + fichas.
// Datos de mentira hasta conectar el backend (igual que FakeAuthRepository
// en sign_in: se cambia en un solo lugar).
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.reports.domain.model

data class OsmLatLng(val lat: Double, val lon: Double)

enum class OsmMarkerType { EMERGENCIA, UNIDAD, INCIDENCIA }

enum class ReportStatus(val label: String) {
    ACTIVA("ACTIVA"),
    ATENDIDA("ATENDIDA"),
    CERRADA("CERRADA"),
}

data class ReportItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val status: ReportStatus,
    val pos: OsmLatLng,
    val type: OsmMarkerType,
)

// Lima centro (como en el video).
val LimaPlaza = OsmLatLng(-12.0464, -77.0422)

// Datos del video.
fun demoReports() = listOf(
    ReportItem("08", "Unidad 08 · Plaza de A…", "Hoy 08:42 · Duración: 7 min", ReportStatus.ACTIVA, OsmLatLng(-12.0464, -77.0422), OsmMarkerType.UNIDAD),
    ReportItem("03", "Unidad 03 · Av. Independen…", "Hoy 07:15 · Duración: 12 min", ReportStatus.ATENDIDA, OsmLatLng(-12.0425, -77.0335), OsmMarkerType.EMERGENCIA),
    ReportItem("07", "Unidad 07 · Mercado San C…", "Ayer 18:32 · Duración: 5 min", ReportStatus.CERRADA, OsmLatLng(-12.0515, -77.0375), OsmMarkerType.INCIDENCIA),
)
