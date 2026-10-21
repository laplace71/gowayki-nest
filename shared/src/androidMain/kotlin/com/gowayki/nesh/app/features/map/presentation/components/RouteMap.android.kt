package com.gowayki.nesh.app.features.map.presentation.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.gson.JsonParser
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.gowayki.nesh.app.features.map.domain.model.UserLocation
import com.gowayki.nesh.app.features.sos.presentation.components.MapboxToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.cos
import kotlin.math.log2

private const val ROUTE_SRC = "route-src"
private const val ROUTE_LINE_CASING = "route-line-casing"
private const val ROUTE_LINE = "route-line"
private const val ME_SRC = "me-src"
private const val ME_HALO = "me-halo"
private const val ME_PIN = "me-pin"

@Composable
actual fun RouteMap(
    routePoints: List<Pair<Double, Double>>,
    userLocation: UserLocation?,
    navigating: Boolean,
    cameraBearingDeg: Double?,
    modifier: Modifier,
) {
    remember {
        if (MapboxToken.PUBLIC_TOKEN.startsWith("pk.")) {
            MapboxOptions.accessToken = MapboxToken.PUBLIC_TOKEN
        }
        true
    }

    var matched by remember(routePoints) { mutableStateOf(routePoints) }
    LaunchedEffect(routePoints) {
        // Los trazados reales de SmartBus ya vienen con cientos de puntos:
        // se dibujan tal cual. Solo se pide Directions para waypoints sueltos.
        if (routePoints.size < 2 || routePoints.size > 24) return@LaunchedEffect
        val result = fetchDirectionsRoute(routePoints)
        if (result != null && result.size >= 2) matched = result
    }

    val viewport = remember {
        MapViewportState().apply {
            setCameraOptions {
                center(Point.fromLngLat(-71.537, -16.409))
                zoom(13.0)
                pitch(0.0)
                bearing(0.0)
            }
        }
    }
    val liveSrc = remember { arrayOfNulls<GeoJsonSource>(1) }
    val liveMeSrc = remember { arrayOfNulls<GeoJsonSource>(1) }

    fun fitRouteToScreen() {
        if (matched.size < 2) return
        val lats = matched.map { it.first }
        val lons = matched.map { it.second }
        val minLat = lats.min()
        val maxLat = lats.max()
        val minLon = lons.min()
        val maxLon = lons.max()
        val centerLat = (minLat + maxLat) / 2.0
        val centerLon = (minLon + maxLon) / 2.0
        // Zoom aproximado para que la ruta entre en pantalla (dejando margen
        // para la barra superior y la tarjeta inferior).
        val screenWidthPx = 900.0
        val spanLon = (maxLon - minLon).coerceAtLeast(0.004)
        val spanLat = (maxLat - minLat).coerceAtLeast(0.004)
        val zoomLon = log2(360.0 * screenWidthPx / (256.0 * spanLon))
        val zoomLat = log2(360.0 * screenWidthPx * cos(Math.toRadians(centerLat)) / (256.0 * spanLat))
        val zoom = minOf(zoomLon, zoomLat).coerceIn(11.0, 15.5)
        viewport.easeTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(centerLon, centerLat))
                .zoom(zoom)
                .pitch(0.0)
                .bearing(0.0)
                .padding(com.mapbox.maps.EdgeInsets(0.0, 0.0, 0.0, 0.0))
                .build(),
            MapAnimationOptions.mapAnimationOptions { duration(600L) },
        )
    }

    // Reencuadra la cámara al trazado cuando llega (fuera del modo navegación).
    LaunchedEffect(matched, navigating) {
        if (!navigating) fitRouteToScreen()
    }

    // Modo navegación: la cámara sigue al usuario, inclinada y orientada
    // hacia donde avanza la ruta (estilo inDrive).
    LaunchedEffect(navigating, userLocation?.lat, userLocation?.lon, cameraBearingDeg) {
        val location = userLocation
        if (!navigating || location == null) return@LaunchedEffect
        val currentBearing = viewport.cameraState?.bearing ?: 0.0
        val bearing = cameraBearingDeg
            ?: location.bearingDeg?.toDouble()
            ?: currentBearing
        val movingFast = (location.speedMps ?: 0f) > 8f
        viewport.easeTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(location.lon, location.lat))
                .zoom(if (movingFast) 16.2 else 17.2)
                .pitch(50.0)
                .bearing(bearing)
                // Deja al usuario en el tercio inferior para ver la ruta delante.
                .padding(com.mapbox.maps.EdgeInsets(140.0, 30.0, 40.0, 30.0))
                .build(),
            MapAnimationOptions.mapAnimationOptions { duration(850L) },
        )
    }

    MapboxMap(
        modifier = modifier,
        mapViewportState = viewport,
    ) {
        // Estilo + capas + fuentes (una sola vez).
        MapEffect(Unit) { mv ->
            mv.mapboxMap.loadStyle(Style.STANDARD) { style ->
                val empty = FeatureCollection.fromFeatures(emptyList())
                val src = GeoJsonSource.Builder(ROUTE_SRC).featureCollection(empty).build()
                style.addSource(src)
                liveSrc[0] = src
                // Dibuja de inmediato el trazado actual (el estilo carga async).
                src.featureCollection(FeatureCollection.fromFeatures(lineFeatures(matched)))
                style.addLayer(
                    LineLayer(ROUTE_LINE_CASING, ROUTE_SRC).apply {
                        lineColor("#0F172A")
                        lineWidth(8.0)
                        lineOpacity(0.35)
                    },
                )
                style.addLayer(
                    LineLayer(ROUTE_LINE, ROUTE_SRC).apply {
                        lineColor("#1E88E5")
                        lineWidth(5.0)
                        lineOpacity(0.95)
                    },
                )

                // Punto de ubicación real: solo aparece durante la navegación.
                val meSrc = GeoJsonSource.Builder(ME_SRC).featureCollection(empty).build()
                style.addSource(meSrc)
                liveMeSrc[0] = meSrc
                style.addLayer(
                    CircleLayer(ME_HALO, ME_SRC).apply {
                        circleRadius(24.0)
                        circleColor("#1E88E5")
                        circleOpacity(0.18)
                    },
                )
                style.addLayer(
                    CircleLayer(ME_PIN, ME_SRC).apply {
                        circleRadius(9.0)
                        circleColor("#1E88E5")
                        circleStrokeColor("#FFFFFF")
                        circleStrokeWidth(3.0)
                    },
                )
            }
        }
        // Actualiza la geometría de la ruta sin recargar el estilo.
        MapEffect(matched) { mv ->
            liveSrc[0]?.featureCollection(FeatureCollection.fromFeatures(lineFeatures(matched)))
        }
        // Actualiza la posición real del usuario.
        MapEffect(userLocation, navigating) { _ ->
            val meSrc = liveMeSrc[0] ?: return@MapEffect
            val features = if (navigating && userLocation != null) {
                listOf(
                    Feature.fromGeometry(
                        Point.fromLngLat(userLocation.lon, userLocation.lat)
                    )
                )
            } else {
                emptyList()
            }
            meSrc.featureCollection(FeatureCollection.fromFeatures(features))
        }
    }
}

private fun lineFeatures(points: List<Pair<Double, Double>>): List<Feature> =
    if (points.size >= 2) {
        listOf(
            Feature.fromGeometry(
                LineString.fromLngLats(points.map { Point.fromLngLat(it.second, it.first) })
            )
        )
    } else emptyList()

private suspend fun fetchDirectionsRoute(points: List<Pair<Double, Double>>): List<Pair<Double, Double>>? =
    withContext(Dispatchers.IO) {
        try {
            val coords = points.joinToString(";") { "${it.second},${it.first}" }
            val url = URL(
                "https://api.mapbox.com/directions/v5/mapbox/driving/$coords" +
                    "?geometries=geojson&overview=full&access_token=${MapboxToken.PUBLIC_TOKEN}"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val json = JsonParser.parseString(body).asJsonObject
            val routes = json.getAsJsonArray("routes") ?: return@withContext null
            if (routes.size() == 0) return@withContext null
            val geometry = routes[0].asJsonObject.getAsJsonObject("geometry") ?: return@withContext null
            val coordsArr = geometry.getAsJsonArray("coordinates") ?: return@withContext null
            coordsArr.map { c ->
                val arr = c.asJsonArray
                Pair(arr[1].asDouble, arr[0].asDouble)
            }
        } catch (e: Exception) {
            Log.w("RouteMap", "Directions falló: ${e.message}")
            null
        }
    }
