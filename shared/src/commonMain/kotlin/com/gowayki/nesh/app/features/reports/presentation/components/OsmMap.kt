// Mapa OSM funcional 100% en commonMain: tiles reales de OpenStreetMap
// (API pública XYZ https://tile.openstreetmap.org/{z}/{x}/{y}.png) vía Ktor.
// Gestos suaves estilo Google Maps:
// - 1 dedo arrastra (pan continuo, sin recargas a saltos)
// - 2 dedos pellizcan con zoom FRACCIONADO visual (escala fluida alrededor
//   del punto medio de los dedos); al soltar se consolida al zoom entero
//   más cercano y la escala residual se anima a 1 (sin saltos).
// - Doble-tap = zoom + animado. Tap en marcador = onMarkerClick.
// Dibuja ruta punteada animada entre puntos.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.reports.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.features.reports.domain.model.OsmLatLng
import com.gowayki.nesh.app.features.reports.domain.model.OsmMarkerType
import com.gowayki.nesh.core.theme.src.core.AppColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

// El marcador lo arma la pantalla a partir del modelo de dominio.
data class OsmMarker(val id: String, val pos: OsmLatLng, val type: OsmMarkerType)

// ---------- Estado ----------

class OsmMapState(center: OsmLatLng, zoom: Int) {
    var center by mutableStateOf(center)
    var zoom by mutableStateOf(zoom.coerceIn(3, 19))

    // Zoom/pan VISUAL transitorio: el gesto lo mueve y al soltar se consolida.
    var scale by mutableStateOf(1f) // 1 px mundo = 1 dp × scale
    var shiftX by mutableStateOf(0f) // dp
    var shiftY by mutableStateOf(0f) // dp
    var settleId by mutableStateOf(0)
        private set

    // Vuelo suave: flyTo anima, moveTo salta directo.
    var flyPos by mutableStateOf<OsmLatLng?>(null)
        private set
    var flyId by mutableStateOf(0)
        private set

    private fun resetVisual() {
        scale = 1f
        shiftX = 0f
        shiftY = 0f
    }

    fun zoomIn() {
        resetVisual()
        zoom = (zoom + 1).coerceAtMost(19)
    }

    fun zoomOut() {
        resetVisual()
        zoom = (zoom - 1).coerceAtLeast(3)
    }

    /** Zoom animado (botones y doble-tap): escala ×2 y consolida. */
    fun smoothZoomIn() {
        shiftX = 0f
        shiftY = 0f
        scale = 2f
        commitVisual()
    }

    fun smoothZoomOut() {
        shiftX = 0f
        shiftY = 0f
        scale = 0.5f
        commitVisual()
    }

    /**
     * Consolida escala+desplazamiento visual: ajusta el zoom al entero más
     * cercano, mueve el centro y deja el residuo para animarlo a 1 (sin salto).
     */
    fun commitVisual() {
        if (scale <= 0f) scale = 1f
        val steps = kotlin.math.round(ln(scale.toDouble()) / ln(2.0)).toInt()
        val nz = (zoom + steps).coerceIn(3, 19)
        val actual = nz - zoom
        val z = zoom
        val ncx = lonToWorldX(center.lon, z) - shiftX
        val ncy = latToWorldY(center.lat, z) - shiftY
        center = OsmLatLng(
            worldYToLat(ncy, z).coerceIn(-85.0, 85.0),
            worldXToLon(ncx, z),
        )
        zoom = nz
        shiftX = 0f
        shiftY = 0f
        scale = (scale / 2.0.pow(actual)).toFloat()
        if (abs(scale - 1f) > 0.005f) settleId++ else scale = 1f
    }

    fun moveTo(p: OsmLatLng) {
        flyPos = null
        resetVisual()
        center = p
    }

    fun flyTo(p: OsmLatLng) {
        resetVisual()
        if (p == center && flyPos == null) return
        flyPos = p
        flyId++
    }
}

// ---------- Matemáticas slippy-map (1 px mundo = 1 dp en pantalla) ----------

private fun lonToWorldX(lon: Double, z: Int): Double =
    (lon + 180.0) / 360.0 * (256.0 * 2.0.pow(z))

private fun latToWorldY(lat: Double, z: Int): Double {
    val rad = lat * PI / 180.0
    return (1.0 - asinh(tan(rad)) / PI) / 2.0 * (256.0 * 2.0.pow(z))
}

private fun worldXToLon(x: Double, z: Int): Double =
    x / (256.0 * 2.0.pow(z)) * 360.0 - 180.0

private fun worldYToLat(y: Double, z: Int): Double {
    val n = PI - 2.0 * PI * y / (256.0 * 2.0.pow(z))
    return 180.0 / PI * atan(0.5 * (exp(n) - exp(-n)))
}

// ---------- Mapa ----------
// Estado y tiles los provee el ViewModel; aquí solo gestos + dibujo.

@Composable
fun OsmMap(
    state: OsmMapState,
    tiles: Map<String, ImageBitmap?>,
    markers: List<OsmMarker>,
    selectedId: String?,
    route: List<OsmLatLng> = emptyList(),
    grayScale: Boolean = true,
    onNeedTiles: (z: Int, x0: Int, y0: Int, x1: Int, y1: Int) -> Unit = { _, _, _, _, _ -> },
    onMarkerClick: (OsmMarker) -> Unit = {},
    markerContent: @Composable (OsmMarker, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    // Vuelo suave al punto: interpola lat/lon con easing (750 ms).
    LaunchedEffect(state.flyId) {
        val target = state.flyPos ?: return@LaunchedEffect
        val start = state.center
        if (start == target) return@LaunchedEffect
        val durMs = 750f
        val t0 = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val f = ((now - t0) / 1_000_000f / durMs).coerceIn(0f, 1f)
            val e = FastOutSlowInEasing.transform(f)
            state.center = OsmLatLng(
                start.lat + (target.lat - start.lat) * e,
                start.lon + (target.lon - start.lon) * e,
            )
            if (f >= 1f) break
        }
    }

    // Asienta la escala residual del pinch/botones (250 ms, sin salto).
    LaunchedEffect(state.settleId) {
        if (state.settleId == 0) return@LaunchedEffect
        val anim = androidx.compose.animation.core.Animatable(state.scale)
        anim.animateTo(1f, tween(220, easing = FastOutSlowInEasing)) { state.scale = value }
        state.scale = 1f
    }

    BoxWithConstraints(modifier) {
        val z = state.zoom
        // 1 px de mundo OSM = 1 dp en pantalla (tile de 256 = 256.dp).
        val wDp = maxWidth.value
        val hDp = maxHeight.value

        val cx = lonToWorldX(state.center.lon, z) - state.shiftX
        val cy = latToWorldY(state.center.lat, z) - state.shiftY
        val left = cx - wDp / 2.0
        val top = cy - hDp / 2.0

        // Margen de 1 tile: cubre el pan y el zoom-out visual sin huecos.
        val x0 = floor(left / 256.0).toInt() - 1
        val y0 = floor(top / 256.0).toInt() - 1
        val x1 = floor((left + wDp) / 256.0).toInt() + 1
        val y1 = floor((top + hDp) / 256.0).toInt() + 1
        val n = 2.0.pow(z).toInt()

        // Los tiles esperan a que asiente el primer frame + animación de
        // entrada: así la llegada al dashboard no compite con todo a la vez.
        var tilesGo by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(350)
            tilesGo = true
        }

        // Carga delegada al ViewModel (él decide si descarga o usa caché).
        LaunchedEffect(z, x0, y0, x1, y1, tilesGo) {
            if (!tilesGo) return@LaunchedEffect
            onNeedTiles(z, x0, y0, x1, y1)
        }

        val missing = remember(z, x0, y0, x1, y1, tiles) {
            var c = 0
            for (tx in x0..x1) for (ty in y0..y1) {
                val wx = ((tx % n) + n) % n
                if (ty < 0 || ty >= n) continue
                if (tiles["$z/$wx/$ty"] == null) c++
            }
            // El margen siempre pide de más: solo importa el área visible.
            (c - 8).coerceAtLeast(0)
        }

        // Refs frescas para el gesto (el pointerInput con clave Unit no se reinicia).
        val leftNow by rememberUpdatedState(left)
        val topNow by rememberUpdatedState(top)
        val zNow by rememberUpdatedState(z)
        val wNow by rememberUpdatedState(wDp)
        val hNow by rememberUpdatedState(hDp)
        val markersNow by rememberUpdatedState(markers)
        val clickNow by rememberUpdatedState(onMarkerClick)

        Box(
            Modifier.fillMaxSize()
                .background(AppColors.steel.copy(alpha = 0.25f))
                // UN solo detector: pan + pinch focal + tap/doble-tap.
                // Clave Unit: NUNCA se reinicia a mitad del gesto.
                .pointerInput(Unit) {
                    val slopPx = viewConfiguration.touchSlop
                    val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
                    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                    var lastTapTime = 0L
                    var lastTapPos = Offset.Zero
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downTime = down.uptimeMillis
                        var lastPos = down.position
                        var lastTime = downTime
                        var totalMove = Offset.Zero
                        var maxFingers = 1
                        var ended = false
                        while (!ended) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) {
                                ended = true
                            } else {
                                maxFingers = maxOf(maxFingers, pressed.size)
                                if (pressed.size >= 2) {
                                    // Pinch con punto focal: lo que está bajo los
                                    // dedos se queda bajo los dedos (sin deriva).
                                    val gz = event.calculateZoom()
                                    val dens = density.density
                                    val c = event.calculateCentroid()
                                    val vcx = wNow / 2f
                                    val vcy = hNow / 2f
                                    val cDx = c.x / dens
                                    val cDy = c.y / dens
                                    val sc = state.scale
                                    val qx = (cDx - vcx - state.shiftX) / sc
                                    val qy = (cDy - vcy - state.shiftY) / sc
                                    val s2 = (sc * gz).coerceIn(0.6f, 4f)
                                    state.shiftX = cDx - vcx - qx * s2
                                    state.shiftY = cDy - vcy - qy * s2
                                    state.scale = s2
                                    event.changes.forEach { it.consume() }
                                    totalMove += event.calculatePan()
                                } else {
                                    val pan = event.calculatePan()
                                    totalMove += pan
                                    state.shiftX += pan.x / density.density
                                    state.shiftY += pan.y / density.density
                                    if (totalMove.getDistance() > slopPx) {
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                                lastPos = pressed[0].position
                                lastTime = pressed[0].uptimeMillis
                            }
                        }
                        val isTap = maxFingers == 1 &&
                            totalMove.getDistance() <= slopPx &&
                            lastTime - downTime <= longPressTimeout
                        if (isTap) {
                            // El tap no mueve nada: revierte el micro-shift.
                            state.scale = 1f
                            state.shiftX = 0f
                            state.shiftY = 0f
                            if (lastTime - lastTapTime <= doubleTapTimeout &&
                                (lastPos - lastTapPos).getDistance() <= slopPx * 2
                            ) {
                                // Doble-tap = zoom + animado.
                                lastTapTime = 0L
                                state.smoothZoomIn()
                            } else {
                                lastTapTime = lastTime
                                lastTapPos = lastPos
                                // Hit-test contra marcadores (dp = px de mundo).
                                val tapXDp = lastPos.x / density.density
                                val tapYDp = lastPos.y / density.density
                                val zz = zNow
                                markersNow.firstOrNull { m ->
                                    val mx = lonToWorldX(m.pos.lon, zz) - leftNow
                                    val my = latToWorldY(m.pos.lat, zz) - topNow
                                    val dx = mx - tapXDp
                                    val dy = my - tapYDp
                                    dx * dx + dy * dy <= 46.0 * 46.0
                                }?.let(clickNow)
                            }
                        } else if (state.scale != 1f || state.shiftX != 0f || state.shiftY != 0f) {
                            // Al soltar se consolida: zoom al entero más cercano
                            // y el residuo se anima a 1 (cero saltos).
                            state.commitVisual()
                        }
                    }
                },
        ) {
            // Capa escalable (zoom fraccionado fluido alrededor del centro).
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer {
                        scaleX = state.scale
                        scaleY = state.scale
                    },
            ) {
                // --- Tiles (offsets en Dp: 1 px mundo = 1 dp) ---
                val gray = remember {
                    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                }
                for (tx in x0..x1) for (ty in y0..y1) {
                    val wx = ((tx % n) + n) % n
                    if (ty < 0 || ty >= n) continue
                    val bmp = tiles["$z/$wx/$ty"] ?: continue
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        modifier = Modifier
                            .size(256.dp)
                            .offset((tx * 256.0 - left).dp, (ty * 256.0 - top).dp),
                        contentScale = ContentScale.FillBounds,
                        colorFilter = if (grayScale) gray else null,
                    )
                }

                // --- Ruta punteada animada entre reportes ---
                if (route.size >= 2) {
                    val phase by rememberInfiniteTransition(label = "route").animateFloat(
                        initialValue = 0f, targetValue = 24f,
                        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
                        label = "ph",
                    )
                    Canvas(Modifier.fillMaxSize()) {
                        val path = Path()
                        route.forEachIndexed { i, p ->
                            val x = (lonToWorldX(p.lon, z) - left).dp.toPx()
                            val y = (latToWorldY(p.lat, z) - top).dp.toPx()
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path = path,
                            color = AppColors.onBackground,
                            style = Stroke(
                                width = 5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), phase),
                                cap = StrokeCap.Round,
                            ),
                            alpha = 0.75f,
                        )
                    }
                }

                // --- Marcadores (el tap lo resuelve el detector del mapa) ---
                markers.forEach { m ->
                    val mxDp = lonToWorldX(m.pos.lon, z) - left
                    val myDp = latToWorldY(m.pos.lat, z) - top
                    if (mxDp < -70 || myDp < -70 || mxDp > wDp + 70 || myDp > hDp + 70) return@forEach
                    Box(
                        Modifier.offset(mxDp.dp, myDp.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.offset((-20).dp, (-40).dp)) {
                            markerContent(m, m.id == selectedId)
                        }
                    }
                }
            }

            // --- Cargando tiles (fuera de la capa con escala) ---
            if (tilesGo && missing > 0) {
                Box(
                    Modifier.align(Alignment.TopStart).padding(8.dp)
                        .background(AppColors.cream.copy(alpha = 0.92f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = AppColors.onBackground,
                        )
                        Text("Cargando mapa…", color = AppColors.onBackground, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// dp double → Dp (1 px mundo = 1 dp).
private val Double.dp
    get() = androidx.compose.ui.unit.Dp(this.toFloat())
