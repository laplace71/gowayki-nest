package com.gowayki.nesh.app.features.map.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.gowayki.nesh.app.features.map.domain.model.UserLocation
import com.gowayki.nesh.core.theme.src.core.AppColors

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
    Box(modifier.fillMaxSize().background(AppColors.cream), contentAlignment = Alignment.Center) {
        Text("Mapa (iOS: pendiente)")
    }
}
