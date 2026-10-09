package com.gowayki.nesh.app.features.sos.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPhoneLocation(enabled: Boolean): PhoneLocationState =
    remember(enabled) {
        PhoneLocationState(
            error = if (enabled) {
                "La integración GPS del teléfono está implementada solo para Android."
            } else {
                null
            },
        )
    }
