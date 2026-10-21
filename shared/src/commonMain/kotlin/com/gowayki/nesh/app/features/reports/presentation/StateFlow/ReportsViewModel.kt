// Estado único de reportes que observa la pantalla (mismo patrón que
// AuthViewModel en sign_in: UiState + intents, repositorio inyectable).
// El mapa y sus tiles viven acá: sobreviven a rotación y se testean sin UI.
//
// FASE JSON/BACKEND: cambia demoReports() por un ReportsRepository,
// igual que FakeAuthRepository en WaykiNav.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.reports.presentation.StateFlow

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gowayki.nesh.app.common.util.currentTimeMillis
import com.gowayki.nesh.app.features.reports.domain.model.LimaPlaza
import com.gowayki.nesh.app.features.reports.domain.model.OsmLatLng
import com.gowayki.nesh.app.features.reports.domain.model.OsmMarkerType
import com.gowayki.nesh.app.features.reports.domain.model.ReportItem
import com.gowayki.nesh.app.features.reports.domain.model.ReportStatus
import com.gowayki.nesh.app.features.reports.presentation.components.OsmMapState
import com.gowayki.nesh.app.features.sos.domain.model.SosMemory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.pow

data class ReportsUiState(
    // Sin presets de muestra: solo puntos reales (botón SOS o backend futuro).
    val reports: List<ReportItem> = emptyList(),
    val selectedId: String = "",
    // Sube en cada select (aunque sea el mismo punto) para re-centrar el mapa.
    val focusSeq: Int = 0,
    // Vuelo libre a una coordenada (mi posición): el mapa lo consume y vuela.
    val locatePos: OsmLatLng? = null,
    val locateSeq: Int = 0,
    val expanded: Boolean = false,
    val fullMap: Boolean = false,
    // 0 = home · 1 = alerta · 2 = mapa.
    val tab: Int = 2,
)

class ReportsViewModel : ViewModel() {

    var ui by mutableStateOf(ReportsUiState())
        private set

    // Cámara del mapa: vive en el VM, sobrevive a rotación.
    // Al crear el VM se restaura la última cámara + puntos de SosMemory
    // para no reiniciar al salir y volver a la ventana.
    val map = OsmMapState(
        center = SosMemory.lastCenter ?: LimaPlaza,
        zoom = SosMemory.lastZoom ?: 15,
    )

    init {
        syncFromMemory()
        // Restaura lo guardado al cerrar la app y lo refleja en pantalla.
        viewModelScope.launch {
            SosMemory.restoreOnce()
            syncFromMemory()
        }
    }

    private fun syncFromMemory() {
        if (SosMemory.points.isNotEmpty()) {
            ui = ui.copy(
                reports = SosMemory.points.toList(),
                selectedId = SosMemory.points.firstOrNull()?.id ?: ui.selectedId,
            )
            SosMemory.lastCenter?.let { map.moveTo(it) }
        }
    }

    // ---------- Intents de pantalla ----------

    fun onReportClick(id: String) {
        if (id == ui.selectedId) {
            ui = ui.copy(expanded = !ui.expanded)
        } else {
            select(id)
        }
    }

    fun select(id: String) {
        ui = ui.copy(selectedId = id, focusSeq = ui.focusSeq + 1)
        // Reubica SIEMPRE, incluso si ya estaba seleccionada.
        ui.reports.firstOrNull { it.id == id }?.let { map.flyTo(it.pos) }
    }

    fun toggleFullMap() {
        val f = !ui.fullMap
        ui = ui.copy(fullMap = f, expanded = if (f) true else ui.expanded)
    }

    fun selectTab(i: Int) {
        ui = ui.copy(tab = i)
    }

    // ---------- SOS botón físico (ESP32 GPIO14 por BLE) ----------
    // Añade un punto EMERGENCIA con el GPS real del teléfono.
    // También lo guarda en SosMemory para no perderlo al salir de la ventana.
    fun addSosEmergency(lat: Double, lon: Double) {
        val pos = OsmLatLng(lat, lon)
        val id = "SOS-${currentTimeMillis()}"
        val item = ReportItem(
            id = id,
            title = "SOS botón físico · ${pos.lat.format6()}, ${pos.lon.format6()}",
            subtitle = "Pulsación ESP32 · GPS real",
            status = ReportStatus.ACTIVA,
            pos = pos,
            type = OsmMarkerType.EMERGENCIA,
        )
        SosMemory.add(item)
        viewModelScope.launch { SosMemory.persist() }
        ui = ui.copy(
            reports = listOf(item) + ui.reports,
            selectedId = id,
            // focusSeq: el mapa (Mapbox) vuela al punto recién marcado.
            focusSeq = ui.focusSeq + 1,
            tab = 2,
        )
        map.flyTo(pos)
    }

    // Elimina un punto (swipe en la tarjeta SOS). Persiste el resto.
    fun removeReport(id: String) {
        SosMemory.remove(id)
        val rest = ui.reports.filter { it.id != id }
        ui = ui.copy(
            reports = rest,
            selectedId = if (ui.selectedId == id) (rest.firstOrNull()?.id ?: "") else ui.selectedId,
        )
        viewModelScope.launch { SosMemory.persist() }
    }

    // Centra el mapa en una coordenada libre (mi posición GPS).
    // OSM lo consume directo; Mapbox vía locateSeq en SosHeatMap.
    fun focusOn(lat: Double, lon: Double) {
        val pos = OsmLatLng(lat, lon)
        ui = ui.copy(locatePos = pos, locateSeq = ui.locateSeq + 1)
        map.flyTo(pos)
    }

    private fun Double.format6(): String = runCatching {
        kotlin.math.round(this * 1_000_000) / 1_000_000.0
    }.getOrDefault(this).toString()

    // ---------- Tiles OSM (LRU + máx 6 en vuelo) ----------
    // Cada tile 256x256x4 = 256 KB: tope 128 = ~32 MB máx.

    val tiles = mutableStateMapOf<String, ImageBitmap?>()
    private val tileOrder = ArrayDeque<String>()
    private val tilePermits = Semaphore(6)
    private var client: HttpClient? = null

    fun ensureTiles(z: Int, x0: Int, y0: Int, x1: Int, y1: Int) {
        val n = 2.0.pow(z).toInt()
        viewModelScope.launch {
            for (tx in x0..x1) for (ty in y0..y1) {
                val wx = ((tx % n) + n) % n
                if (ty < 0 || ty >= n) continue
                val key = "$z/$wx/$ty"
                if (tiles.containsKey(key)) continue
                tilePut(key, null)
                // Descarga+decode fuera del hilo principal para no trabar el mapa.
                launch(Dispatchers.Default) {
                    tilePermits.withPermit {
                        runCatching {
                            val bytes = osmClient()
                                .get("https://tile.openstreetmap.org/$z/$wx/$ty.png") {
                                    header("User-Agent", "WaykiNest/1.0 (contacto@wayki.pe)")
                                }.bodyAsBytes()
                            decodeTile(bytes)?.let { tilePut(key, it) }
                        }
                    }
                }
            }
        }
    }

    private fun tilePut(key: String, bmp: ImageBitmap?) {
        tiles[key] = bmp
        tileOrder.remove(key)
        tileOrder.addLast(key)
        while (tileOrder.size > 128) {
            tiles.remove(tileOrder.removeFirst())
        }
    }

    private fun osmClient(): HttpClient = client ?: HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 8_000
            socketTimeoutMillis = 15_000
        }
    }.also { client = it }

    private fun decodeTile(bytes: ByteArray): ImageBitmap? =
        runCatching { bytes.decodeToImageBitmap() }.getOrNull()

    // ---------- Zona del punto SOS (geocodificación inversa, solo añadido) ----------
    // Nominatim OSM: convierte el GPS real en zona de referencia para la tarjeta
    // (calle + anexo/barrio + distrito). Caché en SosMemory para no repetir.

    suspend fun zoneName(lat: Double, lon: Double): String? {
        val key = "${(lat * 1000).toInt()}/${(lon * 1000).toInt()}"
        val cache = SosMemory.zoneCache
        if (cache.containsKey(key)) return cache[key]
        val name = runCatching {
            val json = osmClient()
                .get("https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$lat&lon=$lon&zoom=18&addressdetails=1") {
                    header("User-Agent", "WaykiNest/1.0 (contacto@wayki.pe)")
                }.bodyAsBytes().decodeToString()
            val obj = kotlinx.serialization.json.Json.parseToJsonElement(json)
                .jsonObject
            val addr = obj["address"]?.jsonObject
            fun f(vararg keys: String): String? =
                keys.firstNotNullOfOrNull { addr?.get(it)?.jsonPrimitive?.content }
            // Calle + número (referencia cercana exacta).
            val street = listOfNotNull(f("house_number"), f("road"))
                .joinToString(" ").ifBlank { null }
                ?: f("footway", "path", "pedestrian", "place")
            // Anexo / barrio / caserío.
            val anexo = f("hamlet", "village", "suburb", "neighbourhood", "quarter", "isolated_dwelling")
            // Distrito + ciudad/departamento.
            val district = f("city_district", "district", "city", "town", "municipality", "county")
            val dept = f("state", "region")
            val line2 = listOfNotNull(anexo, district).joinToString(" · ").ifBlank { null }
            listOfNotNull(street, line2, dept)
                .joinToString("\n").ifBlank { null }
                ?: obj["display_name"]?.jsonPrimitive?.content?.split(",")?.take(3)?.joinToString(",")
        }.getOrNull()
        cache[key] = name
        return name
    }

    override fun onCleared() {
        client?.close()
        client = null
    }
}
