package com.gowayki.nesh.app.features.map.data

/**
 * Guarda la última ruta de combi seleccionada para que siga elegida
 * aunque se cierre la app o se salga de la vista del mapa.
 */
expect object RouteSelectionStore {
    suspend fun save(
        routeId: String,
        routeName: String,
        idaRouteId: String?,
        returnRouteId: String?,
        direction: String
    )
    suspend fun load(): String?
    /** Borra la ruta persistida (logout/cambio de cuenta). */
    suspend fun clear()
}
