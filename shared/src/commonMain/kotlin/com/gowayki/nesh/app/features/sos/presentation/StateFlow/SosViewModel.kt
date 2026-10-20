// SosViewModel — puente entre botón físico ESP32 (BLE) y punto GPS en el mapa.
// No toca Auth ni Reports: ReportsScreen lo instancia aparte y le pasa onSosPoint.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.sos.presentation.StateFlow

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gowayki.nesh.app.features.sos.domain.model.BleDevice
import com.gowayki.nesh.app.features.sos.domain.model.BleSosClient
import com.gowayki.nesh.app.features.sos.domain.model.GpsFix
import com.gowayki.nesh.app.features.sos.domain.model.GpsProvider
import com.gowayki.nesh.app.features.sos.domain.model.SosBleState
import com.gowayki.nesh.app.features.sos.domain.model.createBleSosClient
import com.gowayki.nesh.app.features.sos.domain.model.createGpsProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SosUiState(
    val ble: SosBleState = SosBleState(),
    val gps: GpsFix? = null,
    val locating: Boolean = false,
    val lastSosLat: Double? = null,
    val lastSosLon: Double? = null,
    val info: String = "Conecta tu ESP32 y presiona el botón (GPIO14).",
)

class SosViewModel(
    private val ble: BleSosClient = runCatching { createBleSosClient() }.getOrNull() ?: NoopFallbackBle(),
    private val gps: GpsProvider = runCatching { createGpsProvider() }.getOrNull() ?: NoopFallbackGps(),
    private val onSosPoint: (lat: Double, lon: Double) -> Unit = { _, _ -> },
    // Calibrar / ir a mi posición: centra el mapa en el GPS real.
    private val onLocate: (lat: Double, lon: Double) -> Unit = { _, _ -> },
) : ViewModel() {

    var ui by mutableStateOf(SosUiState())
        private set

    // Dispositivos cercanos estilo WiFi (WAYKI_SOS primero).
    val devices: kotlinx.coroutines.flow.StateFlow<List<BleDevice>> = ble.devices

    // Auto-conexión para ESP32 portátil: conecta solo al prenderlo.
    var autoConnect by mutableStateOf(true)
        private set

    private var collectJob: Job? = null
    private var autoJob: Job? = null

    init {
        collectJob = viewModelScope.launch {
            ble.state.collectLatest { s -> ui = ui.copy(ble = s) }
        }
        ble.setOnPress { handlePress() }
        // GPS inicial (último conocido, sin bloquear).
        viewModelScope.launch {
            runCatching { gps.lastFix() }.getOrNull()?.let { ui = ui.copy(gps = it) }
        }
        // Al entrar busca el ESP32 solo; si se apaga/pierde, reintenta cada 8 s.
        autoJob = viewModelScope.launch {
            delay(500)
            while (true) {
                ble.autoJoin = autoConnect
                val s = ble.state.value
                if (autoConnect && !s.connected && !s.scanning) {
                    ui = ui.copy(info = "Buscando WAYKI_SOS por Bluetooth…")
                    runCatching { ble.startScan() }
                }
                delay(8_000)
            }
        }
    }

    // Conexión manual desde la lista estilo WiFi.
    fun connectDevice(d: BleDevice) {
        autoConnect = true
        ble.autoJoin = true
        ui = ui.copy(info = "Conectando a ${d.name}…")
        runCatching { ble.connectDevice(d.address) }
    }

    fun connect() {
        autoConnect = true
        ui = ui.copy(info = "Buscando WAYKI_SOS por Bluetooth…")
        ble.startScan()
    }

    fun disconnect() {
        autoConnect = false
        ble.stop()
        ui = ui.copy(info = "Desconectado.")
    }

    fun refreshGps() {
        viewModelScope.launch {
            ui = ui.copy(locating = true, info = "Obteniendo GPS real…")
            val fix = runCatching { gps.currentFix() }.getOrNull()
                ?: runCatching { gps.lastFix() }.getOrNull()
            ui = if (fix != null) {
                ui.copy(gps = fix, locating = false, info = "GPS listo: ${fix.lat}, ${fix.lon}")
            } else {
                ui.copy(locating = false, info = "Sin GPS: activa ubicación y acepta permiso.")
            }
        }
    }

    // Punto manual (prueba sin ESP32): usa GPS real igual que el botón físico.
    fun simulatePress() = handlePress()

    // Calibrar mi posición: GPS fresco y vuela el mapa hasta ahí (sin crear punto).
    fun locateMe() {
        viewModelScope.launch {
            ui = ui.copy(locating = true, info = "Ubicando tu posición…")
            val fix = runCatching { gps.currentFix() }.getOrNull()
                ?: runCatching { gps.lastFix() }.getOrNull()
                ?: ui.gps
            if (fix == null) {
                ui = ui.copy(locating = false, info = "Sin GPS: activa ubicación.")
                return@launch
            }
            ui = ui.copy(
                gps = fix,
                locating = false,
                info = "Mi posición: ${fix.lat}, ${fix.lon}",
            )
            onLocate(fix.lat, fix.lon)
        }
    }

    private fun handlePress() {
        viewModelScope.launch {
            ui = ui.copy(locating = true, info = "¡SOS recibido! Marcando punto GPS…")
            val fix = runCatching { gps.currentFix() }.getOrNull()
                ?: runCatching { gps.lastFix() }.getOrNull()
                ?: ui.gps
            if (fix == null) {
                ui = ui.copy(locating = false, info = "SOS sin GPS: activa ubicación.")
                return@launch
            }
            ui = ui.copy(
                gps = fix,
                locating = false,
                lastSosLat = fix.lat,
                lastSosLon = fix.lon,
                info = "SOS marcado en ${fix.lat}, ${fix.lon}",
            )
            onSosPoint(fix.lat, fix.lon)
        }
    }

    override fun onCleared() {
        runCatching { ble.stop() }
        collectJob?.cancel()
        autoJob?.cancel()
    }
}

// Fallbacks internos por si el factory platform falla (nunca crashea la app).
private class NoopFallbackBle : BleSosClient {
    private val s = kotlinx.coroutines.flow.MutableStateFlow(SosBleState())
    override val state: kotlinx.coroutines.flow.StateFlow<SosBleState> = s
    private val d = kotlinx.coroutines.flow.MutableStateFlow(emptyList<BleDevice>())
    override val devices: kotlinx.coroutines.flow.StateFlow<List<BleDevice>> = d
    override var autoJoin: Boolean = false
    override fun startScan() {}
    override fun stop() {}
    override fun connectDevice(address: String) {}
    override fun setOnPress(listener: () -> Unit) {}
}

private class NoopFallbackGps : GpsProvider {
    override suspend fun currentFix(): GpsFix? = null
    override suspend fun lastFix(): GpsFix? = null
}
