package com.gowayki.nesh.app.common.util

import android.content.Context
import android.location.LocationManager
import com.gowayki.nesh.app.features.sos.domain.model.AndroidSosContext

actual fun lastKnownLocation(): Pair<Double, Double>? {
    val ctx = AndroidSosContext.appContext ?: return null
    val manager = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val gps = runCatching { manager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
    val net = runCatching { manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
    val loc = gps ?: net ?: return null
    return loc.latitude to loc.longitude
}