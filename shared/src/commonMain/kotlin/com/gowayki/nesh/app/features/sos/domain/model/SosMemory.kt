// Memoria SOS — sobrevive a la navegación Y al cerrar la app (persistencia).
// Solo puntos reales del botón (sin presets). Guarda cámara + puntos + zonas.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.sos.domain.model

import androidx.compose.runtime.mutableStateListOf
import com.gowayki.nesh.app.features.reports.domain.model.OsmLatLng
import com.gowayki.nesh.app.features.reports.domain.model.ReportItem
import com.gowayki.nesh.app.features.reports.domain.model.ReportStatus
import com.gowayki.nesh.app.features.reports.domain.model.OsmMarkerType

object SosMemory {
    // Solo puntos reales del botón (sin presets de muestra).
    val points = mutableStateListOf<ReportItem>()

    // Última cámara del mapa SOS para restaurarla al volver.
    var lastCenter: OsmLatLng? = null
    var lastZoom: Int? = null

    // Zonas ya resueltas (sobreviven a la navegación).
    val zoneCache = mutableMapOf<String, String?>()

    private var loaded = false

    fun add(point: ReportItem) {
        points.add(0, point)
    }

    fun remove(id: String) {
        points.removeAll { it.id == id }
    }

    fun clear() {
        points.clear()
        zoneCache.clear()
        lastCenter = null
        lastZoom = null
    }

    // Restaura lo guardado al cerrar la app (una sola vez por proceso).
    suspend fun restoreOnce() {
        if (loaded) return
        loaded = true
        runCatching {
            SosStore.load().forEach { s ->
                points.add(
                    ReportItem(
                        id = s.id,
                        title = s.title,
                        subtitle = "Pulsación ESP32 · GPS real",
                        status = ReportStatus.ACTIVA,
                        pos = OsmLatLng(s.lat, s.lon),
                        type = OsmMarkerType.EMERGENCIA,
                    )
                )
            }
        }
    }

    suspend fun persist() {
        runCatching {
            SosStore.save(
                points.map {
                    SosStoredPoint(
                        id = it.id,
                        lat = it.pos.lat,
                        lon = it.pos.lon,
                        title = it.title,
                        timeMillis = it.id.removePrefix("SOS-").toLongOrNull() ?: 0L,
                    )
                }
            )
        }
    }
}
