package com.gowayki.nesh.app.features.sos.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.gowayki.nesh.core.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val SOS_SERVICE_UUID: UUID = UUID.fromString("6f1a0001-7c3b-4f2e-9a5d-2f8c1e0b9a01")
private val SOS_ALERT_CHAR_UUID: UUID = UUID.fromString("6f1a0002-7c3b-4f2e-9a5d-2f8c1e0b9a01")
private val SOS_ACK_CHAR_UUID: UUID = UUID.fromString("6f1a0003-7c3b-4f2e-9a5d-2f8c1e0b9a01")
private val CCC_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
private const val SOS_DEVICE_ID = "ESP32-AQP-017"

@Serializable
private data class BleSosEvent(
    val eventId: String,
    val deviceId: String,
    val occurredAt: String,
)

@SuppressLint("MissingPermission")
@Composable
actual fun rememberSosBleButton(
    enabled: Boolean,
    onAlertForwarded: () -> Unit,
): SosBleButtonState {
    val context = LocalContext.current
    val appContext = context.applicationContext
    var state by remember { mutableStateOf(SosBleButtonState()) }

    val bluetoothManager = remember {
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }
    val adapter = bluetoothManager?.adapter

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    val hasPermissions = requiredPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = requiredPermissions.all { grants[it] == true }
        if (!granted) {
            state = state.copy(error = "Permite Bluetooth para enlazar el botón SOS.")
        }
    }

    LaunchedEffect(enabled, hasPermissions) {
        if (!enabled || hasPermissions) return@LaunchedEffect
        permissionLauncher.launch(requiredPermissions)
    }

    DisposableEffect(enabled, hasPermissions, adapter) {
        if (!enabled || !hasPermissions || adapter == null || !adapter.isEnabled ||
            AppConfig.SOS_API_BASE_URL.isBlank()
        ) {
            if (adapter != null && !adapter.isEnabled && enabled) {
                state = state.copy(error = "Activa el Bluetooth del teléfono.")
            }
            onDispose { }
        } else {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val client = HttpClient {
                install(HttpTimeout) {
                    requestTimeoutMillis = 8_000
                    connectTimeoutMillis = 4_000
                }
            }
            val json = Json { ignoreUnknownKeys = true }
            val baseUrl = AppConfig.SOS_API_BASE_URL.trimEnd('/')
            var gatt: BluetoothGatt? = null

            fun forwardAlert(payload: String, ack: () -> Unit) {
                // La placa envía "SOS:<n>" (payload corto por el límite de ~20 bytes
                // de BLE); el teléfono reconstruye el evento completo con la hora actual.
                val sequence = payload.removePrefix("SOS:").toLongOrNull()
                if (sequence == null) {
                    state = state.copy(error = "Alerta BLE con formato inválido: $payload")
                    return
                }
                val event = BleSosEvent(
                    eventId = "evt-ble-$sequence",
                    deviceId = SOS_DEVICE_ID,
                    occurredAt = java.time.Instant.now().toString(),
                )
                val body = json.encodeToString(BleSosEvent.serializer(), event)
                scope.launch {
                    try {
                        val response = client.post("$baseUrl/api/sos/alerts") {
                            contentType(ContentType.Application.Json)
                            setBody(body)
                        }
                        if (response.status.value in 200..299 || response.status.value == 429) {
                            ack()
                            state = state.copy(lastForwardedEventId = event.eventId, error = null)
                            onAlertForwarded()
                        } else {
                            state = state.copy(
                                error = response.bodyAsText().takeIf(String::isNotBlank)
                                    ?: "El servidor rechazó la alerta (${response.status.value}).",
                            )
                        }
                    } catch (error: Exception) {
                        state = state.copy(
                            error = error.message ?: "No se pudo reenviar la alerta al servidor.",
                        )
                    }
                }
            }

            val gattCallback = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            state = state.copy(connecting = true, error = null)
                            g.discoverServices()
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            state = state.copy(connected = false, connecting = false)
                            g.close()
                            if (gatt == g) gatt = null
                        }
                    }
                }

                override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                    val characteristic = g.getService(SOS_SERVICE_UUID)
                        ?.getCharacteristic(SOS_ALERT_CHAR_UUID)
                    if (characteristic == null) {
                        state = state.copy(connecting = false, error = "El botón no expone el servicio SOS.")
                        g.disconnect()
                        return
                    }
                    if (!g.setCharacteristicNotification(characteristic, true)) {
                        state = state.copy(connecting = false, error = "No se pudo suscribir a las alertas del botón.")
                        g.disconnect()
                        return
                    }
                    val descriptor = characteristic.getDescriptor(CCC_DESCRIPTOR_UUID)
                    if (descriptor == null) {
                        state = state.copy(connecting = false, error = "Falta el descriptor de notificaciones.")
                        g.disconnect()
                        return
                    }
                    // La suscripción termina cuando onDescriptorWrite confirma el CCC.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        g.writeDescriptor(
                            descriptor,
                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        @Suppress("DEPRECATION")
                        g.writeDescriptor(descriptor)
                    }
                }

                override fun onDescriptorWrite(
                    g: BluetoothGatt,
                    descriptor: BluetoothGattDescriptor,
                    status: Int,
                ) {
                    if (descriptor.uuid != CCC_DESCRIPTOR_UUID) return
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        state = state.copy(
                            connected = true,
                            connecting = false,
                            deviceName = g.device.name,
                            error = null,
                        )
                    } else {
                        state = state.copy(
                            connecting = false,
                            error = "El botón no aceptó la suscripción (GATT $status).",
                        )
                        g.disconnect()
                    }
                }

                @Deprecated("Deprecated in Android API")
                override fun onCharacteristicChanged(
                    g: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                ) {
                    if (characteristic.uuid != SOS_ALERT_CHAR_UUID) return
                    @Suppress("DEPRECATION")
                    val payload = characteristic.value?.decodeToString() ?: return
                    forwardAlert(payload) { writeAck(g) }
                }

                override fun onCharacteristicChanged(
                    g: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray,
                ) {
                    if (characteristic.uuid != SOS_ALERT_CHAR_UUID) return
                    forwardAlert(value.decodeToString()) { writeAck(g) }
                }

                private fun writeAck(g: BluetoothGatt) {
                    val ack = g.getService(SOS_SERVICE_UUID)
                        ?.getCharacteristic(SOS_ACK_CHAR_UUID) ?: return
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        g.writeCharacteristic(
                            ack,
                            "ok".encodeToByteArray(),
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        ack.value = "ok".encodeToByteArray()
                        @Suppress("DEPRECATION")
                        g.writeCharacteristic(ack)
                    }
                }
            }

            val scanner = adapter.bluetoothLeScanner
            if (scanner == null) {
                state = state.copy(connecting = false, error = "El escáner Bluetooth no está disponible.")
                onDispose {
                    client.close()
                    scope.cancel()
                }
            } else {
            var scanning = false
            val scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val device: BluetoothDevice = result.device
                    scanning = false
                    runCatching { scanner.stopScan(this) }
                    state = state.copy(connecting = true, deviceName = device.name)
                    gatt = device.connectGatt(appContext, false, gattCallback)
                }

                override fun onScanFailed(errorCode: Int) {
                    scanning = false
                    state = state.copy(
                        connecting = false,
                        error = "No se encontró el botón SOS (error $errorCode). Acércalo y reintenta.",
                    )
                }
            }

            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(SOS_SERVICE_UUID))
                .build()
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            fun startScan() {
                if (scanning || gatt != null) return
                scanning = true
                state = state.copy(connecting = true, error = null)
                runCatching { scanner.startScan(listOf(filter), settings, scanCallback) }
                    .onFailure { scanning = false }
            }

            // Reintenta el escaneo cada 5 s hasta encontrar la placa: el advertising
            // BLE es intermitente y un único intento puede perderlo.
            scope.launch {
                while (true) {
                    if (gatt == null) startScan()
                    kotlinx.coroutines.delay(5_000)
                }
            }

            onDispose {
                runCatching { scanner.stopScan(scanCallback) }
                gatt?.disconnect()
                gatt?.close()
                client.close()
                scope.cancel()
            }
            }
        }
    }

    return state
}
