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

/**
 * Trazados reales de las rutas de combi (C2..C11) empaquetados como asset.
 *
 * El JSON tiene la forma `{ "routeId": [[lat, lon], ...], ... }` y se carga
 * una sola vez (lazy) para no bloquear el arranque de la app.
 */
object RouteGeometryStore {

    private var cache: Map<String, List<Pair<Double, Double>>>? = null
    private val mutex = Mutex()

    /** Devuelve el trazado real de la ruta, o null si no está disponible. */
    suspend fun geometryFor(routeId: String): List<Pair<Double, Double>>? = load()[routeId]

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun load(): Map<String, List<Pair<Double, Double>>> {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: run {
                val parsed = withContext(Dispatchers.Default) {
                    val bytes = Res.readBytes("files/route_geometry.json")
                    val root = Json.parseToJsonElement(bytes.decodeToString()).jsonObject
                    root.mapValues { (_, value) ->
                        value.jsonArray.map { point ->
                            val coords = point.jsonArray
                            Pair(coords[0].jsonPrimitive.double, coords[1].jsonPrimitive.double)
                        }
                    }
                }
                cache = parsed
                parsed
            }
        }
    }
}
