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

data class SitRouteStop(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val district: String?,
)

/** Paraderos por ruta. Fuente primaria: API/BD (VPS) para no sobrecargar la app; fallback a asset local. */
object RouteStopsStore {
    private var assetCache: Map<String, List<SitRouteStop>>? = null
    private val assetMutex = Mutex()
    private val apiCache = mutableMapOf<String, List<SitRouteStop>>()
    private val apiMutex = Mutex()

    suspend fun stopsFor(routeId: String): List<SitRouteStop> {
        val raw = stopsFromApi(routeId) ?: loadAsset()[routeId].orEmpty()
        return postProcess(raw)
    }

    private suspend fun stopsFromApi(routeId: String): List<SitRouteStop>? {
        apiMutex.withLock { apiCache[routeId]?.let { return it } }
        val linea = runCatching { BackendLineasApi.lineaBySitId(routeId) }.getOrNull() ?: return null
        val pts = linea.paraderos?.takeIf { it.isNotEmpty() } ?: runCatching { BackendLineasApi.puntos(linea.id) }.getOrDefault(emptyList())
        if (pts.isEmpty()) return null
        val stops = pts.mapIndexedNotNull { i, p ->
            val lat = p.latitud ?: return@mapIndexedNotNull null
            val lon = p.longitud ?: return@mapIndexedNotNull null
            SitRouteStop(
                id = (p.id ?: i).toString(),
                name = p.nombre ?: "Paradero ${p.orden ?: (i + 1)}",
                latitude = lat,
                longitude = lon,
                district = null,
            )
        }
        if (stops.isEmpty()) return null
        apiMutex.withLock { apiCache[routeId] = stops }
        return stops
    }

    private fun postProcess(stops: List<SitRouteStop>): List<SitRouteStop> {
        val first = stops.firstOrNull() ?: return emptyList()
        val initial = if (first.name.isBlank() || first.name.equals("Innominada", ignoreCase = true)) {
            first.copy(name = "Inicio de ruta")
        } else first
        val initialKey = initial.stopKey()
        val remainder = stops.asSequence()
            .drop(1)
            .filterNot { it.name.isBlank() || it.name.equals("Innominada", ignoreCase = true) }
            .distinctBy { it.stopKey() }
            .filterNot { it.stopKey() == initialKey }
            .toList()
        return listOf(initial) + remainder
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadAsset(): Map<String, List<SitRouteStop>> {
        assetCache?.let { return it }
        return assetMutex.withLock {
            assetCache ?: withContext(Dispatchers.Default) {
                runCatching {
                    val bytes = Res.readBytes("files/sit_route_stops.json")
                    val root = Json.parseToJsonElement(bytes.decodeToString()).jsonObject
                    root.mapValues { (_, routeStops) ->
                        routeStops.jsonArray.map { value ->
                            val stop = value.jsonObject
                            SitRouteStop(
                                id = stop.getValue("id").jsonPrimitive.content,
                                name = stop.getValue("name").jsonPrimitive.content,
                                latitude = stop.getValue("lat").jsonPrimitive.double,
                                longitude = stop.getValue("lon").jsonPrimitive.double,
                                district = stop["district"]?.jsonPrimitive?.content,
                            )
                        }
                    }
                }.getOrDefault(emptyMap())
            }.also { assetCache = it }
        }
    }

    private fun SitRouteStop.stopKey(): String =
        "${name.normalized()}|${district.orEmpty().normalized()}"

    private fun String.normalized(): String = lowercase()
        .replace("av.", "avenida")
        .replace(Regex("\\bav\\b"), "avenida")
        .replace("jr.", "jirón")
        .replace(Regex("\\s+"), " ")
        .trim()
}
