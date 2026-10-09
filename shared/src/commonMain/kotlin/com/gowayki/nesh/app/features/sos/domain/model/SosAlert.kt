package com.gowayki.nesh.app.features.sos.domain.model

import com.gowayki.nesh.app.features.reports.domain.model.OsmLatLng
import kotlinx.serialization.Serializable

@Serializable
data class SosAlert(
    val id: String,
    val eventId: String,
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val occurredAt: String,
    val status: String = "ACTIVE",
    val simulated: Boolean = false,
) {
    val position: OsmLatLng
        get() = OsmLatLng(latitude, longitude)
}
