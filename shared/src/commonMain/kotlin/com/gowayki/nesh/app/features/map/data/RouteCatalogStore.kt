package com.gowayki.nesh.app.features.map.data

import androidx.compose.ui.graphics.Color
import com.gowayki.nesh.app.features.map.domain.model.ArequipaRoutes
import com.gowayki.nesh.app.features.map.domain.model.CombiRoute
import com.gowayki.nesh.data.entities.backend.BackendSubrutaDto
import com.gowayki.nesh.data.repository.BackendLineasApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Catálogo de rutas/subrutas.
 *
 * Fuente primaria: BD/backend (/api/rutas/catalogo). Respaldo: catálogo
 * empaquetado en la app ([ArequipaRoutes]) para funcionar offline o si la API
 * no está disponible. La unidad operativa es la subruta concreta (no la línea
 * general C2/C3...).
 */
object RouteCatalogStore {
    private val mutex = Mutex()
    private var loaded = false
    private var catalogSubrutas: List<CombiRoute> = emptyList()
    private var nombreToCodigo: Map<String, String> = emptyMap()

    /** Carga/actualiza el catálogo desde BD. Idempotente por llamada concurrente. */
    suspend fun refresh(force: Boolean = false): List<CombiRoute> = mutex.withLock {
        if (loaded && !force && catalogSubrutas.isNotEmpty()) return catalogSubrutas

        // Respaldo empaquetado primero (instantáneo) para que la UI no se congele.
        if (!loaded) {
            catalogSubrutas = ArequipaRoutes.all.filter { it.isSub && it.routeId != null }
            loaded = true
        }

        val remote = runCatching { BackendLineasApi.catalogoRutas() }.getOrNull()
        val lineas = remote?.lineas.orEmpty()
        nombreToCodigo = lineas.mapNotNull { linea ->
            val nombre = linea.nombre?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val codigo = linea.codigo?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            nombre.lowercase() to codigo.uppercase()
        }.toMap()
        val mapped = lineas
            .flatMap { it.subrutas.orEmpty() }
            .mapNotNull { it.toCombiRoute() }

        if (mapped.isNotEmpty()) {
            // El backend puede repetir subrutas (misma fila en dos líneas):
            // los ids son keys de LazyColumn y un duplicado cierra la app.
            catalogSubrutas = mapped.distinctBy { it.id }
            loaded = true
        }
        return catalogSubrutas
    }

    /** Subrutas disponibles (desde BD si cargó, si no respaldo empaquetado). */
    suspend fun subrutas(): List<CombiRoute> {
        if (!loaded || catalogSubrutas.isEmpty()) refresh()
        return catalogSubrutas.ifEmpty { ArequipaRoutes.all.filter { it.isSub && it.routeId != null } }
    }

    /** Subrutas de una línea (código padre, ej. "C2"). */
    suspend fun subrutasForLine(lineCode: String): List<CombiRoute> =
        subrutas().filter { it.parent.equals(lineCode, ignoreCase = true) }

    suspend fun byId(id: String): CombiRoute? {
        val subs = subrutas()
        return subs.firstOrNull { it.id == id }
            ?: ArequipaRoutes.byId(id)
    }

    suspend fun byRouteId(routeId: String): CombiRoute? {
        val subs = subrutas()
        return subs.firstOrNull { it.routeId == routeId || it.returnRouteId == routeId }
            ?: ArequipaRoutes.all.firstOrNull { it.routeId == routeId || it.returnRouteId == routeId }
    }

    /** Código C2/C3… a partir del nombre real de la línea del backend. */
    fun codigoParaNombre(nombre: String?): String? =
        nombre?.trim()?.lowercase()?.let { nombreToCodigo[it] }

    /** Resolución síncrona desde lo ya cargado (o el respaldo empaquetado), para etiquetas de UI. */
    fun cachedById(id: String): CombiRoute? =
        catalogSubrutas.firstOrNull { it.id.equals(id, ignoreCase = true) }
            ?: ArequipaRoutes.byId(id)

    /** Todas las rutas conocidas (subrutas de catálogo + rutas empaquetadas como respaldo). */
    suspend fun all(): List<CombiRoute> {
        val subs = subrutas()
        return if (subs.isNotEmpty()) subs else ArequipaRoutes.all
    }

    /**
     * Código de línea (C2, C3…). Si el backend no manda `lineaCodigo`, hay que
     * sacarlo del NOMBRE ("Línea C2") o del prefijo del código ("SB-C2-1"),
     * nunca de los primeros caracteres de un id numérico.
     */
    private fun parentCode(lineaCodigo: String?, lineaNombre: String?, codigo: String?): String? {
        lineaCodigo?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        val desdeNombre = lineaNombre?.trim()
            ?.replace(Regex("(?i)^l[ií]nea\\s+"), "")
            ?.split(Regex("\\s+"))?.firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() && it.any { c -> c.isLetter() } }
        if (desdeNombre != null) return desdeNombre
        val desdeCodigo = codigo?.trim()
            ?.removePrefix("SB-")?.removePrefix("sb-")
            ?.substringBefore("-")
            ?.takeIf { it.isNotBlank() && it.any { c -> c.isLetter() } }
        return desdeCodigo
    }

    private fun BackendSubrutaDto.toCombiRoute(): CombiRoute? {
        val code = codigo ?: return null
        val ida = routeIdIda ?: return null
        val parentCode = parentCode(lineaCodigo, lineaNombre, code).orEmpty().uppercase()
        return CombiRoute(
            id = code,
            name = nombre ?: code,
            operator = operador ?: (lineaNombre ?: ""),
            color = parseColor(colorHex),
            waypoints = emptyList(),
            isSub = true,
            parent = parentCode.ifBlank { null },
            parentName = lineaNombre?.takeIf { it.isNotBlank() },
            routeId = ida,
            returnRouteId = routeIdRetorno,
            returnName = nombreRetorno,
            subrutaDbId = id,
        )
    }

    private fun parseColor(hex: String?): Color {
        val clean = hex?.trim()?.removePrefix("#") ?: return Color(0xFF888888)
        return runCatching {
            when (clean.length) {
                6 -> Color(0xFF000000 or clean.toLong(16))
                8 -> Color(clean.toLong(16))
                else -> Color(0xFF888888)
            }
        }.getOrDefault(Color(0xFF888888))
    }
}
