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
import com.gowayki.nesh.app.features.reports.domain.model.LimaPlaza
import com.gowayki.nesh.app.features.reports.domain.model.ReportItem
import com.gowayki.nesh.app.features.reports.domain.model.demoReports
import com.gowayki.nesh.app.features.reports.presentation.components.OsmMapState
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.pow

data class ReportsUiState(
    val reports: List<ReportItem> = demoReports(),
    val selectedId: String = "08",
    val expanded: Boolean = false,
    val fullMap: Boolean = false,
    // 0 = home · 1 = alerta · 2 = mapa.
    val tab: Int = 2,
)

class ReportsViewModel : ViewModel() {

    var ui by mutableStateOf(ReportsUiState())
        private set

    // Cámara del mapa: vive en el VM, sobrevive a rotación.
    val map = OsmMapState(center = LimaPlaza, zoom = 15)

    // ---------- Intents de pantalla ----------

    fun onReportClick(id: String) {
        if (id == ui.selectedId) {
            ui = ui.copy(expanded = !ui.expanded)
        } else {
            select(id)
        }
    }

    fun select(id: String) {
        ui = ui.copy(selectedId = id)
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

    override fun onCleared() {
        client?.close()
        client = null
    }
}
