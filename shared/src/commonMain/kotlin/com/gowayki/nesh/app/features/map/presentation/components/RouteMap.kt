package com.gowayki.nesh.app.features.map.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gowayki.nesh.app.features.map.domain.model.UserLocation

/**
 * Mapa de ruta (estilo Uber/InDrive). En Android usa Mapbox; en iOS, fallback OSM.
 *
 * @param userLocation posición real del GPS (null si aún no hay señal).
 * @param navigating true para seguir al usuario y mostrar el punto azul.
 * @param cameraBearingDeg rumbo del tramo de ruta (orienta el mapa en navegación).
 */
expect @Composable fun RouteMap(
    routePoints: List<Pair<Double, Double>>,
    userLocation: UserLocation?,
    navigating: Boolean,
    cameraBearingDeg: Double?,
    modifier: Modifier = Modifier,
)
