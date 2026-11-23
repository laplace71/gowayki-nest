package com.gowayki.nesh.app.features.map.data

import androidx.compose.ui.graphics.ImageBitmap
import com.gowayki.nesh.core.network.HttpClientFactory
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * Fotos de las rutas. Los bytes viven en la base de datos y el backend los
 * sirve por routeId; aquí solo se descargan, se decodifican y se cachean.
 */
object RouteImageLoader {
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, ImageBitmap?>()

    suspend fun load(routeId: String?): ImageBitmap? {
        if (routeId.isNullOrBlank()) return null
        cache[routeId]?.let { return it }
        return mutex.withLock {
            cache[routeId]?.let { return@withLock it }
            val bytes = runCatching {
                val response = HttpClientFactory.client
                    .get("${HttpClientFactory.baseUrl}/rutas/$routeId/imagen")
                if (response.status.isSuccess()) response.bodyAsBytes() else null
            }.getOrNull()
            val bitmap = bytes?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() }
            cache[routeId] = bitmap
            bitmap
        }
    }
}
