// Memoria SOS — sobrevive a la navegación Y al cerrar la app (persistencia).
// Solo puntos reales del botón (sin presets). Guarda cámara + puntos + zonas.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.sos.domain.model

import androidx.compose.runtime.mutableStateListOf
import com.gowayki.nesh.app.common.util.currentTimeMillis
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

    // Contador simple para IDs únicos aunque coincidan milisegundos
    private var idCounter = 0

    /**
     * Crea y registra un punto de emergencia SOS con el GPS recibido.
     * La lista observable [points] es la fuente única: todas las pantallas
     * leen el mismo estado y sobrevive a la navegación y al cierre de la app.
     */
    fun addEmergency(lat: Double, lon: Double): ReportItem {
        val pos = OsmLatLng(lat, lon)
        val timestamp = currentTimeMillis()
        // Usamos timestamp + nanoTime + contador para garantizar unicidad
        val uniqueId = "SOS-${timestamp}-${kotlin.random.Random.nextLong()}"
        val item = ReportItem(
            id = uniqueId,
            title = "SOS botón físico · ${format6(pos.lat)}, ${format6(pos.lon)}",
            subtitle = "Pulsación ESP32 · GPS real",
            status = ReportStatus.ACTIVA,
            pos = pos,
            type = OsmMarkerType.EMERGENCIA,
        )
        points.add(0, item)
        com.gowayki.nesh.core.mqtt.MqttClient.publish(
            com.gowayki.nesh.core.mqtt.MqttTopics.MAP_ALERT,
            com.gowayki.nesh.core.mqtt.MqttTopics.MSG_MAP_ALERT
        )
        return item
    }

    private fun format6(value: Double): String =
        runCatching { kotlin.math.round(value * 1_000_000) / 1_000_000.0 }
            .getOrDefault(value)
            .toString()

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
            val existingIds = points.map { it.id }.toMutableSet()
            SosStore.load().forEach { s ->
                // Evita claves duplicadas si el almacenamiento tiene ids repetidos.
                val id = if (existingIds.contains(s.id)) {
                    "SOS-${s.id}-${kotlin.random.Random.nextLong()}"
                } else {
                    s.id
                }
                existingIds.add(id)
                points.add(
                    ReportItem(
                        id = id,
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
