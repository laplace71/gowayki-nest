package com.gowayki.nesh.app.features.map.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.gson.JsonObject
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
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.IconRotationAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.SymbolPlacement
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.gowayki.nesh.app.features.map.domain.model.UserLocation
import com.gowayki.nesh.app.features.sos.presentation.components.MapboxToken
import kotlin.math.cos
import kotlin.math.log2

// Identificadores de capas y fuentes inspirados en Gowayki GO
private const val ROUTE_SRC = "gowayki-transit-line-src"
private const val ROUTE_LINE_CASING = "gowayki-transit-line-casing"
private const val ROUTE_LINE = "gowayki-transit-line"

private const val RETURN_ROUTE_SRC = "gowayki-transit-return-src"
private const val RETURN_ROUTE_LINE_CASING = "gowayki-transit-return-casing"
private const val RETURN_ROUTE_LINE = "gowayki-transit-return-line"

private const val STOPS_SRC = "gowayki-transit-line-stops-src"
private const val STOPS_LAYER_OUTER = "gowayki-transit-line-stops-outer"
private const val STOPS_LAYER_INNER = "gowayki-transit-line-stops-inner"

private const val TERMINALS_SRC = "gowayki-markers-terminals-src"
private const val TERMINALS_LAYER = "gowayki-markers-terminals-layer"

private const val ME_SRC = "gowayki-me-src"
private const val ME_HALO = "gowayki-me-halo"
private const val ME_PIN = "gowayki-me-pin"

private const val ICON_COMBI_START = "gowayki-marker-combi_start"
private const val ICON_COMBI_END = "gowayki-marker-combi_end"
private const val ICON_PEDESTRIAN_STOP = "gowayki-marker-pedestrian_stop"
private const val ICON_ARROW = "gowayki-arrow"

@Composable
actual fun RouteMap(
    routePoints: List<Pair<Double, Double>>,
    returnRoutePoints: List<Pair<Double, Double>>,
    userLocation: UserLocation?,
    navigating: Boolean,
    cameraBearingDeg: Double?,
    centerOnUserTrigger: Int,
    routeColorHex: String,
    returnRouteColorHex: String,
    stops: List<Pair<Double, Double>>,
    stopNames: List<String>,
    modifier: Modifier,
    onStopClick: (Int) -> Unit,
    onMatchedPointsChanged: (List<Pair<Double, Double>>, List<Pair<Double, Double>>) -> Unit,
) {
    remember {
        if (MapboxToken.PUBLIC_TOKEN.startsWith("pk.")) {
            MapboxOptions.accessToken = MapboxToken.PUBLIC_TOKEN
        }
        true
    }

    var matched by remember(routePoints) { mutableStateOf(routePoints) }
    LaunchedEffect(routePoints) {
        // La geometría ya viene suavizada offline (route_geometry.json).
        // Se muestra tal cual para ser determinista y funcionar sin red.
        matched = routePoints
    }

    var returnMatched by remember(returnRoutePoints) { mutableStateOf(returnRoutePoints) }
    LaunchedEffect(returnRoutePoints) {
        returnMatched = returnRoutePoints
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
    val liveReturnSrc = remember { arrayOfNulls<GeoJsonSource>(1) }
    val liveStopsSrc = remember { arrayOfNulls<GeoJsonSource>(1) }
    val liveTerminalsSrc = remember { arrayOfNulls<GeoJsonSource>(1) }
    val liveMeSrc = remember { arrayOfNulls<GeoJsonSource>(1) }

    // Refs "vivas" a los puntos actuales, accesibles desde el callback de loadStyle
    // sin capturar closures stale.
    val latestMatched = remember { arrayOf(matched) }
    val latestReturnMatched = remember { arrayOf(returnMatched) }
    val latestStops = remember { arrayOf(stops) }
    latestMatched[0] = matched
    latestReturnMatched[0] = returnMatched
    latestStops[0] = stops

    fun fitRouteToScreen(currentMatched: List<Pair<Double, Double>>, currentReturnMatched: List<Pair<Double, Double>>) {
        val allPoints = currentMatched + currentReturnMatched
        if (allPoints.size < 2) return
        val lats = allPoints.map { it.first }
        val lons = allPoints.map { it.second }
        val minLat = lats.min()
        val maxLat = lats.max()
        val minLon = lons.min()
        val maxLon = lons.max()
        val centerLat = (minLat + maxLat) / 2.0
        val centerLon = (minLon + maxLon) / 2.0

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
                .padding(com.mapbox.maps.EdgeInsets(80.0, 40.0, 140.0, 40.0))
                .build(),
            MapAnimationOptions.mapAnimationOptions { duration(650L) },
        )
    }

    // Reencuadra la cámara al trazado cuando llega (fuera de modo navegación).
    // Se pasan los puntos actuales explícitamente para evitar closures stale.
    LaunchedEffect(matched, returnMatched, navigating) {
        if (!navigating) fitRouteToScreen(matched, returnMatched)
    }
    // Notifica al padre los puntos emparejados (suavizados) para navegación/instrucciones.
    LaunchedEffect(matched, returnMatched) {
        onMatchedPointsChanged(matched, returnMatched)
    }

    // Centra la cámara en la ubicación del usuario al pulsar "mi ubicación".
    LaunchedEffect(centerOnUserTrigger) {
        if (centerOnUserTrigger == 0) return@LaunchedEffect
        val location = userLocation ?: return@LaunchedEffect
        viewport.easeTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(location.lon, location.lat))
                .zoom(16.2)
                .pitch(0.0)
                .bearing(0.0)
                .build(),
            MapAnimationOptions.mapAnimationOptions { duration(600L) },
        )
    }

    // Modo navegación: la cámara sigue al usuario, inclinada en 3D (pitch 50°) y orientada
    // hacia donde avanza la ruta (estilo inDrive y Gowayki GO).
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
                .padding(com.mapbox.maps.EdgeInsets(140.0, 30.0, 60.0, 30.0))
                .build(),
            MapAnimationOptions.mapAnimationOptions { duration(850L) },
        )
    }

    MapboxMap(
        modifier = modifier,
        mapViewportState = viewport,
        onMapClickListener = { point ->
            val clickedLat = point.latitude()
            val clickedLon = point.longitude()
            // Buscar el paradero más cercano al punto tocado.
            var bestIdx = -1
            var bestDist = Double.MAX_VALUE
            stops.forEachIndexed { idx, stop ->
                val d = haversineMeters(clickedLat, clickedLon, stop.first, stop.second)
                if (d < bestDist) { bestDist = d; bestIdx = idx }
            }
            if (bestIdx >= 0 && bestDist <= 120.0) {
                onStopClick(bestIdx)
            }
            false
        },
    ) {
        MapEffect(Unit) { mv ->
            mv.mapboxMap.loadStyle(Style.STANDARD) { style ->
                // Registrar marcadores nativos de Gowayki
                loadAndRegisterMarkers(mv.context, style)

                val empty = FeatureCollection.fromFeatures(emptyList())

                // 1. Trazado de RETORNO / VUELTA (Casing oscuro + Color marcado distintivo)
                val returnSrc = GeoJsonSource.Builder(RETURN_ROUTE_SRC).featureCollection(empty).build()
                style.addSource(returnSrc)
                liveReturnSrc[0] = returnSrc
                // Aplicar el estado ACTUAL de returnMatched (no el del momento de inicialización)
                returnSrc.featureCollection(FeatureCollection.fromFeatures(lineFeatures(returnMatched)))

                style.addLayer(
                    LineLayer(RETURN_ROUTE_LINE_CASING, RETURN_ROUTE_SRC).apply {
                        lineColor("#0F172A")
                        lineWidth(10.0)
                        lineOpacity(0.4)
                        lineCap(LineCap.ROUND)
                        lineJoin(LineJoin.ROUND)
                        lineOffset(6.0)
                    },
                )
                style.addLayer(
                    LineLayer(RETURN_ROUTE_LINE, RETURN_ROUTE_SRC).apply {
                        lineColor(returnRouteColorHex)
                        lineWidth(6.0)
                        lineOpacity(0.92)
                        lineCap(LineCap.ROUND)
                        lineJoin(LineJoin.ROUND)
                        lineOffset(6.0)
                    },
                )
                // Flechas de dirección (retorno)
                style.addLayer(
                    SymbolLayer("gowayki-transit-return-arrows", RETURN_ROUTE_SRC).apply {
                        iconImage(ICON_ARROW)
                        symbolPlacement(SymbolPlacement.LINE)
                        iconRotationAlignment(IconRotationAlignment.MAP)
                        iconSize(0.45)
                        iconColor("#FFFFFF")
                        symbolSpacing(110.0)
                        iconAllowOverlap(true)
                        iconIgnorePlacement(true)
                    },
                )

                // 2. Trazado de IDA (Casing oscuro + Línea principal de transporte)
                val src = GeoJsonSource.Builder(ROUTE_SRC).featureCollection(empty).build()
                style.addSource(src)
                liveSrc[0] = src
                // Aplicar el estado ACTUAL de matched (no el del momento de inicialización)
                src.featureCollection(FeatureCollection.fromFeatures(lineFeatures(matched)))

                style.addLayer(
                    LineLayer(ROUTE_LINE_CASING, ROUTE_SRC).apply {
                        lineColor("#0F172A")
                        lineWidth(10.0)
                        lineOpacity(0.4)
                        lineCap(LineCap.ROUND)
                        lineJoin(LineJoin.ROUND)
                        lineOffset(6.0)
                    },
                )
                style.addLayer(
                    LineLayer(ROUTE_LINE, ROUTE_SRC).apply {
                        lineColor(routeColorHex)
                        lineWidth(6.0)
                        lineOpacity(0.95)
                        lineCap(LineCap.ROUND)
                        lineJoin(LineJoin.ROUND)
                        lineOffset(6.0)
                    },
                )
                // Flechas de dirección (ida)
                style.addLayer(
                    SymbolLayer("gowayki-transit-line-arrows", ROUTE_SRC).apply {
                        iconImage(ICON_ARROW)
                        symbolPlacement(SymbolPlacement.LINE)
                        iconRotationAlignment(IconRotationAlignment.MAP)
                        iconSize(0.45)
                        iconColor("#FFFFFF")
                        symbolSpacing(110.0)
                        iconAllowOverlap(true)
                        iconIgnorePlacement(true)
                    },
                )

                // 3. Paraderos intermedios a lo largo de la ruta
                val stopsSrc = GeoJsonSource.Builder(STOPS_SRC).featureCollection(empty).build()
                style.addSource(stopsSrc)
                liveStopsSrc[0] = stopsSrc
                // Aplicar el estado ACTUAL de stops (no el del momento de inicialización)
                stopsSrc.featureCollection(FeatureCollection.fromFeatures(stopFeatures(stops)))

                style.addLayer(
                    CircleLayer(STOPS_LAYER_OUTER, STOPS_SRC).apply {
                        circleRadius(
                            Expression.interpolate(
                                Expression.linear(),
                                Expression.zoom(),
                                Expression.literal(10.0),
                                Expression.literal(2.6),
                                Expression.literal(15.0),
                                Expression.literal(5.5)
                            )
                        )
                        circleColor("#FFFFFF")
                        circleStrokeColor("#64748B")
                        circleStrokeWidth(1.2)
                        circleOpacity(
                            Expression.interpolate(
                                Expression.linear(),
                                Expression.zoom(),
                                Expression.literal(10.0),
                                Expression.literal(0.45),
                                Expression.literal(13.5),
                                Expression.literal(1.0)
                            )
                        )
                    },
                )
                style.addLayer(
                    CircleLayer(STOPS_LAYER_INNER, STOPS_SRC).apply {
                        circleRadius(
                            Expression.interpolate(
                                Expression.linear(),
                                Expression.zoom(),
                                Expression.literal(10.0),
                                Expression.literal(1.4),
                                Expression.literal(15.0),
                                Expression.literal(3.2)
                            )
                        )
                        circleColor(routeColorHex)
                        circleOpacity(
                            Expression.interpolate(
                                Expression.linear(),
                                Expression.zoom(),
                                Expression.literal(10.0),
                                Expression.literal(0.45),
                                Expression.literal(13.5),
                                Expression.literal(1.0)
                            )
                        )
                    },
                )

                // 4. Marcadores de Terminales (Inicio Combi Verde, Fin Combi Roja)
                val terminalsSrc = GeoJsonSource.Builder(TERMINALS_SRC).featureCollection(empty).build()
                style.addSource(terminalsSrc)
                liveTerminalsSrc[0] = terminalsSrc
                // Aplicar el estado ACTUAL de matched y returnMatched (no el del momento de inicialización)
                terminalsSrc.featureCollection(FeatureCollection.fromFeatures(terminalFeatures(matched, returnMatched)))

                style.addLayer(
                    SymbolLayer(TERMINALS_LAYER, TERMINALS_SRC).apply {
                        iconImage(Expression.get("icon"))
                        iconAnchor(IconAnchor.BOTTOM)
                        iconAllowOverlap(true)
                        iconIgnorePlacement(true)
                        iconSize(0.60)
                    },
                )

                // 5. Punto de ubicación real: halo de precisión y pin blanco/azul
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
                        circleRadius(9.5)
                        circleColor("#1E88E5")
                        circleStrokeColor("#FFFFFF")
                        circleStrokeWidth(3.0)
                    },
                )
            }
        }

        // Actualiza trazado de IDA
        MapEffect(matched) { _ ->
            liveSrc[0]?.featureCollection(FeatureCollection.fromFeatures(lineFeatures(matched)))
            liveTerminalsSrc[0]?.featureCollection(FeatureCollection.fromFeatures(terminalFeatures(matched, returnMatched)))
        }

        // Actualiza trazado de RETORNO
        MapEffect(returnMatched) { _ ->
            liveReturnSrc[0]?.featureCollection(FeatureCollection.fromFeatures(lineFeatures(returnMatched)))
            liveTerminalsSrc[0]?.featureCollection(FeatureCollection.fromFeatures(terminalFeatures(matched, returnMatched)))
        }

        // Actualiza los paraderos intermedios
        MapEffect(stops) { _ ->
            liveStopsSrc[0]?.featureCollection(FeatureCollection.fromFeatures(stopFeatures(stops)))
        }

        // Actualiza la posición real del usuario
        MapEffect(userLocation, navigating) { _ ->
            val meSrc = liveMeSrc[0] ?: return@MapEffect
            val features = if (userLocation != null) {
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

/** Carga los pines oficiales de Gowayki GO desde assets o genera un fallback si faltan. */
private fun loadAndRegisterMarkers(context: Context, style: Style) {
    try {
        val startBm = loadAssetBitmap(context, "markers/ic_marker_combi_start.png")
            ?: createFallbackPinBitmap(android.graphics.Color.parseColor("#10B981"))
        style.addImage(ICON_COMBI_START, startBm)

        val endBm = loadAssetBitmap(context, "markers/ic_marker_combi_end.png")
            ?: createFallbackPinBitmap(android.graphics.Color.parseColor("#EF4444"))
        style.addImage(ICON_COMBI_END, endBm)

        val stopBm = loadAssetBitmap(context, "markers/ic_marker_pedestrian_stop.png")
            ?: createFallbackPinBitmap(android.graphics.Color.parseColor("#3B82F6"))
        style.addImage(ICON_PEDESTRIAN_STOP, stopBm)

        // Flecha de dirección (apunta al este; Mapbox la rota según el trazado).
        val arrowBm = createArrowBitmap(android.graphics.Color.WHITE)
        style.addImage(ICON_ARROW, arrowBm)
    } catch (e: Exception) {
        Log.w("RouteMap", "Error registrando marcadores en estilo: ${e.message}")
    }
}

private fun loadAssetBitmap(context: Context, path: String): Bitmap? {
    return try {
        context.assets.open(path).use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (e: Exception) {
        Log.w("RouteMap", "Asset no encontrado $path: ${e.message}")
        null
    }
}

private fun createFallbackPinBitmap(color: Int): Bitmap {
    val size = 96
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 3f, size / 3f, paint)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 3f, size / 6f, paint)
    val path = Path().apply {
        moveTo(size / 4f, size / 3f)
        lineTo(size * 3 / 4f, size / 3f)
        lineTo(size / 2f, size * 0.9f)
        close()
    }
    paint.color = color
    canvas.drawPath(path, paint)
    return bitmap
}


/**
 * Flecha de sentido para [SymbolLayer] con `symbolPlacement = LINE`.
 *
 * El glifo se dibuja desplazado hacia abajo (+Y) dentro del bitmap. Como Mapbox
 * rota el ícono siguiendo la dirección del trazado, ese desplazamiento queda
 * perpendicular a la vía y hace que la flecha caiga exactamente sobre su propia
 * línea (la capa de líneas usa `lineOffset = 6 px` hacia la derecha del sentido).
 *
 * Esto evita que las flechas de IDA y VUELTA se dibujen una encima de otra en el
 * eje de la calle.
 */
private fun createArrowBitmap(color: Int): Bitmap {
    val size = 64
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    // iconSize = 0.45 -> 13 px del bitmap equivalen a ~6 px en pantalla.
    val lateralPx = 13.3f
    val cx = size / 2f
    val cy = size / 2f + lateralPx
    val halfW = 13f
    val halfH = 10f
    val path = Path().apply {
        moveTo(cx - halfW, cy - halfH)
        lineTo(cx + halfW, cy)
        lineTo(cx - halfW, cy + halfH)
        close()
    }
    canvas.drawPath(path, paint)
    return bitmap
}


private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val dLat = (lat2 - lat1) * kotlin.math.PI / 180.0
    val dLon = (lon2 - lon1) * kotlin.math.PI / 180.0
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
        kotlin.math.cos(lat1 * kotlin.math.PI / 180.0) * kotlin.math.cos(lat2 * kotlin.math.PI / 180.0) *
        kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return r * c
}

private fun lineFeatures(points: List<Pair<Double, Double>>): List<Feature> {
    // Limpia duplicados exactos o casi idénticos (< 0.4 m) que producen
    // segmentos de longitud cero y artefactos de renderizado en las uniones.
    val clean = ArrayList<Pair<Double, Double>>(points.size)
    for (p in points) {
        val last = clean.lastOrNull()
        if (last == null || haversineMeters(last.first, last.second, p.first, p.second) > 0.4) {
            clean.add(p)
        }
    }
    if (clean.size < 2) return emptyList()
    return listOf(
        Feature.fromGeometry(
            LineString.fromLngLats(clean.map { Point.fromLngLat(it.second, it.first) })
        )
    )
}

private fun terminalFeatures(
    idaPoints: List<Pair<Double, Double>>,
    vueltaPoints: List<Pair<Double, Double>>
): List<Feature> {
    val features = mutableListOf<Feature>()
    if (idaPoints.isNotEmpty()) {
        val start = idaPoints.first()
        features.add(
            Feature.fromGeometry(
                Point.fromLngLat(start.second, start.first),
                JsonObject().apply {
                    addProperty("icon", ICON_COMBI_START)
                    addProperty("title", "Inicio de Ruta (Ida)")
                }
            )
        )
    }
    val destination = if (vueltaPoints.isNotEmpty()) {
        vueltaPoints.first()
    } else if (idaPoints.size > 1) {
        idaPoints.last()
    } else null

    if (destination != null) {
        features.add(
            Feature.fromGeometry(
                Point.fromLngLat(destination.second, destination.first),
                JsonObject().apply {
                    addProperty("icon", ICON_COMBI_END)
                    addProperty("title", "Terminal de Retorno")
                }
            )
        )
    }
    return features
}

private fun stopFeatures(stops: List<Pair<Double, Double>>): List<Feature> =
    stops.mapIndexed { index, stop ->
        Feature.fromGeometry(
            Point.fromLngLat(stop.second, stop.first),
            JsonObject().apply {
                addProperty("id", index)
                addProperty("title", "Paradero ${index + 1}")
            }
        )
    }
