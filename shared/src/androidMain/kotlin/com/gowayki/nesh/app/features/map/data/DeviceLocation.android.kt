@file:Suppress("MissingPermission")
package com.gowayki.nesh.app.features.map.data

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.gowayki.nesh.app.features.map.domain.model.UserLocation
import com.gowayki.nesh.app.features.sos.domain.model.AndroidSosContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * Actual Android: ubicación continua con LocationManager (sin Play Services).
 * Se usa durante la navegación; emite como máximo ~1 fix por segundo.
 */
private class AndroidDeviceLocationSource(
    private val context: Context,
) : DeviceLocationSource {

    override fun locations(): Flow<UserLocation> = callbackFlow {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (manager == null) {
            close()
            return@callbackFlow
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.toUserLocation())
            }

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {}

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        // Último punto conocido: permite centrar el mapa al instante.
        runCatching {
            val gps = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val net = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            (gps ?: net)?.let { trySend(it.toUserLocation()) }
        }

        val providers = buildList {
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
        }

        providers.forEach { provider ->
            runCatching {
                manager.requestLocationUpdates(
                    provider,
                    1_000L,
                    1f,
                    listener,
                    Looper.getMainLooper(),
                )
            }
        }

        awaitClose {
            runCatching { manager.removeUpdates(listener) }
        }
    }.conflate()

    private fun Location.toUserLocation() = UserLocation(
        lat = latitude,
        lon = longitude,
        bearingDeg = if (hasBearing()) bearing else null,
        speedMps = if (hasSpeed()) speed else null,
        accuracyM = if (hasAccuracy()) accuracy else null,
    )
}

actual fun createDeviceLocationSource(): DeviceLocationSource {
    val ctx = AndroidSosContext.appContext ?: return NoopDeviceLocationSource()
    return AndroidDeviceLocationSource(ctx)
}
