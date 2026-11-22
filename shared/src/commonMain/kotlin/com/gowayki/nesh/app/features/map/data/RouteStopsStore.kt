package com.gowayki.nesh.app.features.map.data

import com.gowayki.nesh.data.repository.BackendLineasApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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

/**
 * Paraderos por ruta.
 *
 * Fuente primaria: BD/backend por routeId (subruta ida/retorno), con caché en memoria.
 * Respaldo: asset local empaquetado (datos oficiales) y API antigua de lineas.
 */
object RouteStopsStore {
    private var assetJson: JsonObject? = null
    private val assetMutex = Mutex()
    private val localCache = mutableMapOf<String, List<SitRouteStop>>()
    private val apiCache = mutableMapOf<String, List<SitRouteStop>>()
    private val apiMutex = Mutex()
    private val refreshScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _actualizaciones = MutableSharedFlow<String>(extraBufferCapacity = 16)

    /** Aviso de que una ruta tiene datos nuevos viniendo del backend. */
    fun actualizaciones(): Flow<String> = _actualizaciones

    /** Paraderos de una ruta: asset local (instantáneo) → BD/API nueva → API antigua (respaldo). */
    suspend fun stopsFor(routeId: String): List<SitRouteStop> {
        apiMutex.withLock { apiCache[routeId] }?.let { return postProcess(it) }

        // Primero el asset local (0 ms) para que la UI responda al instante.
        val local = localRoute(routeId)
        if (local.isNotEmpty()) {
            // El respaldo empaquetado se muestra ya, pero SÍ se lanza la
            // actualización contra la BD para que la app no se quede con datos
            // viejos para siempre.
            refrescarEnSegundoPlano(routeId)
            return postProcess(local)
        }

        val remoteNew = stopsFromNewApi(routeId)
        if (remoteNew.isNotEmpty()) {
            val processed = postProcess(remoteNew)
            apiMutex.withLock { apiCache[routeId] = processed }
            return processed
        }

        val remote = stopsFromApi(routeId).orEmpty()
        val processed = postProcess(remote)
        if (processed.isNotEmpty()) apiMutex.withLock { apiCache[routeId] = processed }
        return processed
    }

    private fun refrescarEnSegundoPlano(routeId: String) {
        refreshScope.launch {
            val remoto = stopsFromNewApi(routeId)
            if (remoto.isEmpty()) return@launch
            val procesado = postProcess(remoto)
            apiMutex.withLock { apiCache[routeId] = procesado }
            _actualizaciones.tryEmit(routeId)
        }
    }

    private suspend fun stopsFromNewApi(routeId: String): List<SitRouteStop> {
        val dtos = runCatching { BackendLineasApi.paraderosPorRouteId(routeId) }.getOrElse { emptyList() }
        if (dtos.isEmpty()) return emptyList()
        return dtos.mapIndexedNotNull { i, p ->
            val lat = p.lat ?: return@mapIndexedNotNull null
            val lon = p.lon ?: return@mapIndexedNotNull null
            SitRouteStop(
                id = p.id ?: i.toString(),
                name = p.name ?: "Paradero ${p.orden ?: (i + 1)}",
                latitude = lat,
                longitude = lon,
                district = p.district,
            )
        }
    }

    /**
     * Solo asset local, sin red. Se usa para elegir una ruta de respaldo sin
     * disparar una petición HTTP por cada sub-ruta (eso congelaba la pantalla).
     */
    suspend fun localStops(routeId: String): List<SitRouteStop> = localRoute(routeId)

    private suspend fun localRoute(routeId: String): List<SitRouteStop> {
        localCache[routeId]?.let { return it }
        val root = loadAssetJson()
        val array = root[routeId]?.jsonArray ?: return emptyList()
        val stops = array.mapNotNull { value ->
            runCatching {
                val stop = value.jsonObject
                SitRouteStop(
                    id = stop.getValue("id").jsonPrimitive.content,
                    name = stop.getValue("name").jsonPrimitive.content,
                    latitude = stop.getValue("lat").jsonPrimitive.double,
                    longitude = stop.getValue("lon").jsonPrimitive.double,
                    district = stop["district"]?.jsonPrimitive?.content,
                )
            }.getOrNull()
        }
        val processed = postProcess(stops)
        localCache[routeId] = processed
        return processed
    }

    private suspend fun stopsFromApi(routeId: String): List<SitRouteStop>? {
        apiMutex.withLock { apiCache[routeId]?.let { return it } }
        val linea = runCatching { BackendLineasApi.lineaBySitId(routeId) }.getOrNull() ?: return null
        // La API antigua /api/lineas ya no incluye paraderos: se piden por línea
        // a /api/lineas/{id}/puntos-control. La fuente canónica sigue siendo
        // /api/rutas/{routeId}/paraderos (stopsFromNewApi).
        val pts = runCatching { BackendLineasApi.puntos(linea.id) }.getOrDefault(emptyList())
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

    private suspend fun postProcess(stops: List<SitRouteStop>): List<SitRouteStop> {
        // Nombres genéricos ("Paradero 2") → descripción real del lugar.
        val conDescripcion = stops.map { stop ->
            val descripcion = StopDescriptions.describe(stop.latitude, stop.longitude, "")
            val nombre = when {
                descripcion.isBlank() -> stop.name
                stop.name.isBlank() || stop.name.equals("Innominada", ignoreCase = true) ||
                    stop.name.startsWith("Paradero", ignoreCase = true) -> descripcion
                stop.name.contains(descripcion, ignoreCase = true) -> stop.name
                else -> "${stop.name} · $descripcion"
            }
            stop.copy(name = nombre)
        }
        val first = conDescripcion.firstOrNull() ?: return emptyList()
        val initial = if (first.name.isBlank() || first.name.equals("Innominada", ignoreCase = true)) {
            first.copy(name = "Inicio de ruta")
        } else first
        val initialKey = initial.stopKey()
        val remainder = conDescripcion.asSequence()
            .drop(1)
            .filterNot { it.name.isBlank() || it.name.equals("Innominada", ignoreCase = true) }
            .distinctBy { it.stopKey() }
            .filterNot { it.stopKey() == initialKey }
            .toList()
        return listOf(initial) + remainder
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadAssetJson(): JsonObject {
        assetJson?.let { return it }
        return assetMutex.withLock {
            assetJson ?: withContext(Dispatchers.Default) {
                runCatching {
                    val bytes = Res.readBytes("files/sit_route_stops.json")
                    Json.parseToJsonElement(bytes.decodeToString()).jsonObject
                }.getOrDefault(JsonObject(emptyMap()))
            }.also { assetJson = it }
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
