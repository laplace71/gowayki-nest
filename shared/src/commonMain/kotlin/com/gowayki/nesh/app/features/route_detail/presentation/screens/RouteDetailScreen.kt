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
import com.gowayki.nesh.app.common.ui.shared.WaykiSidebarContent
import com.gowayki.nesh.app.common.ui.shared.WaykiReveal
import com.gowayki.nesh.app.common.ui.shared.WaykiSkeleton
import com.gowayki.nesh.app.features.map.data.RouteCatalogStore
import com.gowayki.nesh.app.features.map.data.RouteStopsStore
import com.gowayki.nesh.app.features.map.data.SitRouteStop
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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity

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
    var loadingStops by remember { mutableStateOf(true) }
    var returnStops by remember { mutableStateOf<List<SitRouteStop>>(emptyList()) }
    // Ruta cuyos paraderos se están mostrando. Si hay fallback, debe ser la
    // misma que se muestra en la cabecera para no mezclar identidades.
    var effectiveRoute by remember { mutableStateOf(selectedRoute) }

    LaunchedEffect(selectedRoute?.id, selectedRoute?.routeId, selectedRoute?.returnRouteId) {
        loadingStops = true
        var effective = selectedRoute
        var ida = effective?.routeId?.let { RouteStopsStore.stopsFor(it) }.orEmpty()

        // Si la ruta seleccionada no tiene paraderos cargados, usa la primera
        // sub-ruta del catálogo (BD) que sí tenga datos para que la pantalla nunca quede vacía.
        if (ida.isEmpty()) {
            runCatching { RouteCatalogStore.refresh() }
            val candidates = RouteCatalogStore.subrutas().ifEmpty {
                SelectedRouteHolder.catalog.value
            }
            val fallback = candidates.firstOrNull { route ->
                route.isSub && route.routeId != null &&
                    RouteStopsStore.stopsFor(route.routeId!!).isNotEmpty()
            }
            if (fallback != null) {
                effective = fallback
                ida = fallback.routeId?.let { RouteStopsStore.stopsFor(it) }.orEmpty()
            }
        }

        idaStops = ida
        returnStops = effective?.returnRouteId?.let { RouteStopsStore.stopsFor(it) }.orEmpty()
        effectiveRoute = effective
        loadingStops = false
    }

    val sharedDirection = SelectedRouteHolder.direction.collectAsState()
    var direction by remember(selectedRoute?.id) { mutableStateOf(sharedDirection.value) }
    // Solo mostramos el selector de vuelta si hay paraderos de retorno cargados.
    val hasReturn = returnStops.isNotEmpty()
    val isIda = direction == RouteDirection.IDA || !hasReturn
    val directionColor = if (isIda) IdaColor else VueltaColor
    val stops = if (!isIda) returnStops else idaStops
    val sections = remember(stops) { buildStopSections(stops) }

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
                // LazyColumn: una ruta puede tener hasta 219 paraderos. Con
                // Column + verticalScroll se componían y medían TODOS de golpe
                // (más una sombra y un `IntrinsicSize.Min` por fila), y ese
                // trabajo en el primer frame era el tirón al entrar.
                // Ahora solo se componen las filas visibles y se animan escalonadamente.
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = AppSpacing.pageHorizontal,
                        end = AppSpacing.pageHorizontal,
                        bottom = 130.dp
                    )
                ) {
                    item(key = "ficha") {
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
                                Text(
                                    text = effectiveRoute?.name ?: "Selecciona una ruta",
                                    style = AppTextStyles.waykiTitle.copy(fontSize = 24.sp),
                                    color = directionColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = effectiveRoute?.routeId ?: "Ruta sin nombre",
                                    style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                                    color = AppColors.slate
                                )
                            }
                        }
                    }

                    // Secciones de paraderos por distrito
                    when {
                        loadingStops -> items(4) { SkeletonStopRow() }
                        stops.isEmpty() -> item(key = "vacio") { EmptyStopsCard() }
                        else -> {
                            sections.forEach { section ->
                                val district = section.district
                                if (district != null) {
                                    item(key = "zona-${section.startIndex}") {
                                        StopSectionHeader(
                                            title = district.districtDisplayName(),
                                            count = section.stops.size,
                                            accent = directionColor
                                        )
                                    }
                                }
                                itemsIndexed(
                                    items = section.stops,
                                    key = { i, stop -> "${stop.id}-${section.startIndex + i}" }
                                ) { i, stop ->
                                    val index = section.startIndex + i
                                    StopEntrance(index) {
                                        TimelineStop(
                                            stop = stop,
                                            number = index + 1,
                                            isFirst = index == 0,
                                            isLast = index == stops.lastIndex,
                                            accent = directionColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "pie") {
                        Spacer(Modifier.height(AppSpacing.xl))
                    }
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
private fun StopSectionHeader(title: String, count: Int, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(accent, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = AppColors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (count == 1) "1 paradero" else "$count paraderos",
            style = AppTextStyles.waykiMini.copy(fontSize = 10.sp),
            color = AppColors.slate
        )
    }
}

@Composable
private fun StopEntrance(index: Int, content: @Composable () -> Unit) {
    content()
}

@Composable
private fun SkeletonStopRow() {
    Row(modifier = Modifier.fillMaxWidth()) {
        WaykiSkeleton(Modifier.size(26.dp), shape = CircleShape)
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 10.dp)
        ) {
            WaykiSkeleton(Modifier.fillMaxWidth(0.5f).height(14.dp))
            Spacer(Modifier.height(8.dp))
            WaykiSkeleton(Modifier.fillMaxWidth(0.28f).height(10.dp))
            Spacer(Modifier.height(10.dp))
            WaykiSkeleton(
                Modifier.fillMaxWidth().height(76.dp),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

private data class StopSection(
    val district: String?,
    val startIndex: Int,
    val stops: List<SitRouteStop>
)

/**
 * Agrupa los paraderos por distrito conservando el orden oficial del recorrido.
 * Si la ruta no trae distritos (24 % de los datos), devuelve una sola sección
 * sin encabezado para no inventar zonas.
 */
private fun buildStopSections(stops: List<SitRouteStop>): List<StopSection> {
    if (stops.isEmpty()) return emptyList()
    val hasDistrict = stops.any { !it.district.isNullOrBlank() }
    if (!hasDistrict) return listOf(StopSection(null, 0, stops))

    val sections = mutableListOf<StopSection>()
    var start = 0
    var current: String? = null
    var bucket = mutableListOf<SitRouteStop>()

    fun flush() {
        if (bucket.isEmpty()) return
        sections += StopSection(current, start, bucket)
        start += bucket.size
        bucket = mutableListOf()
    }

    stops.forEach { stop ->
        val district = stop.district?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        if (district != null && district != current && bucket.isNotEmpty()) flush()
        if (district != null) current = district
        bucket += stop
    }
    flush()
    return sections
}

private fun String.districtDisplayName(): String {
    return lowercase().split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercaseChar() }
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
 * Paradero en formato línea de tiempo: riel + nodo + tarjeta.
 * Optimizado para scroll rápido: sin shadow por fila, sin IntrinsicSize.Min.
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
    val cardShape = RoundedCornerShape(20.dp)
    val district = stop.district?.takeIf { it.isNotBlank() }
    val coords = remember(stop.latitude, stop.longitude) {
        "${formatCoord(stop.latitude)}, ${formatCoord(stop.longitude)}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val x = 21.dp.toPx()
                val lineColor = accent.copy(alpha = 0.26f)
                val top = if (isFirst) 14.dp.toPx() + 15.dp.toPx() else 0f
                val bottom = if (isLast) 14.dp.toPx() + 15.dp.toPx() else size.height
                if (bottom > top) {
                    drawLine(lineColor, Offset(x, top), Offset(x, bottom), strokeWidth = 2.dp.toPx())
                }
            }
            .padding(bottom = 2.dp)
    ) {
        // Riel y nodo
        Box(
            modifier = Modifier.width(42.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .size(if (isTerminal) 30.dp else 26.dp)
                    .clip(CircleShape)
                    .background(if (isTerminal) accent else AppColors.cream)
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

        // Tarjeta del paradero (sin shadow para scroll fluido)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, bottom = 10.dp)
                .clip(cardShape)
                .background(AppColors.cream)
                .border(
                    1.dp,
                    if (isTerminal) accent.copy(alpha = 0.38f) else AppColors.outline.copy(alpha = 0.2f),
                    cardShape
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
                    if (district != null) {
                        Text(
                            text = district,
                            style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                            color = AppColors.slate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                    .background(AppColors.surfaceVariant.copy(alpha = 0.45f))
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
                    text = coords,
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
