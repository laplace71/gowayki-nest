package com.gowayki.nesh.data.repository

import com.gowayki.nesh.core.network.HttpClientFactory
import com.gowayki.nesh.data.entities.backend.BackendCatalogoRutasDto
import com.gowayki.nesh.data.entities.backend.BackendLineaDto
import com.gowayki.nesh.data.entities.backend.BackendParaderoRutaDto
import com.gowayki.nesh.data.entities.backend.BackendPuntoDto
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Fuente API (VPS/BD) para lineas y puntos de control.
 * Cachea la lista para no sobrecargar red/app; se usa como fuente primaria
 * del módulo de rutas/mapa con fallback a assets locales.
 *
 * La red siempre se ejecuta en IO: estas funciones se llaman desde
 * `LaunchedEffect` (Main) y al cancelar, Ktor cierra el stream en ese hilo.
 */
object BackendLineasApi {
    private val client = HttpClientFactory.client
    private val baseUrl get() = HttpClientFactory.baseUrl
    private val mutex = Mutex()
    private var cachedLineas: List<BackendLineaDto>? = null
    // indice: SIT id numérico (extraído de "SIT 17659544 ...") -> linea
    private var bySitId: Map<String, BackendLineaDto>? = null

    suspend fun lineas(force: Boolean = false): List<BackendLineaDto> = mutex.withLock {
        if (!force) cachedLineas?.let { return it }
        val list = withContext(Dispatchers.Default) {
            runCatching { client.get("$baseUrl/lineas").body<List<BackendLineaDto>>() }
                .getOrElse { emptyList() }
        }
        cachedLineas = list
        bySitId = buildIndex(list)
        list
    }

    suspend fun lineaBySitId(sitId: String): BackendLineaDto? {
        if (bySitId == null) lineas()
        return mutex.withLock { bySitId?.get(sitId) }
    }

    suspend fun puntos(lineaId: Int): List<BackendPuntoDto> = withContext(Dispatchers.Default) {
        runCatching { client.get("$baseUrl/lineas/$lineaId/puntos-control").body<List<BackendPuntoDto>>() }
            .getOrElse { emptyList() }
    }

    /** Catálogo de líneas y subrutas desde BD. */
    suspend fun catalogoRutas(): BackendCatalogoRutasDto? = withContext(Dispatchers.Default) {
        runCatching { client.get("$baseUrl/rutas/catalogo").body<BackendCatalogoRutasDto>() }.getOrNull()
    }

    /** Paraderos por routeId (ida o retorno) desde BD. */
    suspend fun paraderosPorRouteId(routeId: String): List<BackendParaderoRutaDto> =
        withContext(Dispatchers.Default) {
            runCatching { client.get("$baseUrl/rutas/$routeId/paraderos").body<List<BackendParaderoRutaDto>>() }
                .getOrElse { emptyList() }
        }

    /** Geometría por routeId (ida o retorno) desde BD: lista de [lat,lon]. */
    suspend fun geometriaPorRouteId(routeId: String): List<List<Double>> =
        withContext(Dispatchers.Default) {
            runCatching { client.get("$baseUrl/rutas/$routeId/geometria").body<List<List<Double>>>() }
                .getOrElse { emptyList() }
        }

    private fun buildIndex(list: List<BackendLineaDto>): Map<String, BackendLineaDto> {
        val map = mutableMapOf<String, BackendLineaDto>()
        val re = Regex("""^SIT\s+(\d+)""")
        for (l in list) {
            val name = l.nombre ?: continue
            val m = re.find(name) ?: continue
            if (m.groupValues[1] !in map) map[m.groupValues[1]] = l
        }
        return map
    }

    fun invalidate() { cachedLineas = null; bySitId = null }
}
