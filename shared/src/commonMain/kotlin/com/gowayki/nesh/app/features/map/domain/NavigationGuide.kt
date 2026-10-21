package com.gowayki.nesh.app.features.map.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Tipo de maniobra que se muestra en la guía de navegación. */
enum class NavManeuver { LEFT, RIGHT, STRAIGHT, ARRIVE, OFF_ROUTE }

/**
 * Instrucción calculada a partir del trazado real de la combi y la posición
 * del usuario. No depende de internet: funciona con la geometría empaquetada.
 *
 * @param distanceMeters distancia hasta la próxima maniobra.
 * @param remainingMeters distancia que falta hasta el final de la ruta.
 * @param bearingDeg rumbo del tramo actual (para orientar el mapa).
 */
data class NavInstruction(
    val maneuver: NavManeuver,
    val distanceMeters: Double,
    val remainingMeters: Double,
    val bearingDeg: Double,
)

/**
 * Trazado simplificado listo para calcular giros.
 *
 * Se simplifica una sola vez por ruta (Ramer–Douglas–Peucker con ~8 m) para
 * que el cálculo por cada fix de GPS sea barato incluso en un Helio G99.
 */
class NavigationPath private constructor(
    private val points: List<Pair<Double, Double>>,
    private val cumulative: DoubleArray,
    private val total: Double,
) {

    /**
     * Devuelve la instrucción para la posición actual, o null si la ruta no
     * tiene suficientes puntos.
     */
    fun instruction(lat: Double, lon: Double, deviceBearingDeg: Float? = null): NavInstruction? {
        if (points.size < 2) return null

        // Segmento más cercano al usuario.
        var bestDistance = Double.MAX_VALUE
        var bestSegment = 0
        var bestT = 0.0
        for (i in 0 until points.size - 1) {
            val (distance, t) = projectOnSegment(points[i], points[i + 1], lat, lon)
            if (distance < bestDistance) {
                bestDistance = distance
                bestSegment = i
                bestT = t
            }
        }

        val segmentBearing = bearing(points[bestSegment], points[bestSegment + 1])
        val along = cumulative[bestSegment] + bestT * segmentLength(bestSegment)
        val remaining = (total - along).coerceAtLeast(0.0)

        if (bestDistance > OFF_ROUTE_METERS) {
            return NavInstruction(
                maneuver = NavManeuver.OFF_ROUTE,
                distanceMeters = 0.0,
                remainingMeters = remaining,
                bearingDeg = deviceBearingDeg?.toDouble() ?: segmentBearing,
            )
        }

        if (remaining <= ARRIVE_METERS) {
            return NavInstruction(
                maneuver = NavManeuver.ARRIVE,
                distanceMeters = 0.0,
                remainingMeters = remaining,
                bearingDeg = segmentBearing,
            )
        }

        // Primer vértice delante del usuario con un cambio de dirección claro.
        var turnIndex = -1
        var turnDelta = 0.0
        for (j in (bestSegment + 1) until points.size - 1) {
            val delta = signedAngle(
                bearing(points[j - 1], points[j]),
                bearing(points[j], points[j + 1]),
            )
            if (abs(delta) >= TURN_DEGREES) {
                turnIndex = j
                turnDelta = delta
                break
            }
        }

        if (turnIndex == -1) {
            return NavInstruction(
                maneuver = NavManeuver.STRAIGHT,
                distanceMeters = remaining,
                remainingMeters = remaining,
                bearingDeg = segmentBearing,
            )
        }

        val distanceToTurn = (cumulative[turnIndex] - along).coerceAtLeast(0.0)
        return NavInstruction(
            maneuver = if (turnDelta > 0) NavManeuver.RIGHT else NavManeuver.LEFT,
            distanceMeters = distanceToTurn,
            remainingMeters = remaining,
            bearingDeg = segmentBearing,
        )
    }

    private fun segmentLength(index: Int): Double =
        cumulative[index + 1] - cumulative[index]

    companion object {
        private const val TURN_DEGREES = 28.0
        private const val SIMPLIFY_METERS = 8.0
        private const val OFF_ROUTE_METERS = 120.0
        private const val ARRIVE_METERS = 25.0
        private const val EARTH_RADIUS_M = 6_371_000.0

        fun build(raw: List<Pair<Double, Double>>): NavigationPath {
            val points = simplify(raw)
            val cumulative = DoubleArray(points.size)
            for (i in 1 until points.size) {
                cumulative[i] = cumulative[i - 1] + haversine(points[i - 1], points[i])
            }
            return NavigationPath(points, cumulative, cumulative.lastOrNull() ?: 0.0)
        }

        /** Distancia legible: "80 m", "450 m", "1.2 km". */
        fun formatDistance(meters: Double): String = when {
            meters >= 1_000.0 -> {
                val km = meters / 100.0
                val rounded = km.roundToInt() / 10.0
                "${rounded.toString().replace('.', ',')} km"
            }
            meters >= 100.0 -> "${(meters / 10.0).roundToInt() * 10} m"
            else -> "${meters.roundToInt().coerceAtLeast(0)} m"
        }

        // ── Geometría ───────────────────────────────────────────────────────

        private fun simplify(points: List<Pair<Double, Double>>): List<Pair<Double, Double>> {
            if (points.size < 3) return points
            val keep = BooleanArray(points.size)
            keep[0] = true
            keep[points.size - 1] = true
            val stack = ArrayDeque<Pair<Int, Int>>()
            stack.addLast(0 to points.size - 1)
            while (stack.isNotEmpty()) {
                val (start, end) = stack.removeLast()
                if (end <= start + 1) continue
                var maxDistance = 0.0
                var index = -1
                for (k in start + 1 until end) {
                    val distance = projectOnSegment(points[start], points[end], points[k].first, points[k].second).first
                    if (distance > maxDistance) {
                        maxDistance = distance
                        index = k
                    }
                }
                if (maxDistance > SIMPLIFY_METERS && index > 0) {
                    keep[index] = true
                    stack.addLast(start to index)
                    stack.addLast(index to end)
                }
            }
            return points.filterIndexed { index, _ -> keep[index] }
        }

        /** Proyección local (metros) del punto sobre el segmento a–b. */
        private fun projectOnSegment(
            a: Pair<Double, Double>,
            b: Pair<Double, Double>,
            lat: Double,
            lon: Double,
        ): Pair<Double, Double> {
            val midLat = (a.first + b.first) / 2.0 * PI / 180.0
            val kx = 111_320.0 * cos(midLat)
            val ky = 110_540.0

            val ax = (a.second - lon) * kx
            val ay = (a.first - lat) * ky
            val bx = (b.second - lon) * kx
            val by = (b.first - lat) * ky

            val dx = bx - ax
            val dy = by - ay
            val lengthSq = dx * dx + dy * dy
            val t = if (lengthSq == 0.0) 0.0 else ((-ax) * dx + (-ay) * dy) / lengthSq
            val clamped = t.coerceIn(0.0, 1.0)
            val px = ax + dx * clamped
            val py = ay + dy * clamped
            return sqrt(px * px + py * py) to clamped
        }

        private fun haversine(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
            val dLat = (b.first - a.first) * PI / 180.0
            val dLon = (b.second - a.second) * PI / 180.0
            val lat1 = a.first * PI / 180.0
            val lat2 = b.first * PI / 180.0
            val h = sin(dLat / 2).pow(2) +
                cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
            return 2 * EARTH_RADIUS_M * asin(sqrt(h.coerceIn(0.0, 1.0)))
        }

        private fun bearing(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
            val lat1 = a.first * PI / 180.0
            val lat2 = b.first * PI / 180.0
            val dLon = (b.second - a.second) * PI / 180.0
            val y = sin(dLon) * cos(lat2)
            val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
            return (atan2(y, x) * 180.0 / PI + 360.0) % 360.0
        }

        /** Diferencia angular con signo en (-180, 180]; positivo = derecha. */
        private fun signedAngle(from: Double, to: Double): Double =
            ((to - from + 540.0) % 360.0) - 180.0
    }
}
