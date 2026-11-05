// Actual Android — BLE real con APIs del SDK (sin librerías nuevas, sin WiFi, sin tokens).
// Escanea "WAYKI_SOS", conecta GATT, suscribe notify. Al recibir "SOS!" avisa onPress.
@file:Suppress("SpellCheckingInspection", "MissingPermission")
package com.gowayki.nesh.app.features.sos.domain.model

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import com.gowayki.nesh.app.di.AppDi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// Context de app registrado desde MainActivity (sin romper DI existente).
object AndroidSosContext {
    var appContext: Context? = null
}

private class AndroidBleSosClient(
    private val appContext: Context,
) : BleSosClient {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(SosBleState())
    override val state: StateFlow<SosBleState> = _state.asStateFlow()

    private var onPress: (PressType) -> Unit = { _ -> }
    private var gatt: BluetoothGatt? = null
    private var scanning = false
    private var scanTimeout: Job? = null
    override var autoJoin: Boolean = true

    // Para detectar presiones múltiples: ventana de 1.5s para triple click
    private var pressWindowJob: Job? = null
    private val TRIPLE_PRESS_WINDOW_MS = 1500L
    private val TRIPLE_PRESS_COUNT = 3

    // Dispositivos vistos en el scan actual (estilo WiFi).
    private val found = mutableMapOf<String, BleDevice>()
    private val _devices = MutableStateFlow(emptyList<BleDevice>())
    override val devices: StateFlow<List<BleDevice>> = _devices.asStateFlow()

    private fun publishDevices() {
        _devices.value = found.values.sortedWith(
            compareByDescending<BleDevice> { it.isSos }.thenByDescending { it.rssi },
        )
    }

    private val btAdapter: BluetoothAdapter? by lazy {
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    private val scanCb = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val address = result.device.address ?: return
            val name = result.device.name ?: result.scanRecord?.deviceName
            val matchesName = name == SosBleConfig.DEVICE_NAME
            val matchesService = result.scanRecord?.serviceUuids?.any {
                it.uuid.toString().equals(SosBleConfig.SERVICE_UUID, ignoreCase = true)
            } == true
            val isSos = matchesName || matchesService
            // Lista estilo WiFi: se muestra todo lo cercano con nombre.
            if (name != null || isSos) {
                found[address] = BleDevice(
                    name = name ?: SosBleConfig.DEVICE_NAME,
                    address = address,
                    rssi = result.rssi,
                    isSos = isSos,
                )
                publishDevices()
            }
            if (isSos && autoJoin) {
                stopScan()
                connect(address)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _state.value = _state.value.copy(scanning = false, error = "Scan falló ($errorCode)")
        }
    }

    private val gattCb = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                scope.launch {
                    _state.value = _state.value.copy(
                        connected = true,
                        deviceName = gatt.device.name ?: SosBleConfig.DEVICE_NAME,
                        deviceAddress = gatt.device.address,
                        error = null,
                    )
                }
                try {
                    gatt.discoverServices()
                } catch (e: Exception) {
                    e.printStackTrace()
                    scope.launch {
                        _state.value = _state.value.copy(error = "Error descubriendo servicios: ${e.message}")
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                scope.launch {
                    _state.value = _state.value.copy(connected = false, deviceName = null, deviceAddress = null)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                scope.launch {
                    _state.value = _state.value.copy(error = "Error descubriendo servicios: $status")
                }
                return
            }
            val svc = gatt.getService(UUID.fromString(SosBleConfig.SERVICE_UUID)) ?: return
            val ch = svc.getCharacteristic(UUID.fromString(SosBleConfig.CHAR_UUID)) ?: return
            try {
                gatt.setCharacteristicNotification(ch, true)
                // Descriptor CCCD para activar notify.
                val cccd = ch.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                if (cccd != null) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        gatt.writeDescriptor(cccd, android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    } else {
                        @Suppress("DEPRECATION")
                        cccd.value = android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        @Suppress("DEPRECATION")
                        gatt.writeDescriptor(cccd)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                scope.launch {
                    _state.value = _state.value.copy(error = "Error configurando notificaciones: ${e.message}")
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            handleBytes(characteristic.value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleBytes(value)
        }
    }

    private fun handleBytes(bytes: ByteArray?) {
        val txt = runCatching { bytes?.toString(Charsets.UTF_8)?.trim() }.getOrNull() ?: return
        // El ESP32 manda "SOS!" (o "1" como fallback). Cualquiera dispara el punto GPS.
        if (txt == SosBleConfig.PRESS_PAYLOAD || txt == "1" || txt.contains("SOS")) {
            val now = System.currentTimeMillis()
            // Usamos un Mutex para evitar race conditions en el estado de presses
            scope.launch {
                // Capturamos el estado actual de forma atómica
                val s = _state.value
                val timeSinceLastPress = if (s.lastPressTime > 0) now - s.lastPressTime else Long.MAX_VALUE

                // Si pasó más de la ventana, reseteamos el contador
                val effectiveCount = if (timeSinceLastPress > TRIPLE_PRESS_WINDOW_MS) 1 else s.pendingPressCount + 1

                _state.value = s.copy(
                    lastPressAt = now,
                    pressCount = s.pressCount + 1,
                    pendingPressCount = effectiveCount,
                    lastPressTime = now,
                )

                // Cancelamos cualquier ventana anterior de forma segura
                pressWindowJob?.cancel()
                pressWindowJob = null

                if (effectiveCount >= TRIPLE_PRESS_COUNT) {
                    // Triple presión detectada -> SOS
                    _state.value = _state.value.copy(pendingPressCount = 0)
                    // Llamamos onPress fuera del bloque de estado para evitar deadlocks
                    try {
                        onPress(PressType.TRIPLE)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    // Programamos verificación de presión simple
                    pressWindowJob = scope.launch {
                        delay(TRIPLE_PRESS_WINDOW_MS)
                        val currentState = _state.value
                        // Verificamos que sigue siendo una presión simple válida
                        if (currentState.pendingPressCount == 1 && currentState.lastPressTime == now) {
                            _state.value = _state.value.copy(pendingPressCount = 0)
                            try {
                                onPress(PressType.SINGLE)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun startScan() {
        val adapter = btAdapter
        if (adapter == null || !adapter.isEnabled) {
            _state.value = _state.value.copy(error = "Activa el Bluetooth")
            return
        }
        try {
            val scanner = adapter.bluetoothLeScanner ?: run {
                _state.value = _state.value.copy(error = "Sin scanner BLE")
                return
            }
            stopScan()
            found.clear()
            publishDevices()
            scanning = true
            _state.value = _state.value.copy(scanning = true, error = null)
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            scanner.startScan(null, settings, scanCb)
            // El scan no queda eterno si el ESP32 está apagado: corta a los 20 s.
            scanTimeout?.cancel()
            scanTimeout = scope.launch {
                delay(20_000)
                stopScan()
            }
        } catch (e: SecurityException) {
            _state.value = _state.value.copy(scanning = false, error = "Faltan permisos BLE/ubicación")
        } catch (e: Exception) {
            _state.value = _state.value.copy(scanning = false, error = "Error al escanear: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    override fun stop() {
        stopScan()
        pressWindowJob?.cancel()
        pressWindowJob = null
        try { gatt?.disconnect() } catch (_: SecurityException) {}
        try { gatt?.close() } catch (_: Exception) {}
        gatt = null
        scope.launch {
            _state.value = _state.value.copy(connected = false, scanning = false, deviceName = null, deviceAddress = null, pendingPressCount = 0)
        }
    }

    // Conexión manual desde la lista estilo WiFi.
    @SuppressLint("MissingPermission")
    override fun connectDevice(address: String) {
        if (_state.value.connected && _state.value.deviceAddress == address) return
        stopScan()
        scope.launch {
            _state.value = _state.value.copy(deviceName = found[address]?.name, deviceAddress = address, error = null)
        }
        connect(address)
    }

    override fun setOnPress(listener: (PressType) -> Unit) {
        onPress = listener
    }

    @SuppressLint("MissingPermission")
    private fun connect(address: String) {
        try {
            val device = btAdapter?.getRemoteDevice(address) ?: return
            gatt?.close()
            gatt = device.connectGatt(appContext, false, gattCb, BluetoothDeviceTransportSafe)
        } catch (e: SecurityException) {
            _state.value = _state.value.copy(error = "Sin permiso BLUETOOTH_CONNECT")
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = "Error de conexión: ${e.message}")
        }
    }

    private fun stopScan() {
        if (!scanning) return
        scanning = false
        scanTimeout?.cancel()
        scanTimeout = null
        try { btAdapter?.bluetoothLeScanner?.stopScan(scanCb) } catch (_: Exception) {}
        scope.launch { _state.value = _state.value.copy(scanning = false) }
    }

    // Compat: en API 23+ el transporte LE es default con connectGatt(ctx, autoConnect, cb).
    companion object {
        private const val BluetoothDeviceTransportSafe = 2 // TRANSPORT_LE
    }

    @Suppress("unused")
    fun destroy() = scope.cancel()
}

// Factory llamada desde common. Si MainActivity aún no registró contexto,
// devuelve un Noop para no crashear Preview/tests.
actual fun createBleSosClient(): BleSosClient {
    val ctx = AndroidSosContext.appContext ?: return NoopBleSosClient()
    return AndroidBleSosClient(ctx)
}
