package com.gowayki.nesh.app.features.map.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gowayki.nesh.app.features.map.domain.model.UserLocation

/**
 * Mapa de ruta (estilo Gowayki GO e InDrive).
 * En Android usa Mapbox con trazados de Ida y Retorno diferenciados; en iOS, fallback OSM.
 *
 * @param routePoints trazado de IDA (sentido origen -> destino).
 * @param returnRoutePoints trazado de RETORNO / VUELTA (sentido destino -> origen).
 * @param userLocation posición real del GPS (null si aún no hay señal).
 * @param navigating true para seguir al usuario y orientar la cámara en 3D.
 * @param cameraBearingDeg rumbo del tramo de ruta (orienta el mapa en navegación).
 * @param routeColorHex color distintivo de la IDA.
 * @param returnRouteColorHex color distintivo del RETORNO (marcado contrastante).
 */
expect @Composable fun RouteMap(
    routePoints: List<Pair<Double, Double>>,
    returnRoutePoints: List<Pair<Double, Double>> = emptyList(),
    userLocation: UserLocation?,
    navigating: Boolean,
    cameraBearingDeg: Double?,
    centerOnUserTrigger: Int = 0,
    routeColorHex: String = "#1E88E5",
    returnRouteColorHex: String = "#F59E0B",
    stops: List<Pair<Double, Double>> = emptyList(),
    stopNames: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    /** Índice del paradero tocado por el usuario (para mostrar su ficha). */
    onStopClick: (Int) -> Unit = {},
    onMatchedPointsChanged: (List<Pair<Double, Double>>, List<Pair<Double, Double>>) -> Unit = { _, _ -> },
)
