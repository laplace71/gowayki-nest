package com.gowayki.nesh.app.features.map.presentation

import com.gowayki.nesh.app.features.map.data.RouteCatalogStore

/**
 * Etiqueta legible de la subruta concreta de un chat (`claveRuta` = SB-…).
 * Se usa en las pantallas de comunicación para decir dónde está cada conductor.
 */
object SubrouteLabels {
    fun nameOf(claveRuta: String?): String? {
        val key = claveRuta?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (key.equals("GRUPO_RUTA", ignoreCase = true)) return null
        if (key.startsWith("GRUPO ", ignoreCase = true)) return null
        // Las líneas (C2, C11, "Línea C3") no son subrutas y no deben
        // etiquetarse como tal aunque estén en el catálogo.
        if (key.matches(Regex("""(?i)^(?:l[ií]nea\s+|canal\s+de\s+(?:l[ií]nea\s+)?)?c\d+$"""))) return null

        RouteCatalogStore.cachedById(key)?.name?.takeIf { it.isNotBlank() }?.let { return it }

        // Solo mostramos la clave cruda si parece un id de subruta (SB-…).
        // Así una clave corrupta no aparece como "Subruta: GRUPO_RUTA".
        return if (key.startsWith("SB-", ignoreCase = true) || key.startsWith("SUB-", ignoreCase = true)) {
            key
        } else {
            null
        }
    }
}
