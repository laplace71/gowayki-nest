// Actual Android — mapa Mapbox real con heatmap NATIVO (capa HeatmapLayer).
// Pins 100% centrados (los dibuja el SDK, sin offsets manuales).
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.sos.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.google.gson.JsonObject
import com.gowayki.nesh.app.features.reports.presentation.StateFlow.ReportsViewModel
import com.gowayki.nesh.app.features.reports.presentation.components.OsmMarker
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.layers.generated.HeatmapLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import kotlin.math.hypot

private const val SOS_SRC = "sos-src"
private const val SOS_HEAT = "sos-heat"
private const val SOS_HALO = "sos-halo"
private const val SOS_PINS = "sos-pins"

private fun featureCollection(markers: List<OsmMarker>, selectedId: String?): FeatureCollection =
    FeatureCollection.fromFeatures(
        markers.map { m ->
            Feature.fromGeometry(
                Point.fromLngLat(m.pos.lon, m.pos.lat),
                JsonObject().apply {
                    addProperty("id", m.id)
                    addProperty("sel", if (m.id == selectedId) 1 else 0)
                },
            )
        },
    )

// Radio según selección (0 = normal, 1 = tocado): el anillo queda CENTRADO en el pin.
private fun selRadius(normal: Double, selected: Double): Expression = Expression.interpolate(
    Expression.linear(),
    Expression.toNumber(Expression.get("sel")),
    Expression.literal(0.0),
    Expression.literal(normal),
    Expression.literal(1.0),
    Expression.literal(selected),
)

/** Degradado del foco de calor: rojo intenso al centro, se apaga hacia fuera. */
private fun heatColor(): Expression = Expression.interpolate(
    Expression.linear(),
    Expression.heatmapDensity(),
    Expression.literal(0.0),
    Expression.rgba(255.0, 138.0, 61.0, 0.0),
    Expression.literal(0.25),
    Expression.rgba(255.0, 138.0, 61.0, 0.45),
    Expression.literal(0.55),
    Expression.rgba(226.0, 72.0, 72.0, 0.75),
    Expression.literal(0.80),
    Expression.rgba(198.0, 64.0, 73.0, 0.92),
    Expression.literal(1.0),
    Expression.rgba(140.0, 24.0, 34.0, 0.95),
)

@Composable
actual fun SosHeatMap(
    markers: List<OsmMarker>,
    selectedId: String?,
    reportsVm: ReportsViewModel,
    onMarkerClick: (OsmMarker) -> Unit,
    modifier: Modifier,
) {
    // Token una sola vez (si aún es placeholder, el mapa sale vacío hasta pegarlo).
    remember {
        if (MapboxToken.PUBLIC_TOKEN.startsWith("pk.")) {
            MapboxOptions.accessToken = MapboxToken.PUBLIC_TOKEN
        }
        true
    }
    val center = reportsVm.map.center
    val viewport = remember {
        MapViewportState().apply {
            setCameraOptions {
                center(Point.fromLngLat(center.lon, center.lat))
                zoom(12.5)
                pitch(0.0)
                bearing(0.0)
            }
        }
    }
    // Fuente viva para refrescar puntos sin recargar el estilo.
    val liveSrc = remember { arrayOfNulls<GeoJsonSource>(1) }
    // Refs frescas: el listener de taps se registra una vez, pero debe ver
    // los puntos actuales (si no, compara contra la lista vacía inicial).
    val markersNow by rememberUpdatedState(markers)
    val clickNow by rememberUpdatedState(onMarkerClick)
    val selectedNow by rememberUpdatedState(selectedId)

    MapboxMap(
        modifier = modifier,
        mapViewportState = viewport,
    ) {
        // Estilo + capas (una vez).
        MapEffect(Unit) { mv ->
            mv.mapboxMap.loadStyle(Style.STANDARD) { style ->
                val src = GeoJsonSource.Builder(SOS_SRC)
                    .featureCollection(featureCollection(markersNow, selectedNow))
                    .build()
                style.addSource(src)
                // ── Marca de calor ──────────────────────────────────────
                // El aviso se lee como un foco de calor: halo rojo que se
                // difumina hacia fuera y un núcleo sólido en el punto exacto.
                style.addLayer(
                    HeatmapLayer(SOS_HEAT, SOS_SRC).apply {
                        heatmapColor(heatColor())
                        heatmapIntensity(1.4)
                        heatmapRadius(70.0)
                        heatmapOpacity(0.9)
                    },
                )
                // Halo intermedio: separa el núcleo del degradado.
                style.addLayer(
                    CircleLayer(SOS_HALO, SOS_SRC).apply {
                        circleRadius(selRadius(15.0, 21.0))
                        circleColor(Expression.rgba(198.0, 64.0, 73.0, 0.35))
                        circleBlur(1.0)
                    },
                )
                // Núcleo: el punto exacto de la emergencia.
                style.addLayer(
                    CircleLayer(SOS_PINS, SOS_SRC).apply {
                        circleRadius(selRadius(9.0, 13.0))
                        circleColor(Expression.rgb(198.0, 64.0, 73.0))
                        circleStrokeColor(Expression.rgb(255.0, 253.0, 246.0))
                        circleStrokeWidth(2.5)
                    },
                )
                liveSrc[0] = src
            }
            // Punto azul "estás aquí" en vivo (permisos ya concedidos).
            runCatching {
                mv.location.updateSettings {
                    enabled = true
                    pulsingEnabled = true
                }
            }
            // Tap en pin: pin más cercano al toque (48 px). Centrado garantizado por SDK.
            mv.gestures.addOnMapClickListener { tap ->
                val mb = mv.mapboxMap
                val tp = mb.pixelForCoordinate(tap)
                val hit = markersNow.minByOrNull { m ->
                    val sp = mb.pixelForCoordinate(Point.fromLngLat(m.pos.lon, m.pos.lat))
                    hypot((sp.x - tp.x).toDouble(), (sp.y - tp.y).toDouble())
                }
                val sp = hit?.let { mb.pixelForCoordinate(Point.fromLngLat(it.pos.lon, it.pos.lat)) }
                if (hit != null && sp != null &&
                    hypot((sp.x - tp.x).toDouble(), (sp.y - tp.y).toDouble()) <= 48.0
                ) {
                    clickNow(hit)
                    true
                } else {
                    false
                }
            }
        }
        // Puntos nuevos (botón físico) y cambios de selección sin recargar nada.
        MapEffect(markers, selectedId) { mv ->
            liveSrc[0]?.featureCollection(featureCollection(markers, selectedId))
        }
        // Selección: vuelo suave al punto. Clave focusSeq: centra SIEMPRE,
        // incluso re-tocando el mismo pin, desde la tarjeta o al presionar el botón.
        MapEffect(reportsVm.ui.focusSeq) { mv ->
            val sel = reportsVm.ui.selectedId
            markers.firstOrNull { it.id == sel }?.let { m ->
                mv.camera.flyTo(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(m.pos.lon, m.pos.lat))
                        .zoom(14.5)
                        .build(),
                    MapAnimationOptions.mapAnimationOptions { duration(900L) },
                    null,
                )
            }
        }
        // Mi posición (calibrar): vuelo libre a la coordenada GPS.
        MapEffect(reportsVm.ui.locateSeq) { mv ->
            reportsVm.ui.locatePos?.let { p ->
                mv.camera.flyTo(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(p.lon, p.lat))
                        .zoom(15.0)
                        .build(),
                    MapAnimationOptions.mapAnimationOptions { duration(900L) },
                    null,
                )
            }
        }
    }
}
