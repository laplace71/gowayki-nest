package com.gowayki.nesh.app.features.map.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import com.gowayki.nesh.app.common.ui.shared.BackHandler
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.app.common.ui.shared.WaykiBottomNav
import com.gowayki.nesh.app.features.map.data.RouteGeometryStore
import com.gowayki.nesh.app.features.map.data.RouteStopsStore
import com.gowayki.nesh.app.features.map.data.SitRouteStop
import com.gowayki.nesh.app.features.map.data.createDeviceLocationSource
import com.gowayki.nesh.app.features.map.domain.NavInstruction
import com.gowayki.nesh.app.features.map.domain.NavManeuver
import com.gowayki.nesh.app.features.map.domain.NavigationPath
import com.gowayki.nesh.app.features.map.domain.model.ArequipaRoutes
import com.gowayki.nesh.app.features.map.domain.model.UserLocation
import com.gowayki.nesh.app.features.map.presentation.RouteDirection
import com.gowayki.nesh.app.features.map.presentation.SelectedRouteHolder
import com.gowayki.nesh.app.features.map.presentation.components.RouteMap
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowUp
import compose.icons.feathericons.CornerUpLeft
import compose.icons.feathericons.CornerUpRight
import compose.icons.feathericons.Flag
import compose.icons.feathericons.Menu
import compose.icons.feathericons.Navigation
import compose.icons.feathericons.Sliders
import compose.icons.feathericons.X
import compose.icons.fontawesomeicons.SolidGroup
import compose.icons.fontawesomeicons.solid.Bus
import compose.icons.fontawesomeicons.solid.ChevronDown
import compose.icons.fontawesomeicons.solid.ShuttleVan

@Composable
fun MapScreen(
    onBack: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onCommunicationClick: () -> Unit = {},
    onRecorridoClick: () -> Unit = {},
    onMapaClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
) {
    val routeState = SelectedRouteHolder.selected.collectAsState()
    val route = routeState.value

    val sharedDirection = SelectedRouteHolder.direction.collectAsState()
    var selectedDirection by remember(route?.id) { mutableStateOf(sharedDirection.value) }
    
    // Estado de carga: true hasta que la geometría esté lista o se confirme que no hay geometría
    var isLoadingGeometry by remember(route?.id) { mutableStateOf(true) }
    
    var idaPoints by remember(route?.id) {
        mutableStateOf(
            // Usa el cache si corresponde a la ruta actual; si no, empieza vacío
            // y el LaunchedEffect cargará la geometría real.
            SelectedRouteHolder.cachedIdaPoints(route?.id)
        )
    }
    var returnPoints by remember(route?.id) {
        mutableStateOf(
            SelectedRouteHolder.cachedReturnPoints(route?.id)
        )
    }
    // Trazado suavizado (Mapbox Directions) para navegación e instrucciones de giro.
    var matchedIdaPoints by remember(route?.id) {
        mutableStateOf(route?.waypoints ?: emptyList())
    }
    var matchedReturnPoints by remember(route?.id) {
        mutableStateOf<List<Pair<Double, Double>>>(emptyList())
    }
    var idaStopList by remember(route?.id) {
        mutableStateOf<List<SitRouteStop>>(emptyList())
    }
    var returnStopList by remember(route?.id) {
        mutableStateOf<List<SitRouteStop>>(emptyList())
    }

    // Colores fijos por sentido: IDA azul brillante, VUELTA rosada.
    val routeColorHex = "#2196F3"
    val returnRouteColorHex = "#E91E63"

    // Carga los trazados reales empaquetados (Ida y Retorno) y los paraderos de la ruta.
    LaunchedEffect(route?.id, route?.routeId, route?.returnRouteId) {
        // Si hay cache, no mostrar loader
        val hasCache = idaPoints.isNotEmpty() || returnPoints.isNotEmpty()
        if (hasCache) {
            isLoadingGeometry = false
        }
        
        val id = route?.routeId
        if (id != null) {
            val geometry = RouteGeometryStore.geometryFor(id)
            if (!geometry.isNullOrEmpty()) {
                idaPoints = geometry
                matchedIdaPoints = geometry
            }
            // Si no hay geometría en el store, idaPoints queda como fue inicializado
            // (vacío para ruta nueva, cacheado para ruta conocida).

            val loadedStops = RouteStopsStore.stopsFor(id)
            if (loadedStops.isNotEmpty()) {
                idaStopList = loadedStops
            }
        } else {
            idaPoints = route?.waypoints ?: emptyList()
            idaStopList = emptyList()
        }

        val retId = route?.returnRouteId
        if (retId != null) {
            val retGeom = RouteGeometryStore.geometryFor(retId)
            if (!retGeom.isNullOrEmpty()) {
                returnPoints = retGeom
                matchedReturnPoints = retGeom
            }

            val loadedReturnStops = RouteStopsStore.stopsFor(retId)
            if (loadedReturnStops.isNotEmpty()) {
                returnStopList = loadedReturnStops
            }
        } else {
            returnPoints = emptyList()
            returnStopList = emptyList()
        }

        // Persiste en cache identificado por route?.id para que la próxima visita
        // a la misma ruta muestre la geometría al instante.
        SelectedRouteHolder.cacheGeometry(route?.id, idaPoints, returnPoints)
        
        // Terminar carga - la geometría ya está lista (o confirmada como no disponible)
        isLoadingGeometry = false
    }

    // Trazado activo para navegación según el sentido seleccionado (Ida o Retorno)
    val activePoints = remember(selectedDirection, matchedIdaPoints, matchedReturnPoints) {
        if (selectedDirection == RouteDirection.RETORNO && matchedReturnPoints.isNotEmpty()) {
            matchedReturnPoints
        } else {
            matchedIdaPoints
        }
    }
    val activeStops = remember(selectedDirection, idaStopList, returnStopList) {
        val list = if (selectedDirection == RouteDirection.RETORNO && returnStopList.isNotEmpty()) returnStopList else idaStopList
        list.map { Pair(it.latitude, it.longitude) }
    }
    val activeStopNames = remember(selectedDirection, idaStopList, returnStopList) {
        val list = if (selectedDirection == RouteDirection.RETORNO && returnStopList.isNotEmpty()) returnStopList else idaStopList
        list.map { it.name }
    }

    fun getStartName(): String {
        return if (selectedDirection == RouteDirection.IDA) {
            val raw = route?.name ?: "Punto de partida"
            val parts = raw.split("⇄", "→")
            parts.firstOrNull()?.replace(Regex("^[^·]*·"), "")?.trim()?.ifBlank { null }
                ?: "Inicio de ruta"
        } else {
            val raw = route?.returnName ?: route?.name ?: "Retorno"
            val parts = raw.split("→", "⇄")
            parts.firstOrNull()?.trim()?.ifBlank { null }
                ?: "Terminal de retorno"
        }
    }

    // ── Navegación (estilo inDrive y Gowayki GO) ───────────────────────────
    val navigationPath = remember(activePoints) { NavigationPath.build(activePoints) }
    val locationSource = remember { createDeviceLocationSource() }

    var navigating by remember { mutableStateOf(false) }
    var enteredRoute by remember(route?.id) { mutableStateOf(false) }
    var controlPointNotified by remember { mutableStateOf(false) }
    var centerOnUserTrigger by remember { mutableStateOf(0) }
    // Paradero tocado en el mapa (se muestra su nombre y su orden en la ruta).
    var tappedStopIndex by remember(route?.id, selectedDirection) { mutableStateOf<Int?>(null) }
    var userLocation by remember { mutableStateOf<UserLocation?>(null) }
    var navInstruction by remember { mutableStateOf<NavInstruction?>(null) }
    // true mientras se espera el primer fix GPS tras presionar "Entrar en ruta"
    var waitingForGps by remember { mutableStateOf(false) }

    // Captura la ubicación actual aunque no se esté navegando,
    // para poder validar si el usuario está en el inicio de la ruta.
    // Se inicializa con la última ubicación conocida del sistema para
    // que el valor nunca sea null cuando el GPS ya estuvo activo antes.
    LaunchedEffect(Unit) {
        // Precarga rápida: última posición conocida del sistema operativo
        if (userLocation == null) {
            com.gowayki.nesh.app.common.util.lastKnownLocation()?.let { (lat, lon) ->
                userLocation = UserLocation(lat, lon, null, null, null)
            }
        }
        // Actualizaciones continuas del GPS
        locationSource.locations().collect { location ->
            userLocation = location
        }
    }

    LaunchedEffect(navigating) {
        com.gowayki.nesh.core.AppSettings.isOnRoute.value = navigating
        controlPointNotified = false
    }

    // Al cambiar de ruta se detiene el recorrido anterior.
    LaunchedEffect(route?.id) {
        navigating = false
        enteredRoute = false
    }

    // Mientras navega, escucha el GPS real y recalcula la instrucción.
    LaunchedEffect(navigating, navigationPath) {
        if (!navigating) {
            userLocation = null
            navInstruction = null
            return@LaunchedEffect
        }
        locationSource.locations().collect { location ->
            userLocation = location
            navInstruction = navigationPath.instruction(
                lat = location.lat,
                lon = location.lon,
                deviceBearingDeg = location.bearingDeg,
            )
            // Solo notificar cuando realmente se detecta posición sobre el trazo.
            val instruction = navInstruction
            val onRoute = instruction != null && instruction.maneuver != com.gowayki.nesh.app.features.map.domain.NavManeuver.OFF_ROUTE
            if (!controlPointNotified &&
                com.gowayki.nesh.core.AppSettings.controlPointAlertsEnabled.value &&
                com.gowayki.nesh.core.AppSettings.isOnRoute.value &&
                onRoute
            ) {
                controlPointNotified = true
                val dist = instruction?.distanceMeters?.toInt() ?: 0
                com.gowayki.nesh.app.common.ui.shared.ToastManager.show("Próximo punto de control a $dist m")
            }
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = true, onBack = onBack)

    val mains = remember { ArequipaRoutes.all.filter { !it.isSub && it.id.startsWith("C") } }
    val subsByLine = remember { ArequipaRoutes.all.filter { it.isSub }.groupBy { it.parent } }
    var expandedLines by remember {
        mutableStateOf(
            buildSet {
                route?.parent?.let { add(it) }
                route?.takeIf { !it.isSub && it.id.startsWith("C") }?.let { add(it.id) }
            }
        )
    }

        Box(Modifier.fillMaxSize().background(AppColors.cream)) {
            // Pantalla de carga mientras se carga la geometría de la ruta
            if (isLoadingGeometry) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = AppColors.primary,
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = "Cargando trazado de ruta",
                            style = AppTextStyles.waykiLabelMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = AppColors.ink
                        )
                        Text(
                            text = route?.name ?: "Ruta seleccionada",
                            style = AppTextStyles.waykiMini.copy(fontSize = 13.sp),
                            color = AppColors.slate
                        )
                    }
                }
            } else {
                // Contenido del mapa - solo se muestra cuando la geometría está lista
            var mapReady by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { kotlinx.coroutines.delay(300); mapReady = true }
            val mapAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (mapReady) 1f else 0f,
                animationSpec = androidx.compose.animation.core.tween(600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                label = "mapFade"
            )
            val overlayAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (mapReady) 0f else 1f,
                animationSpec = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                label = "overlayFade"
            )
            RouteMap(
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = mapAlpha },
                routePoints = idaPoints,
                returnRoutePoints = returnPoints,
                userLocation = userLocation,
                navigating = navigating,
                cameraBearingDeg = navInstruction?.bearingDeg,
                centerOnUserTrigger = centerOnUserTrigger,
                routeColorHex = routeColorHex,
                returnRouteColorHex = returnRouteColorHex,
                stops = activeStops,
                stopNames = activeStopNames,
                onStopClick = { idx -> tappedStopIndex = idx },
                onMatchedPointsChanged = { ida, ret ->
                    if (ida.isNotEmpty()) matchedIdaPoints = ida
                    if (ret.isNotEmpty()) matchedReturnPoints = ret
                },
            )

            // Pantalla de carga mientras el mapa se inicializa
            if (overlayAlpha > 0.01f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = overlayAlpha }
                        .background(AppColors.cream),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = AppColors.primary,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Cargando mapa...",
                            style = AppTextStyles.waykiMini.copy(fontSize = 13.sp),
                            color = AppColors.slate
                        )
                    }
                }
            }

            // Barra superior flotante (Material 3, elevada)
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(AppSpacing.pageHorizontal, AppSpacing.sm)
                    .shadow(10.dp, RoundedCornerShape(26.dp), spotColor = AppColors.ink.copy(alpha = 0.16f))
                    .clip(RoundedCornerShape(26.dp))
                    .background(AppColors.cream.copy(alpha = 0.97f))
                    .border(1.dp, AppColors.outline.copy(alpha = 0.25f), RoundedCornerShape(26.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(AppColors.ink)
                            .clickable {
    com.gowayki.nesh.app.common.ui.shared.ToastManager.show("Escoge tu ruta actual de micro en Ajustes")
    onSettingsClick()
},
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(FeatherIcons.Sliders, "Ajustes", tint = AppColors.cream, modifier = Modifier.size(20.dp))
                    }

                    WaykiBackButton(onClick = onBack)

                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = route?.name ?: "Mapa de rutas",
                            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.ink,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (route != null) {
                                val sent = if (selectedDirection == RouteDirection.IDA) "IDA" else "VUELTA"
                                "$sent · ${route.operator}"
                            } else {
                                "Arequipa · SmartBus"
                            },
                            style = AppTextStyles.waykiMini.copy(fontSize = 10.sp),
                            color = AppColors.slate,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    if (route != null) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(route.color.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(SolidGroup.Bus, null, tint = AppColors.ink, modifier = Modifier.size(19.dp))
                        }
                    }
                }
            }

            // Leyenda de sentidos (solo cuando el mapa está libre)
            if (route != null && !navigating) {
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(top = 84.dp, start = AppSpacing.pageHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendChip("IDA", Color(0xFF2196F3))
                    LegendChip("VUELTA", Color(0xFFE91E63))
                }
            }

            // Instrucción de navegación (debajo de la barra superior).
            if (navigating) {
                NavigationBanner(
                    instruction = navInstruction,
                    hasLocation = userLocation != null,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(
                            top = 80.dp,
                            start = AppSpacing.pageHorizontal,
                            end = AppSpacing.pageHorizontal,
                        ),
                )
            }

            // Panel inferior: botón de ubicación + tarjeta Material 3
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 100.dp)
                    .padding(horizontal = AppSpacing.pageHorizontal, vertical = AppSpacing.sm)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Botón flotante: mi ubicación actual
                    Box(
                        Modifier
                            .size(56.dp)
                            .shadow(10.dp, CircleShape, spotColor = AppColors.ink.copy(alpha = 0.22f))
                            .clip(CircleShape)
                            .background(AppColors.primary)
                            .border(2.dp, AppColors.cream, CircleShape)
                            .clickable {
                                val loc = userLocation
                                if (loc == null) {
                                    com.gowayki.nesh.app.common.ui.shared.ToastManager.show("Buscando tu ubicación GPS...")
                                    scope.launch {
                                        val fix = withTimeoutOrNull(8_000L) {
                                            locationSource.locations().first()
                                        }
                                        if (fix != null) {
                                            userLocation = fix
                                        } else {
                                            com.gowayki.nesh.app.common.ui.shared.ToastManager.show("No se pudo obtener señal GPS. Verifica que el GPS esté activo.")
                                        }
                                    }
                                } else {
                                    val start = activePoints.firstOrNull()
                                    if (start == null) {
                                        com.gowayki.nesh.app.common.ui.shared.ToastManager.show("Selecciona una ruta")
                                    } else {
                                        val dist = haversineMeters(loc.lat, loc.lon, start.first, start.second)
                                        com.gowayki.nesh.app.common.ui.shared.ToastManager.show(
                                            "Punto de partida (${getStartName()}): a ${formatDistance(dist)}"
                                        )
                                    }
                                }
                                centerOnUserTrigger++
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            FeatherIcons.Navigation,
                            contentDescription = "Mi ubicación",
                            tint = AppColors.cream,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Tarjeta inferior
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = AppColors.ink.copy(alpha = 0.22f))
                            .clip(RoundedCornerShape(28.dp))
                            .background(AppColors.cream.copy(alpha = 0.98f))
                            .border(1.dp, AppColors.outline.copy(alpha = 0.22f), RoundedCornerShape(28.dp))
                            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
                    ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Asa tipo bottom sheet
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(width = 38.dp, height = 4.dp)
                                .background(AppColors.outline.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                        )
                    }

                    // Ficha del paradero tocado en el mapa (nombre + orden)
                    val tapped = tappedStopIndex?.takeIf { it in activeStopNames.indices }
                    AnimatedVisibility(
                        visible = tapped != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val spotColor = if (selectedDirection == RouteDirection.IDA) Color(0xFF2196F3) else Color(0xFFE91E63)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(spotColor.copy(alpha = 0.10f))
                                .border(1.dp, spotColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(spotColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${(tapped ?: 0) + 1}",
                                    style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                    color = Color.White
                                )
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = tapped?.let { activeStopNames.getOrNull(it) } ?: "",
                                    style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                    color = AppColors.ink,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Paradero ${(tapped ?: 0) + 1} de ${activeStopNames.size} · " +
                                        if (selectedDirection == RouteDirection.IDA) "IDA" else "VUELTA",
                                    style = AppTextStyles.waykiMini.copy(fontSize = 10.sp),
                                    color = AppColors.slate
                                )
                            }
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { tappedStopIndex = null },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(FeatherIcons.X, "Cerrar", tint = AppColors.slate, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // Otros conductores de la misma ruta que usan la app: adelante / atrás
                    if (route != null) {
                        val others by com.gowayki.nesh.app.features.map.data.ActiveDriversStore.drivers.collectAsState()
                        // Registra la posición del conductor actual para que otros lo vean.
                        androidx.compose.runtime.LaunchedEffect(userLocation, route.id) {
                            userLocation?.let { com.gowayki.nesh.app.features.map.data.ActiveDriversStore.updateSelf(route.id, it) }
                        }
                        val sameRouteOthers = others.filter { !it.isSelf && it.routeId == route.id }
                        val loc = userLocation
                        val userProgress = if (loc != null && activePoints.size >= 2) {
                            projectProgressMeters(activePoints, loc.lat, loc.lon)
                        } else 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.softSage),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(SolidGroup.Bus, null, tint = AppColors.ink, modifier = Modifier.size(13.dp))
                            }
                            Text(
                                text = "Conductores en tu línea",
                                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                                color = AppColors.ink,
                            )
                            Spacer(Modifier.weight(1f))
                            if (sameRouteOthers.isEmpty()) {
                                MiniChip("Sin otros conductores")
                            } else {
                                sameRouteOthers.forEach { driver ->
                                    val otherProgress = projectProgressMeters(activePoints, driver.lat, driver.lon)
                                    val ahead = otherProgress > userProgress
                                    val diffMeters = kotlin.math.abs(otherProgress - userProgress)
                                    val minutes = (diffMeters / 500.0).toInt().coerceAtLeast(1)
                                    MiniChip("${driver.id} · $minutes min ${if (ahead) "adelante" else "atrás"}")
                                }
                            }
                        }
                    }

                    when {
                        navigating -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                            ) {
                                Box(
                                    Modifier.size(46.dp).clip(CircleShape).background(AppColors.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        FeatherIcons.Navigation,
                                        contentDescription = null,
                                        tint = AppColors.cream,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = route?.name ?: "Recorrido",
                                        style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = AppColors.ink,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = navSubtitle(userLocation, navInstruction),
                                        style = AppTextStyles.waykiMini,
                                        color = AppColors.slate
                                    )
                                }
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(AppColors.error.copy(alpha = 0.12f))
                                        .clickable { navigating = false }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Finalizar",
                                        style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                                        color = AppColors.error
                                    )
                                }
                            }
                        }
                        route != null -> {
                            if (!enteredRoute) {
                                val locNow = userLocation
                                val startPoint = activePoints.firstOrNull()
                                val distToStart = if (locNow != null && startPoint != null) {
                                    haversineMeters(locNow.lat, locNow.lon, startPoint.first, startPoint.second)
                                } else null

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                                    ) {
                                        BusBadge(color = route.color, size = 46.dp)
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = route.name,
                                                style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = AppColors.ink,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = route.operator,
                                                style = AppTextStyles.waykiMini,
                                                color = AppColors.slate
                                            )
                                        }
                                        Text(
                                            text = "${activeStops.size} paraderos",
                                            style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                            color = AppColors.ink,
                                            modifier = Modifier
                                                .background(AppColors.softSage, RoundedCornerShape(20.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        DirectionPill(
                                            text = "IDA",
                                            tint = Color(0xFF2196F3),
                                            selected = selectedDirection == RouteDirection.IDA,
                                            onClick = {
                                                selectedDirection = RouteDirection.IDA
                                                SelectedRouteHolder.setDirection(RouteDirection.IDA)
                                            }
                                        )
                                        if (returnPoints.isNotEmpty() || route.returnRouteId != null) {
                                            DirectionPill(
                                                text = "VUELTA",
                                                tint = Color(0xFFE91E63),
                                                selected = selectedDirection == RouteDirection.RETORNO,
                                                onClick = {
                                                    selectedDirection = RouteDirection.RETORNO
                                                    SelectedRouteHolder.setDirection(RouteDirection.RETORNO)
                                                }
                                            )
                                        }
                                        Spacer(Modifier.weight(1f))
                                        if (distToStart != null) {
                                            Text(
                                                text = "${formatDistance(distToStart)} al inicio",
                                                style = AppTextStyles.waykiMini.copy(fontSize = 10.sp),
                                                color = AppColors.slate
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(if (waitingForGps) AppColors.mauve.copy(alpha = 0.5f) else AppColors.primary)
                                                .clickable(enabled = !waitingForGps) {
                                                    centerOnUserTrigger++
                                                    val currentLoc = userLocation
                                                    val startPoint = activePoints.firstOrNull()
                                                    val startPlace = getStartName()

                                                    fun processLocation(loc: UserLocation) {
                                                        enteredRoute = true
                                                        if (startPoint != null) {
                                                            val dist = haversineMeters(loc.lat, loc.lon, startPoint.first, startPoint.second)
                                                            if (dist <= 80.0) {
                                                                com.gowayki.nesh.app.common.ui.shared.ToastManager.show("¡Estás en $startPlace! Iniciando recorrido.")
                                                                navigating = true
                                                            } else {
                                                                val walkMin = (dist / 1.3 / 60.0).toInt().coerceAtLeast(1)
                                                                val distStr = formatDistance(dist)
                                                                val msg = if (dist < 4000.0) {
                                                                    "Dirígete al punto de partida: $startPlace (a $distStr · ~$walkMin min a pie)"
                                                                } else {
                                                                    "Dirígete al punto de partida: $startPlace (a $distStr)"
                                                                }
                                                                com.gowayki.nesh.app.common.ui.shared.ToastManager.show(msg)
                                                            }
                                                        } else {
                                                            com.gowayki.nesh.app.common.ui.shared.ToastManager.show("Iniciando ruta...")
                                                            navigating = true
                                                        }
                                                    }

                                                    if (currentLoc != null) {
                                                        processLocation(currentLoc)
                                                    } else {
                                                        waitingForGps = true
                                                        scope.launch {
                                                            val fix = withTimeoutOrNull(8_000L) {
                                                                locationSource.locations().first()
                                                            }
                                                            waitingForGps = false
                                                            if (fix == null) {
                                                                com.gowayki.nesh.app.common.ui.shared.ToastManager.show("No se pudo obtener señal GPS. Verifica que el GPS esté activo y prueba al aire libre.")
                                                            } else {
                                                                userLocation = fix
                                                                processLocation(fix)
                                                            }
                                                        }
                                                    }
                                                }
                                                .padding(vertical = 13.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    FeatherIcons.Navigation,
                                                    contentDescription = null,
                                                    tint = AppColors.onPrimary.copy(alpha = if (waitingForGps) 0.55f else 1f),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text(
                                                    text = if (waitingForGps) "Buscando GPS..." else "Entrar en ruta",
                                                    style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                                                    color = AppColors.onPrimary.copy(alpha = if (waitingForGps) 0.55f else 1f)
                                                )
                                            }
                                        }

                                        Box(
                                            Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(AppColors.surfaceVariant)
                                                .border(1.dp, AppColors.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                                .clickable { onRecorridoClick() }
                                                .padding(horizontal = 14.dp, vertical = 13.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Ver recorrido",
                                                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                                                color = AppColors.ink
                                            )
                                        }
                                    }
                                }
                            } else {
                                val loc = userLocation ?: com.gowayki.nesh.app.common.util.lastKnownLocation()?.let { com.gowayki.nesh.app.features.map.domain.model.UserLocation(it.first, it.second, null, null, null) }
                                val startPoint = activePoints.firstOrNull()
                                val dist = if (loc != null && startPoint != null) haversineMeters(loc.lat, loc.lon, startPoint.first, startPoint.second) else null
                                val distStr = dist?.let { formatDistance(it) }
                                val walkMin = if (dist != null && dist < 4000.0) (dist / 1.3 / 60.0).toInt().coerceAtLeast(1) else null
                                val startPlace = getStartName()

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                                    ) {
                                        Box(
                                            Modifier.size(44.dp).clip(CircleShape).background(AppColors.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                FeatherIcons.Navigation,
                                                contentDescription = null,
                                                tint = AppColors.cream,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "Dirígete al punto de partida",
                                                style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = AppColors.ink
                                            )
                                            Text(
                                                text = if (distStr != null) {
                                                    if (walkMin != null) "$startPlace · a $distStr (~$walkMin min a pie)" else "$startPlace · a $distStr"
                                                } else {
                                                    startPlace
                                                },
                                                style = AppTextStyles.waykiMini,
                                                color = AppColors.slate
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(AppColors.primary)
                                                .clickable { navigating = true }
                                                .padding(vertical = 13.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Iniciar navegación",
                                                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                                                color = AppColors.onPrimary
                                            )
                                        }
                                        Box(
                                            Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(AppColors.surfaceVariant)
                                                .border(1.dp, AppColors.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                                .clickable { enteredRoute = false; navigating = false }
                                                .padding(horizontal = 16.dp, vertical = 13.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Salir",
                                                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                                                color = AppColors.ink
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                                ) {
                                    Box(
                                        Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(15.dp))
                                            .background(AppColors.softSage),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(FeatherIcons.Sliders, null, tint = AppColors.ink, modifier = Modifier.size(22.dp))
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Elige tu línea",
                                            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = AppColors.ink
                                        )
                                        Text(
                                            text = "Abre el menú y toca una ruta SIT para ver su trazado.",
                                            style = AppTextStyles.waykiMini,
                                            color = AppColors.slate
                                        )
                                    }
                                }
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(AppColors.primary)
                                        .clickable {
    com.gowayki.nesh.app.common.ui.shared.ToastManager.show("Escoge tu ruta actual de micro en Ajustes")
    onSettingsClick()
}
                                        .padding(vertical = 13.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Ver líneas",
                                        style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                                        color = AppColors.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
                    }
                }
            }
            
            } // Cierre del else - contenido del mapa

            // Barra de navegación inferior, anclada al fondo
            WaykiBottomNav(
                selectedTab = 3, // "Mapa" activo
                onTabSelected = { tab ->
                    when (tab) {
                        0 -> onHomeClick()
                        1 -> onCommunicationClick()
                        2 -> onRecorridoClick()
                        3 -> onMapaClick()
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
}


/**
 * Flota estimada que viene detrás del usuario para una línea.
 *
 * No hay backend de flota todavía, así que se deriva de forma determinista del
 * id de la ruta: el mismo recorrido muestra siempre los mismos buses y tiempos
 * (no cambia en cada recomposición ni entre IDA y VUELTA de la misma línea).
 */
/** Bus de la flota estimada con su posición relativa al usuario. */
private data class FleetBus(val code: String, val etaMin: Int, val ahead: Boolean)


/** Longitud total del trazado en metros. */
private fun routeTotalMeters(points: List<Pair<Double, Double>>): Double {
    var total = 0.0
    for (i in 0 until points.size - 1) total += haversineMeters(points[i].first, points[i].second, points[i + 1].first, points[i + 1].second)
    return total
}

/** Proyección del punto (lat,lon) sobre la polilínea: metros acumulados hasta la proyección. */
private fun projectProgressMeters(points: List<Pair<Double, Double>>, lat: Double, lon: Double): Double {
    var bestDistance = Double.MAX_VALUE
    var bestSegment = 0
    var bestT = 0.0
    for (i in 0 until points.size - 1) {
        val (distance, t) = projectOnSegmentMeters(points[i], points[i + 1], lat, lon)
        if (distance < bestDistance) {
            bestDistance = distance
            bestSegment = i
            bestT = t
        }
    }
    var cumulative = 0.0
    for (i in 0 until bestSegment) {
        cumulative += haversineMeters(points[i].first, points[i].second, points[i + 1].first, points[i + 1].second)
    }
    val segLen = haversineMeters(points[bestSegment].first, points[bestSegment].second, points[bestSegment + 1].first, points[bestSegment + 1].second)
    return cumulative + bestT * segLen
}

private fun projectOnSegmentMeters(
    a: Pair<Double, Double>,
    b: Pair<Double, Double>,
    lat: Double,
    lon: Double,
): Pair<Double, Double> {
    val midLat = (a.first + b.first) / 2.0 * kotlin.math.PI / 180.0
    val kx = 111_320.0 * kotlin.math.cos(midLat)
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
    return kotlin.math.sqrt(px * px + py * py) to clamped
}

/** Chip de sentido con el color real de la línea (IDA azul / VUELTA rosada). */
@Composable
private fun DirectionPill(
    text: String,
    tint: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) tint.copy(alpha = 0.16f) else AppColors.surfaceVariant.copy(alpha = 0.7f))
            .border(
                1.dp,
                if (selected) tint.copy(alpha = 0.55f) else AppColors.outline.copy(alpha = 0.2f),
                RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(8.dp).background(tint, CircleShape))
        Text(
            text = text,
            style = AppTextStyles.waykiMini.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 10.sp
            ),
            color = if (selected) AppColors.ink else AppColors.slate
        )
    }
}

/** Leyenda flotante sobre el mapa. */
@Composable
private fun LegendChip(text: String, tint: Color) {
    Row(
        Modifier
            .shadow(4.dp, RoundedCornerShape(50), spotColor = AppColors.ink.copy(alpha = 0.12f))
            .clip(RoundedCornerShape(50))
            .background(AppColors.cream.copy(alpha = 0.96f))
            .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(8.dp).background(tint, CircleShape))
        Text(
            text = text,
            style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = AppColors.ink
        )
    }
}

/** Chip compacto para tiempos de buses. */
@Composable
private fun MiniChip(text: String) {
    Box(
        Modifier
            .background(AppColors.softSage, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = AppTextStyles.waykiMini.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
            color = AppColors.ink
        )
    }
}

private fun formatDistance(meters: Double): String =
    if (meters >= 1000.0) "${(meters / 100.0).toInt() / 10.0} km" else "${meters.toInt()} m"

/** Texto secundario de la tarjeta inferior durante la navegación. */
private fun navSubtitle(location: UserLocation?, instruction: NavInstruction?): String = when {
    location == null -> "Esperando señal GPS…"
    instruction == null -> "Calculando recorrido…"
    instruction.maneuver == NavManeuver.ARRIVE -> "Buen viaje"
    instruction.maneuver == NavManeuver.OFF_ROUTE -> "Acércate al trazado de la ruta"
    else -> "Faltan ${NavigationPath.formatDistance(instruction.remainingMeters)}"
}

/** Aviso grande de la próxima maniobra, al estilo inDrive. */
@Composable
private fun NavigationBanner(
    instruction: NavInstruction?,
    hasLocation: Boolean,
    modifier: Modifier = Modifier,
) {
    val icon: ImageVector
    val title: String
    val subtitle: String?
    when {
        !hasLocation -> {
            icon = FeatherIcons.Navigation
            title = "Buscando señal GPS…"
            subtitle = "Activa la ubicación del teléfono"
        }
        instruction == null -> {
            icon = FeatherIcons.Navigation
            title = "Calculando recorrido…"
            subtitle = null
        }
        instruction.maneuver == NavManeuver.ARRIVE -> {
            icon = FeatherIcons.Flag
            title = "Llegaste a tu destino"
            subtitle = "Buen viaje"
        }
        instruction.maneuver == NavManeuver.OFF_ROUTE -> {
            icon = FeatherIcons.Navigation
            title = "Vuelve a la ruta"
            subtitle = "Acércate al trazado de la combi"
        }
        instruction.maneuver == NavManeuver.STRAIGHT -> {
            icon = FeatherIcons.ArrowUp
            title = "Continúa por la ruta"
            subtitle = "Faltan ${NavigationPath.formatDistance(instruction.remainingMeters)}"
        }
        else -> {
            val right = instruction.maneuver == NavManeuver.RIGHT
            icon = if (right) FeatherIcons.CornerUpRight else FeatherIcons.CornerUpLeft
            title = if (right) "Gira a la derecha" else "Gira a la izquierda"
            subtitle = if (instruction.distanceMeters <= 15.0) {
                "Ahora"
            } else {
                "en ${NavigationPath.formatDistance(instruction.distanceMeters)}"
            }
        }
    }

    Row(
        modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = AppColors.ink.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(22.dp))
            .background(AppColors.primary)
            .border(1.dp, AppColors.cream.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(AppColors.mauve),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AppColors.ink,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTextStyles.waykiLabelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = AppColors.cream
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = AppTextStyles.waykiMini,
                    color = AppColors.cream.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/** Imagen/insignia del bus con el color de la línea. */
@Composable
private fun BusBadge(
    color: Color,
    size: Dp,
    icon: androidx.compose.ui.graphics.vector.ImageVector = SolidGroup.Bus,
) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(color.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = AppColors.ink,
            modifier = Modifier.size(size * 0.58f)
        )
    }
}

/** Cabecera desplegable de una línea (C2..C11). */
@Composable
private fun LineHeader(
    route: com.gowayki.nesh.app.features.map.domain.model.CombiRoute,
    subCount: Int,
    expanded: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) route.color.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BusBadge(color = route.color, size = 38.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = route.name,
                style = AppTextStyles.waykiLabelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = AppColors.ink
            )
            Text(
                text = "$subCount sub-rutas · ${route.operator}",
                style = AppTextStyles.waykiMini.copy(fontSize = 10.sp),
                color = AppColors.slate
            )
        }
        Icon(
            SolidGroup.ChevronDown,
            contentDescription = if (expanded) "Contraer" else "Desplegar",
            tint = AppColors.slate,
            modifier = Modifier.size(16.dp).rotate(rotation)
        )
    }
}

/** Fila de una sub-ruta dentro de su línea. */
@Composable
private fun SubRouteRow(
    route: com.gowayki.nesh.app.features.map.domain.model.CombiRoute,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) route.color.copy(alpha = 0.22f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        BusBadge(color = route.color, size = 26.dp, icon = SolidGroup.ShuttleVan)
        Text(
            text = route.name,
            style = AppTextStyles.waykiMini.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
            ),
            color = AppColors.ink,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Box(Modifier.size(7.dp).background(route.color, CircleShape))
        }
    }
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
