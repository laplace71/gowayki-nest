package com.gowayki.nesh.app.features.sos.data

import androidx.compose.runtime.Composable

data class SosBleButtonState(
    val supported: Boolean = true,
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val deviceName: String? = null,
    val error: String? = null,
    val lastForwardedEventId: String? = null,
)

/**
 * Cliente BLE del botón físico SOS. En Android escanea el periférico ESP32,
 * recibe las alertas por notificación y las reenvía al backend por HTTP.
 * No usa tokens: el backend solo acepta el reenvío desde la red privada.
 */
@Composable
expect fun rememberSosBleButton(
    enabled: Boolean,
    onAlertForwarded: () -> Unit = {},
): SosBleButtonState
