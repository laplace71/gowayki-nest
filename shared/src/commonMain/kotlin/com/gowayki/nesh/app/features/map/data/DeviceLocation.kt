package com.gowayki.nesh.app.features.map.data

import com.gowayki.nesh.app.features.map.domain.model.UserLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Fuente de ubicación continua del teléfono.
 *
 * Se usa solo durante el modo navegación: emite fixes reales del GPS
 * mientras haya un collector y deja de escuchar al cancelarse.
 */
interface DeviceLocationSource {
    fun locations(): Flow<UserLocation>
}

/** Fallback para previews/tests o plataformas sin GPS. */
class NoopDeviceLocationSource : DeviceLocationSource {
    override fun locations(): Flow<UserLocation> = emptyFlow()
}

expect fun createDeviceLocationSource(): DeviceLocationSource
