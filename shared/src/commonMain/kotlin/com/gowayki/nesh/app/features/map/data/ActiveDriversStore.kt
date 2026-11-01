package com.gowayki.nesh.app.features.map.data

import com.gowayki.nesh.app.features.map.domain.model.UserLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Registro en memoria de los conductores que están usando la app en una ruta.
 *
 * Hoy es local (mismo proceso). La arquitectura está pensada para que, cuando
 * se conecte un backend (Supabase Realtime), este store se reemplace por una
 * fuente remota sin tocar la UI del mapa: la pantalla solo observa [drivers].
 */
data class ActiveDriver(
    val id: String,
    val routeId: String,
    val lat: Double,
    val lon: Double,
    val isSelf: Boolean = false,
)

object ActiveDriversStore {
    private val _drivers = MutableStateFlow<List<ActiveDriver>>(emptyList())
    val drivers: StateFlow<List<ActiveDriver>> = _drivers.asStateFlow()

    /** Registra/actualiza la posición del conductor actual. */
    fun updateSelf(routeId: String, location: UserLocation) {
        val self = ActiveDriver(
            id = "self",
            routeId = routeId,
            lat = location.lat,
            lon = location.lon,
            isSelf = true,
        )
        _drivers.value = _drivers.value.filterNot { it.isSelf } + self
    }

    /** Elimina al conductor actual (al salir de la ruta). */
    fun clearSelf() {
        _drivers.value = _drivers.value.filterNot { it.isSelf }
    }

    /** Reemplaza la lista completa (útil al sincronizar con backend). */
    fun setAll(list: List<ActiveDriver>) {
        _drivers.value = list
    }
}