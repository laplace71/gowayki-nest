package com.gowayki.nesh.app.features.map.presentation

import com.gowayki.nesh.app.features.map.data.RouteCatalogStore

/**
 * Etiqueta legible de la subruta concreta de un chat (`claveRuta` = SB-…).
 * Se usa en las pantallas de comunicación para decir dónde está cada conductor.
 */
object SubrouteLabels {
    fun nameOf(claveRuta: String?): String? {
        val key = claveRuta?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return RouteCatalogStore.cachedById(key)?.name?.takeIf { it.isNotBlank() } ?: key
    }
}
