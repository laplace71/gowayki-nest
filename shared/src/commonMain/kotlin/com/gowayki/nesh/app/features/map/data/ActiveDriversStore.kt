package com.gowayki.nesh.app.features.map.data

import com.gowayki.nesh.app.features.map.domain.model.UserLocation
import com.gowayki.nesh.data.entities.backend.BackendUnidadDto
import com.gowayki.nesh.data.entities.backend.PosicionGpsDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveDriver(
    val id: String,
    val routeId: String,
    val lat: Double,
    val lon: Double,
    val bearingDeg: Float = 0f,
    val speedKmh: Double = 0.0,
    val numeroPadron: String? = null,
    val unidadId: Int? = null,
    val isSelf: Boolean = false,
)

object ActiveDriversStore {
    private val _drivers = MutableStateFlow<List<ActiveDriver>>(emptyList())
    val drivers: StateFlow<List<ActiveDriver>> = _drivers.asStateFlow()

    fun updateSelf(routeId: String, location: UserLocation) {
        val self = ActiveDriver(
            id = "self",
            routeId = routeId,
            lat = location.lat,
            lon = location.lon,
            bearingDeg = location.bearingDeg ?: 0f,
            speedKmh = ((location.speedMps ?: 0f) * 3.6).toDouble(),
            isSelf = true,
        )
        _drivers.value = _drivers.value.filterNot { it.isSelf } + self
    }

    fun updateRemoteDriver(dto: PosicionGpsDto, myUnidadId: Int?) {
        val uId = dto.unidadId ?: return
        if (uId == myUnidadId) return
        val driverId = "u-$uId"
        val padron = dto.numeroPadron ?: uId.toString()
        val routeCode = dto.lineaId?.let { "C$it" } ?: ""
        val driver = ActiveDriver(
            id = driverId,
            routeId = routeCode,
            lat = dto.latitud,
            lon = dto.longitud,
            bearingDeg = dto.rumboGrados?.toFloat() ?: 0f,
            speedKmh = dto.velocidadKmh ?: 0.0,
            numeroPadron = padron,
            unidadId = uId,
            isSelf = false,
        )
        _drivers.value = _drivers.value.filterNot { it.id == driverId } + driver
    }

    fun updateFromUnidades(unidades: List<BackendUnidadDto>, myUnidadId: Int?) {
        val remoteList = unidades.mapNotNull { u ->
            if (u.id == myUnidadId) return@mapNotNull null
            val pos = u.posicionActual ?: return@mapNotNull null
            val routeCode = u.lineaId?.let { "C$it" } ?: ""
            ActiveDriver(
                id = "u-${u.id}",
                routeId = routeCode,
                lat = pos.latitud,
                lon = pos.longitud,
                bearingDeg = pos.rumboGrados?.toFloat() ?: 0f,
                speedKmh = pos.velocidadKmh ?: 0.0,
                numeroPadron = u.numeroPadron ?: u.id.toString(),
                unidadId = u.id,
                isSelf = false,
            )
        }
        val self = _drivers.value.firstOrNull { it.isSelf }
        _drivers.value = if (self != null) listOf(self) + remoteList else remoteList
    }

    fun clearSelf() {
        _drivers.value = _drivers.value.filterNot { it.isSelf }
    }

    fun setAll(list: List<ActiveDriver>) {
        _drivers.value = list
    }
}