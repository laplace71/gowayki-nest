package com.gowayki.nesh.app.features.sos.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberSosBleButton(
    enabled: Boolean,
    onAlertForwarded: () -> Unit,
): SosBleButtonState =
    remember(enabled) {
        SosBleButtonState(
            supported = false,
            error = if (enabled) {
                "La conexión BLE con el botón está implementada solo para Android."
            } else {
                null
            },
        )
    }
