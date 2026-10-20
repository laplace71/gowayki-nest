// Actual Android — GPS real con LocationManager (sin Play Services, sin cambios gradle).
@file:Suppress("SpellCheckingInspection", "MissingPermission")
package com.gowayki.nesh.app.features.sos.domain.model

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private class AndroidGpsProvider(
    private val appContext: Context,
) : GpsProvider {

    private fun manager(): LocationManager? =
        appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    override suspend fun lastFix(): GpsFix? {
        val m = manager() ?: return null
        return try {
            val gps = try { m.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: SecurityException) { null }
            val net = try { m.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: SecurityException) { null }
            best(gps, net)?.toFix()
        } catch (_: Exception) { null }
    }

    override suspend fun currentFix(): GpsFix? {
        // Punto EXACTO donde estás: pide fix fresco del satélite (hasta 12 s)
        // y se queda con el más preciso entre ese y el último conocido.
        val m = manager() ?: return null
        if (!m.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            !m.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        ) return null
        val fresh = withTimeoutOrNull(12_000) { singleUpdate(m) }
        val last = lastKnown(m)
        return mostAccurate(fresh, last)?.toFix()
    }

    private suspend fun singleUpdate(m: LocationManager): Location? =
        suspendCancellableCoroutine { cont ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    try { m.removeUpdates(this) } catch (_: Exception) {}
                    if (cont.isActive) cont.resume(location)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            try {
                val looper = Looper.getMainLooper()
                try {
                    m.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, looper)
                } catch (_: Exception) {
                    m.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, looper)
                }
                cont.invokeOnCancellation { try { m.removeUpdates(listener) } catch (_: Exception) {} }
            } catch (_: SecurityException) {
                if (cont.isActive) cont.resume(null)
            } catch (_: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }

    private fun lastKnown(m: LocationManager): Location? {
        return try {
            val gps = try { m.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: SecurityException) { null }
            val net = try { m.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: SecurityException) { null }
            best(gps, net)
        } catch (_: Exception) { null }
    }

    // Elige el fix con menor error estimado (más exacto). Sin dato de
    // precisión, prefiere el fresco del satélite sobre la caché.
    private fun mostAccurate(a: Location?, b: Location?): Location? {
        if (a == null) return b
        if (b == null) return a
        val aa = if (a.hasAccuracy()) a.accuracy else Float.MAX_VALUE
        val bb = if (b.hasAccuracy()) b.accuracy else Float.MAX_VALUE
        if (aa != bb) return if (aa < bb) a else b
        return if (a.time >= b.time) a else b
    }

    private fun best(a: Location?, b: Location?): Location? {
        if (a == null) return b
        if (b == null) return a
        // Prioriza la más reciente; GPS sobre red si empatan.
        return if (b.time > a.time + 30_000) b else a
    }

    private fun Location.toFix() = GpsFix(latitude, longitude, if (hasAccuracy()) accuracy else null)
}

actual fun createGpsProvider(): GpsProvider {
    val ctx = AndroidSosContext.appContext ?: return NoopGpsProvider()
    return AndroidGpsProvider(ctx)
}
