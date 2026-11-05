package com.gowayki.nesh.app.features.route_detail.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.ui.shared.BackHandler
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.app.common.ui.shared.WaykiBottomNav
import com.gowayki.nesh.app.features.home.presentation.screens.WaykiSidebarContent
import com.gowayki.nesh.app.features.map.data.RouteStopsStore
import com.gowayki.nesh.app.features.map.data.SitRouteStop
import com.gowayki.nesh.app.features.map.domain.model.ArequipaRoutes
import com.gowayki.nesh.app.features.map.presentation.SelectedRouteHolder
import com.gowayki.nesh.app.features.map.presentation.RouteDirection
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bell
import compose.icons.feathericons.Clock
import compose.icons.feathericons.GitCommit
import compose.icons.feathericons.Map
import compose.icons.feathericons.MapPin
import compose.icons.feathericons.Menu
import compose.icons.feathericons.Navigation
import kotlinx.coroutines.launch

/**
 * Colores de la paleta de la app para distinguir el sentido del recorrido
 * en "Tu recorrido" (el mapa mantiene los colores de ruta #2196F3 / #E91E63).
 */
private val IdaColor = AppColors.primary
private val VueltaColor = AppColors.tertiary

@Composable
fun RouteDetailScreen(
    onBack: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onTabSelected: (Int) -> Unit = {},
    onSosClick: () -> Unit = {},
    onInicioClick: () -> Unit = {},
    onCommunicationClick: () -> Unit = {},
    onRecorridoClick: () -> Unit = {},
    onMapaClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = true, onBack = onBack)

    val selectedRoute by SelectedRouteHolder.selected.collectAsState()
    var idaStops by remember { mutableStateOf<List<SitRouteStop>>(emptyList()) }
    var returnStops by remember { mutableStateOf<List<SitRouteStop>>(emptyList()) }

    LaunchedEffect(selectedRoute?.id, selectedRoute?.routeId, selectedRoute?.returnRouteId) {
        var effective = selectedRoute
        var ida = effective?.routeId?.let { RouteStopsStore.stopsFor(it) }.orEmpty()

        // Si la ruta seleccionada no tiene paraderos cargados, usa la primera
        // sub-ruta que sí tenga datos para que la pantalla nunca quede vacía.
        if (ida.isEmpty()) {
            val fallback = ArequipaRoutes.all.firstOrNull { route ->
                route.isSub && route.routeId != null &&
                    RouteStopsStore.stopsFor(route.routeId!!).isNotEmpty()
            }
            if (fallback != null) {
                SelectedRouteHolder.select(fallback)
                effective = fallback
                ida = fallback.routeId?.let { RouteStopsStore.stopsFor(it) }.orEmpty()
            }
        }

        idaStops = ida
        returnStops = effective?.returnRouteId?.let { RouteStopsStore.stopsFor(it) }.orEmpty()
    }

    val sharedDirection = SelectedRouteHolder.direction.collectAsState()
    var direction by remember(selectedRoute?.id) { mutableStateOf(sharedDirection.value) }
    // Solo mostramos el selector de vuelta si hay paraderos de retorno cargados.
    val hasReturn = returnStops.isNotEmpty()
    val isIda = direction == RouteDirection.IDA || !hasReturn
    val directionColor = if (isIda) IdaColor else VueltaColor
    val stops = if (!isIda) returnStops else idaStops

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                drawerContainerColor = AppColors.cream,
                modifier = Modifier.width(328.dp).fillMaxHeight()
            ) {
                WaykiSidebarContent(
                    selectedTab = 2,
                    onClose = { coroutineScope.launch { drawerState.close() } },
                    onSosClick = {
                        coroutineScope.launch { drawerState.close() }
                        onSosClick()
                    },
                    onInicioClick = {
                        coroutineScope.launch { drawerState.close() }
                        onInicioClick()
                    },
                    onCommunicationClick = {
                        coroutineScope.launch { drawerState.close() }
                        onCommunicationClick()
                    },
                    onRecorridoClick = {
                        coroutineScope.launch { drawerState.close() }
                        onRecorridoClick()
                    },
                    onMapaClick = {
                        coroutineScope.launch { drawerState.close() }
                        onMapaClick()
                    },
                    onSettingsClick = {
                        coroutineScope.launch { drawerState.close() }
                        onSettingsClick()
                    },
                    onSupportClick = {
                        coroutineScope.launch { drawerState.close() }
                        onSupportClick()
                    },
                    onLogoutClick = {
                        coroutineScope.launch { drawerState.close() }
                        onLogoutClick()
                    }
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.cream)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // ── Encabezado ────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.pageHorizontal, vertical = AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tu recorrido",
                            style = AppTextStyles.waykiHeader,
                            color = AppColors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Paraderos y puntos de control",
                            style = AppTextStyles.waykiMini.copy(fontSize = 12.sp),
                            color = AppColors.slate
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(AppColors.ink)
                            .clickable { coroutineScope.launch { drawerState.open() } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FeatherIcons.Menu,
                            contentDescription = "Menú",
                            tint = AppColors.cream,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    CircleIconButton(
                        icon = FeatherIcons.Map,
                        description = "Ver en el mapa",
                        onClick = onMapaClick
                    )
                    CircleIconButton(
                        icon = FeatherIcons.Bell,
                        description = "Notificaciones",
                        onClick = onNotificationsClick
                    )
                }

                // ── Contenido desplazable ─────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = AppSpacing.pageHorizontal)
                ) {
                    // Ficha de la ruta
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(10.dp, RoundedCornerShape(26.dp), spotColor = AppColors.ink.copy(alpha = 0.14f))
                            .clip(RoundedCornerShape(26.dp))
                            .background(AppColors.cream)
                            .border(1.dp, AppColors.outline.copy(alpha = 0.22f), RoundedCornerShape(26.dp))
                            .padding(AppSpacing.lg)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(directionColor.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = FeatherIcons.MapPin,
                                        contentDescription = null,
                                        tint = directionColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedRoute?.name ?: "Selecciona una ruta",
                                        style = AppTextStyles.waykiLabelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = AppColors.ink,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = selectedRoute?.operator ?: "Sistema Integrado de Transporte",
                                        style = AppTextStyles.waykiMini.copy(fontSize = 12.sp),
                                        color = AppColors.slate
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MetaChip(text = "${stops.size} paraderos", tint = AppColors.slate)
                                if (hasReturn) {
                                    MetaChip(
                                        text = if (isIda) "Ida" else "Vuelta",
                                        tint = directionColor
                                    )
                                }
                            }

                            // Selector de sentido (solo si hay retorno cargado)
                            if (hasReturn) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DirectionTab(
                                        text = "IDA",
                                        tint = IdaColor,
                                        selected = isIda,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            direction = RouteDirection.IDA
                                            SelectedRouteHolder.setDirection(RouteDirection.IDA)
                                        }
                                    )
                                    DirectionTab(
                                        text = "VUELTA",
                                        tint = VueltaColor,
                                        selected = !isIda,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            direction = RouteDirection.RETORNO
                                            SelectedRouteHolder.setDirection(RouteDirection.RETORNO)
                                        }
                                    )
                                }
                            }

                            if (selectedRoute != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(AppColors.primary)
                                        .clickable(onClick = onMapaClick)
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = FeatherIcons.Navigation,
                                            contentDescription = null,
                                            tint = AppColors.onPrimary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            text = "Ver en el mapa",
                                            style = AppTextStyles.waykiLabelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            ),
                                            color = AppColors.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(AppSpacing.xl))

                    // ── Lista de paraderos en orden ───────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PARADEROS",
                            style = AppTextStyles.waykiMini.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = AppColors.slate
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(directionColor, CircleShape)
                            )
                            Text(
                                text = if (isIda) "Recorrido de ida" else "Recorrido de vuelta",
                                style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                                color = AppColors.slate
                            )
                        }
                    }

                    Spacer(Modifier.height(AppSpacing.md))

                    if (stops.isEmpty()) {
                        EmptyStopsCard()
                    } else {
                        stops.forEachIndexed { index, stop ->
                            TimelineStop(
                                stop = stop,
                                number = index + 1,
                                isFirst = index == 0,
                                isLast = index == stops.lastIndex,
                                accent = directionColor
                            )
                        }
                    }

                    Spacer(Modifier.height(AppSpacing.xl))

                    Text(
                        text = if (stops.isEmpty()) {
                            "No hay paraderos registrados para esta ruta."
                        } else {
                            "Paraderos ordenados según el recorrido oficial de la ruta."
                        },
                        style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                        color = AppColors.slate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 130.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Barra de navegación inferior
            WaykiBottomNav(
                selectedTab = 2, // "Recorrido" activo
                onTabSelected = onTabSelected,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(AppColors.surfaceVariant.copy(alpha = 0.7f))
            .border(1.dp, AppColors.outline.copy(alpha = 0.2f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = AppColors.ink,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun MetaChip(text: String, tint: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.1f))
            .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.size(6.dp).background(tint, CircleShape))
        Text(
            text = text,
            style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = AppColors.ink
        )
    }
}

@Composable
private fun DirectionTab(
    text: String,
    tint: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) tint.copy(alpha = 0.14f) else AppColors.surfaceVariant.copy(alpha = 0.6f))
            .border(
                1.dp,
                if (selected) tint.copy(alpha = 0.5f) else AppColors.outline.copy(alpha = 0.18f),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(8.dp).background(tint, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = AppTextStyles.waykiMini.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = if (selected) AppColors.ink else AppColors.slate
        )
    }
}

/**
 * Paradero en formato línea de tiempo: riel vertical + nodo numerado + tarjeta.
 * El primer y último paradero se resaltan como inicio / fin del recorrido.
 */
@Composable
private fun TimelineStop(
    stop: SitRouteStop,
    number: Int,
    isFirst: Boolean,
    isLast: Boolean,
    accent: Color,
) {
    val isTerminal = isFirst || isLast
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Riel y nodo
        Box(
            modifier = Modifier
                .width(42.dp)
                .fillMaxHeight()
        ) {
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(accent.copy(alpha = 0.3f))
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(if (isTerminal) 30.dp else 26.dp)
                    .clip(CircleShape)
                    .background(if (isTerminal) accent else AppColors.surface)
                    .border(2.dp, accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isFirst) "A" else if (isLast) "B" else number.toString(),
                    style = AppTextStyles.waykiMini.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isTerminal) 12.sp else 10.sp
                    ),
                    color = if (isTerminal) Color.White else accent
                )
            }
        }

        // Tarjeta del paradero
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, bottom = 10.dp)
                .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = AppColors.ink.copy(alpha = 0.1f))
                .clip(RoundedCornerShape(20.dp))
                .background(AppColors.surface)
                .border(
                    1.dp,
                    if (isTerminal) accent.copy(alpha = 0.4f) else AppColors.outline.copy(alpha = 0.25f),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stop.name,
                            style = AppTextStyles.waykiLabelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = AppColors.ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!stop.district.isNullOrBlank()) {
                            Text(
                                text = stop.district,
                                style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                                color = AppColors.slate
                            )
                        }
                    }
                    if (isTerminal) {
                        Text(
                            text = if (isFirst) "INICIO" else "FIN",
                            style = AppTextStyles.waykiMini.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = accent,
                            modifier = Modifier
                                .background(accent.copy(alpha = 0.12f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.surfaceVariant.copy(alpha = 0.55f))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = FeatherIcons.GitCommit,
                        contentDescription = null,
                        tint = AppColors.slate,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${formatCoord(stop.latitude)}, ${formatCoord(stop.longitude)}",
                        style = AppTextStyles.waykiMini.copy(fontSize = 10.sp),
                        color = AppColors.slate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = if (isTerminal) FeatherIcons.Clock else FeatherIcons.MapPin,
                        contentDescription = null,
                        tint = accent.copy(alpha = 0.8f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStopsCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(AppColors.softSage)
            .border(1.dp, AppColors.outline.copy(alpha = 0.2f), RoundedCornerShape(22.dp))
            .padding(AppSpacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = FeatherIcons.MapPin,
                contentDescription = null,
                tint = AppColors.slate,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = "Esta ruta aún no tiene paraderos registrados",
                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                color = AppColors.ink,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "Prueba con otra línea desde el menú de rutas.",
                style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                color = AppColors.slate,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun formatCoord(value: Double): String {
    val rounded = (value * 100000.0).toLong() / 100000.0
    return rounded.toString()
}
