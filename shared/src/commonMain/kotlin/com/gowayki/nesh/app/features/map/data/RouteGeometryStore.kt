package com.gowayki.nesh.app.features.map.data

import com.gowayki.nesh.data.repository.BackendLineasApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.ExperimentalResourceApi
import gowaykinesh.shared.generated.resources.Res

/**
 * Geometría de ruta. Fuente primaria: API/BD (polilínea desde puntos de control)
 * para no sobrecargar la app; fallback a asset local de trazados.
 */
object RouteGeometryStore {
    private var assetCache: Map<String, List<Pair<Double, Double>>>? = null
    private val assetMutex = Mutex()
    private val apiCache = mutableMapOf<String, List<Pair<Double, Double>>>()
    private val apiMutex = Mutex()

    suspend fun geometryFor(routeId: String): List<Pair<Double, Double>>? {
        geometryFromApi(routeId)?.let { return it }
        return loadAsset()[routeId]
    }

    private suspend fun geometryFromApi(routeId: String): List<Pair<Double, Double>>? {
        apiMutex.withLock { apiCache[routeId]?.let { return it } }
        val linea = runCatching { BackendLineasApi.lineaBySitId(routeId) }.getOrNull() ?: return null
        val pts = linea.paraderos?.takeIf { it.isNotEmpty() } ?: runCatching { BackendLineasApi.puntos(linea.id) }.getOrDefault(emptyList())
        val geom = pts.mapNotNull { p ->
            val lat = p.latitud ?: return@mapNotNull null
            val lon = p.longitud ?: return@mapNotNull null
            Pair(lat, lon)
        }
        if (geom.size < 2) return null
        apiMutex.withLock { apiCache[routeId] = geom }
        return geom
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadAsset(): Map<String, List<Pair<Double, Double>>> {
        assetCache?.let { return it }
        return assetMutex.withLock {
            assetCache ?: withContext(Dispatchers.Default) {
                runCatching {
                    val bytes = Res.readBytes("files/route_geometry.json")
                    val root = Json.parseToJsonElement(bytes.decodeToString()).jsonObject
                    root.mapValues { (_, value) ->
                        value.jsonArray.map { point ->
                            val coords = point.jsonArray
                            Pair(coords[0].jsonPrimitive.double, coords[1].jsonPrimitive.double)
                        }
                    }
                }.getOrDefault(emptyMap())
            }.also { assetCache = it }
        }
    }
}
