package com.gowayki.nesh.app.features.map.data

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

/** Paraderos ordenados por ruta, con distritos precalculados para uso sin red. */
object RouteStopsStore {
    private var cache: Map<String, List<SitRouteStop>>? = null
    private val mutex = Mutex()

    suspend fun stopsFor(routeId: String): List<SitRouteStop> {
        val stops = load()[routeId].orEmpty()
        val first = stops.firstOrNull() ?: return emptyList()
        val initial = if (first.name.isBlank() || first.name.equals("Innominada", ignoreCase = true)) {
            first.copy(name = "Inicio de ruta")
        } else {
            first
        }
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
    private suspend fun load(): Map<String, List<SitRouteStop>> {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: withContext(Dispatchers.Default) {
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
            }.also { cache = it }
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
