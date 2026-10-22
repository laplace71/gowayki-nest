package com.gowayki.nesh.app.features.map.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import com.gowayki.nesh.app.common.ui.shared.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.app.features.map.data.RouteGeometryStore
import com.gowayki.nesh.app.features.map.data.createDeviceLocationSource
import com.gowayki.nesh.app.features.map.domain.NavInstruction
import com.gowayki.nesh.app.features.map.domain.NavManeuver
import com.gowayki.nesh.app.features.map.domain.NavigationPath
import com.gowayki.nesh.app.features.map.domain.model.ArequipaRoutes
import com.gowayki.nesh.app.features.map.domain.model.UserLocation
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
import compose.icons.feathericons.X
import compose.icons.fontawesomeicons.SolidGroup
import compose.icons.fontawesomeicons.solid.Bus
import compose.icons.fontawesomeicons.solid.ChevronDown
import compose.icons.fontawesomeicons.solid.ShuttleVan

@Composable
fun MapScreen(
    onBack: () -> Unit = {},
) {
    BackHandler(true) { onBack() }
    val routeState = SelectedRouteHolder.selected.collectAsState()
    val route = routeState.value

    var routePoints by remember(route) {
        mutableStateOf(route?.waypoints ?: emptyList())
    }
    // Carga el trazado real empaquetado (sigue calles) cuando la ruta lo tiene.
    LaunchedEffect(route?.routeId) {
        val id = route?.routeId ?: return@LaunchedEffect
        val geometry = RouteGeometryStore.geometryFor(id)
        if (!geometry.isNullOrEmpty()) routePoints = geometry
    }

    // ── Navegación (estilo inDrive) ─────────────────────────────────────────
    val navigationPath = remember(routePoints) { NavigationPath.build(routePoints) }
    val locationSource = remember { createDeviceLocationSource() }
    var navigating by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<UserLocation?>(null) }
    var navInstruction by remember { mutableStateOf<NavInstruction?>(null) }

    // Al cambiar de ruta se detiene el recorrido anterior.
    LaunchedEffect(route?.id) {
        navigating = false
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
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                drawerContainerColor = AppColors.cream,
                modifier = Modifier.width(300.dp).fillMaxHeight()
            ) {
                Column(
                    Modifier.fillMaxSize().statusBarsPadding()
                        .padding(AppSpacing.pageHorizontal, AppSpacing.sm)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        BusBadge(color = AppColors.mauve, size = 48.dp)
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Rutas de combi",
                                style = AppTextStyles.waykiHeader,
                                color = AppColors.ink,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Arequipa · SmartBus",
                                style = AppTextStyles.waykiMini,
                                color = AppColors.slate
                            )
                        }
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(AppColors.mauve.copy(alpha = 0.5f))
                                .clickable { scope.launch { drawerState.close() } },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                FeatherIcons.X,
                                contentDescription = "Cerrar menú",
                                tint = AppColors.ink,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(AppSpacing.sm))
                    Text(
                        text = "Toca una línea para desplegar sus sub-rutas.",
                        style = AppTextStyles.waykiMini,
                        color = AppColors.slate
                    )
                    Spacer(Modifier.height(AppSpacing.md))
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        item {
                            Text(
                                text = "Líneas SIT (C2–C11)",
                                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                                color = AppColors.slate
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        mains.forEach { main ->
                            val subs = subsByLine[main.id].orEmpty()
                            item(key = main.id) {
                                LineHeader(
                                    route = main,
                                    subCount = subs.size,
                                    expanded = main.id in expandedLines,
                                    selected = false,
                                    onToggle = {
                                        // Solo despliega/contrae; no cambia la ruta activa.
                                        expandedLines = if (main.id in expandedLines) {
                                            expandedLines - main.id
                                        } else {
                                            expandedLines + main.id
                                        }
                                    },
                                )
                                AnimatedVisibility(
                                    visible = main.id in expandedLines,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut(),
                                ) {
                                    Column(Modifier.padding(start = 10.dp)) {
                                        subs.forEach { sub ->
                                            SubRouteRow(
                                                route = sub,
                                                selected = route?.id == sub.id,
                                                onClick = {
                                                    SelectedRouteHolder.select(sub)
                                                    scope.launch { drawerState.close() }
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(AppSpacing.sm))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppColors.mauve.copy(alpha = 0.55f))
                            .clickable { scope.launch { drawerState.close() } }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            FeatherIcons.X,
                            contentDescription = null,
                            tint = AppColors.ink,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Cerrar panel",
                            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.ink
                        )
                    }
                }
            }
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            RouteMap(
                routePoints = routePoints,
                userLocation = userLocation,
                navigating = navigating,
                cameraBearingDeg = navInstruction?.bearingDeg,
                modifier = Modifier.fillMaxSize(),
            )

            // Top bar flotante
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(AppSpacing.pageHorizontal, AppSpacing.sm)
                    .background(AppColors.cream.copy(alpha = 0.92f), RoundedCornerShape(24.dp))
                    .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                Box(
                    Modifier.size(36.dp).background(AppColors.mauve, CircleShape)
                        .clickable { scope.launch { drawerState.open() } },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(FeatherIcons.Menu, null, tint = AppColors.ink, modifier = Modifier.size(20.dp))
                }
                WaykiBackButton(onClick = onBack)
                Text(
                    text = route?.name ?: "Mapa",
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.ink,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (route != null) {
                    BusBadge(color = route.color, size = 28.dp)
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
                            top = 68.dp,
                            start = AppSpacing.pageHorizontal,
                            end = AppSpacing.pageHorizontal,
                        ),
                )
            }

            // Tarjeta inferior flotante
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(AppSpacing.pageHorizontal, AppSpacing.sm)
                    .background(AppColors.cream.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
                    .padding(AppSpacing.pageHorizontal, AppSpacing.sm)
            ) {
                when {
                    navigating -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            Icon(
                                FeatherIcons.Navigation,
                                contentDescription = null,
                                tint = AppColors.ink,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(Modifier.weight(1f)) {
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
                                    .background(AppColors.ink)
                                    .clickable { navigating = false }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Finalizar",
                                    style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                                    color = AppColors.cream
                                )
                            }
                        }
                    }
                    route != null -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            BusBadge(color = route.color, size = 44.dp)
                            Column(Modifier.weight(1f)) {
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
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AppColors.mauve)
                                    .clickable { navigating = true }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        FeatherIcons.Navigation,
                                        contentDescription = null,
                                        tint = AppColors.ink,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Navegar",
                                        style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                                        color = AppColors.ink
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        Text(
                            text = "Abre el menú (☰) y selecciona una ruta.",
                            style = AppTextStyles.waykiMini,
                            color = AppColors.slate
                        )
                    }
                }
            }

            // Cierra el panel al tocar fuera de él. El scrim de Material3 no
            // responde cuando gesturesEnabled = false.
            if (drawerState.isOpen) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { scope.launch { drawerState.close() } }
                )
            }
        }
    }
}

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
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.ink.copy(alpha = 0.94f))
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
                    color = AppColors.cream.copy(alpha = 0.75f)
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
