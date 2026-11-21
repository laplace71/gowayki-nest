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

/**
 * Geometría de ruta.
 *
 * Fuente primaria: BD/backend por routeId (subruta ida/retorno), con caché.
 * Respaldo: asset local empaquetado y API antigua.
 */
object RouteGeometryStore {
    private var assetJson: JsonObject? = null
    private val assetMutex = Mutex()
    private val localCache = mutableMapOf<String, List<Pair<Double, Double>>>()
    private val apiCache = mutableMapOf<String, List<Pair<Double, Double>>>()
    private val apiMutex = Mutex()
    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _actualizaciones = MutableSharedFlow<String>(extraBufferCapacity = 16)

    /** Aviso de que una ruta tiene geometría nueva viniendo del backend. */
    fun actualizaciones(): Flow<String> = _actualizaciones

    suspend fun geometryFor(routeId: String): List<Pair<Double, Double>>? {
        apiMutex.withLock { apiCache[routeId] }?.let { return it }

        // Primero el asset local (0 ms) para que la UI responda al instante.
        val local = localRoute(routeId)
        if (!local.isNullOrEmpty()) {
            refrescarEnSegundoPlano(routeId)
            return local
        }

        val remoteNew = geometryFromNewApi(routeId)
        if (!remoteNew.isNullOrEmpty()) {
            apiMutex.withLock { apiCache[routeId] = remoteNew }
            return remoteNew
        }

        return geometryFromApi(routeId)
    }

    private fun refrescarEnSegundoPlano(routeId: String) {
        refreshScope.launch {
            val remoto = geometryFromNewApi(routeId)
            if (remoto.isNullOrEmpty()) return@launch
            apiMutex.withLock { apiCache[routeId] = remoto }
            _actualizaciones.tryEmit(routeId)
        }
    }

    private suspend fun geometryFromNewApi(routeId: String): List<Pair<Double, Double>>? {
        val pts = runCatching { BackendLineasApi.geometriaPorRouteId(routeId) }.getOrElse { emptyList() }
        if (pts.size < 2) return null
        val pairs = pts.mapNotNull { p ->
            if (p.size >= 2) Pair(p[0], p[1]) else null
        }
        return if (pairs.size >= 2) pairs else null
    }

    private suspend fun localRoute(routeId: String): List<Pair<Double, Double>>? {
        localCache[routeId]?.let { return it }
        val root = loadAssetJson()
        val array = root[routeId]?.jsonArray ?: return null
        val points = array.mapNotNull { value ->
            runCatching {
                val coords = value.jsonArray
                Pair(coords[0].jsonPrimitive.double, coords[1].jsonPrimitive.double)
            }.getOrNull()
        }
        if (points.isNotEmpty()) {
            localCache[routeId] = points
        }
        return points
    }

    private suspend fun geometryFromApi(routeId: String): List<Pair<Double, Double>>? {
        val linea = runCatching { BackendLineasApi.lineaBySitId(routeId) }.getOrNull() ?: return null
        // /api/lineas ya no trae paraderos (viven por subruta); el respaldo
        // legacy se pide por línea. La fuente principal es /api/rutas/{routeId}/geometria.
        val pts = runCatching { BackendLineasApi.puntos(linea.id) }.getOrDefault(emptyList())
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
    private suspend fun loadAssetJson(): JsonObject {
        assetJson?.let { return it }
        return assetMutex.withLock {
            assetJson ?: withContext(Dispatchers.Default) {
                runCatching {
                    val bytes = Res.readBytes("files/route_geometry.json")
                    Json.parseToJsonElement(bytes.decodeToString()).jsonObject
                }.getOrDefault(JsonObject(emptyMap()))
            }.also { assetJson = it }
        }
    }
}
