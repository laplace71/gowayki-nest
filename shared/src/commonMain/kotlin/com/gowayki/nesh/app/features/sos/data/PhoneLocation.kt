package com.gowayki.nesh.app.features.sos.data

import androidx.compose.runtime.Composable
import com.gowayki.nesh.app.features.reports.domain.model.OsmLatLng

data class PhoneLocationState(
    val location: OsmLatLng? = null,
    val permissionGranted: Boolean = false,
    val error: String? = null,
    val capturedAtEpochMs: Long? = null,
    val accuracyMeters: Float? = null,
)

@Composable
expect fun rememberPhoneLocation(enabled: Boolean): PhoneLocationState
