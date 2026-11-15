// Módulo SOS — modelos comunes. Solo añade, no toca Report.kt ni nada existente.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.sos.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Config BLE — debe coincidir con el firmware del ESP32.
// Sin WiFi, sin tokens: solo BLE puro.
object SosBleConfig {
    const val DEVICE_NAME = "WAYKI_SOS"
    // Prefijo común de los ESP32 Wayki con firmware único (WAYKI_96E6, WAYKI_97C6, …).
    const val DEVICE_NAME_PREFIX = "WAYKI_"
    // UUIDs generados para Wayki (cámbialos solo si cambias el .ino también).
    const val SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
    const val CHAR_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8"
    // Payloads que manda el ESP32 (deben coincidir con el firmware).
    //   SOS!        -> 1 clic  -> grabar audio
    //   SOS_TRIPLE  -> 3 clics -> punto GPS SOS + alerta
    //   SOS_ALERT   -> unidad cercana en emergencia
    const val PRESS_PAYLOAD = "SOS!"
    const val PAYLOAD_TRIPLE = "SOS_TRIPLE"
    const val PAYLOAD_REMOTE = "SOS_ALERT"
}

// Estado visible en UI.
data class SosBleState(
    val connected: Boolean = false,
    val scanning: Boolean = false,
    val deviceName: String? = null,
    val deviceAddress: String? = null,
    val lastPressAt: Long = 0L,
    val pressCount: Int = 0,
    val error: String? = null,
    // Nuevo: para detectar presiones múltiples (simple vs triple)
    val pendingPressCount: Int = 0,
    val lastPressTime: Long = 0L,
)

// Dispositivo BLE cercano (estilo lista WiFi).
data class BleDevice(
    val name: String,
    val address: String,
    val rssi: Int = -100,
    val isSos: Boolean = false,
)

data class GpsFix(
    val lat: Double,
    val lon: Double,
    val accuracyM: Float? = null,
)

// Contrato BLE — implementado por platform (androidMain real, iosMain stub).
interface BleSosClient {
    val state: StateFlow<SosBleState>
    // Lista de dispositivos cercanos, estilo WiFi (WAYKI_SOS primero).
    val devices: StateFlow<List<BleDevice>>
    // Si es true, al ver WAYKI_SOS conecta solo; si false, solo lista.
    var autoJoin: Boolean
    fun startScan()
    fun stop()
    fun connectDevice(address: String)
    fun setOnPress(listener: (PressType) -> Unit)
}

enum class PressType {
    SINGLE,  // SOS! -> grabar audio
    TRIPLE,  // SOS_TRIPLE -> punto GPS SOS
    REMOTE   // SOS_ALERT -> unidad cercana en emergencia
}

// Contrato GPS — GPS real del teléfono (LocationManager / CoreLocation).
interface GpsProvider {
    suspend fun currentFix(): GpsFix?
    suspend fun lastFix(): GpsFix?
}

// Factories platform. Los actual están en androidMain/iosMain.
expect fun createBleSosClient(): BleSosClient
expect fun createGpsProvider(): GpsProvider

// Fallbacks para Preview / tests (sin BLE ni GPS).
class NoopBleSosClient : BleSosClient {
    private val _state = MutableStateFlow(SosBleState(error = "BLE no disponible en Preview"))
    override val state: StateFlow<SosBleState> = _state.asStateFlow()
    private val _devices = MutableStateFlow(emptyList<BleDevice>())
    override val devices: StateFlow<List<BleDevice>> = _devices.asStateFlow()
    override var autoJoin: Boolean = false
    override fun startScan() {}
    override fun stop() {}
    override fun connectDevice(address: String) {}
    override fun setOnPress(listener: (PressType) -> Unit) {}
}

class NoopGpsProvider : GpsProvider {
    override suspend fun currentFix(): GpsFix? = null
    override suspend fun lastFix(): GpsFix? = null
}
