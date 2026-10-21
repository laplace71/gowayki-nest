package com.gowayki.nesh.app.features.map.data

/**
 * Guarda la última ruta de combi seleccionada para que siga elegida
 * aunque se cierre la app o se salga de la vista del mapa.
 */
expect object RouteSelectionStore {
    suspend fun save(routeId: String)
    suspend fun load(): String?
}
