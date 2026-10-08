// Pantalla "Reportes" — idéntica al video de referencia.
// Mapa OSM funcional con gestos + iconos Feather/FontAwesome + historial colapsable.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.reports.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gowayki.nesh.app.common.ui.shared.WaykiBackground
import com.gowayki.nesh.app.features.reports.domain.model.OsmLatLng
import com.gowayki.nesh.app.features.reports.domain.model.OsmMarkerType
import com.gowayki.nesh.app.features.reports.domain.model.ReportItem
import com.gowayki.nesh.app.features.reports.presentation.StateFlow.ReportsViewModel
import com.gowayki.nesh.app.features.reports.presentation.components.OsmMap
import com.gowayki.nesh.app.features.reports.presentation.components.OsmMarker
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppRadius
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Bell
import compose.icons.feathericons.ChevronDown
import compose.icons.feathericons.Crosshair
import compose.icons.feathericons.Home
import compose.icons.feathericons.LifeBuoy
import compose.icons.feathericons.LogOut
import compose.icons.feathericons.Map
import compose.icons.feathericons.Maximize2
import compose.icons.feathericons.Menu
import compose.icons.feathericons.Minus
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Settings
import compose.icons.feathericons.User
import compose.icons.feathericons.Zap
import compose.icons.fontawesomeicons.SolidGroup
import compose.icons.fontawesomeicons.solid.Bus

// ReportItem vive en domain/model (fuente única para pantalla y ViewModel).

// ---------- Pantalla (solo muestra estado y reenvía eventos al VM) ----------

@Composable
fun ReportsScreen(
    onBack: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onSosClick: () -> Unit = {},
    onMapClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    vm: ReportsViewModel = viewModel { ReportsViewModel() },
) {
    val ui = vm.ui
    val reports = ui.reports
    val selectedId = ui.selectedId
    val expanded = ui.expanded
    val fullMap = ui.fullMap
    val tab = ui.tab
    var menuOpen by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }
    // El home aún no existe: se avisa con un toast que se oculta solo.
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { entered = true }
    LaunchedEffect(toast) {
        if (toast != null) {
            kotlinx.coroutines.delay(2200)
            toast = null
        }
    }

    val mapState = vm.map
    val markers = remember(reports) { reports.map { OsmMarker(it.id, it.pos, it.type) } }
    val route = remember(reports) { reports.map { it.pos } }
    val mapHeight by animateDpAsState(
        targetValue = if (fullMap) 430.dp else 252.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 320f),
        label = "mapH",
    )

    WaykiBackground(withRoads = true, roadsAnimated = false) {
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = AppSpacing.pageHorizontal, vertical = AppSpacing.sm)
                .padding(bottom = 108.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(350)) + slideInVertically(tween(350) { -it / 3 }),
            ) {
                ReportsTopBar(onBack = onBack, onMenu = { menuOpen = true })
            }
            Spacer(Modifier.height(AppSpacing.md))

            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(450, delayMillis = 120)) +
                    scaleIn(spring(dampingRatio = 0.8f, stiffness = 300f), initialScale = 0.94f),
            ) {
                Box(
                    Modifier.fillMaxWidth()
                        .height(mapHeight)
                        .animateContentSize()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, AppColors.primary.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                        .shadow(10.dp, RoundedCornerShape(20.dp))
                        .background(AppColors.cream),
                ) {
                    OsmMap(
                        state = mapState,
                        tiles = vm.tiles,
                        markers = markers,
                        selectedId = selectedId,
                        route = route,
                        grayScale = true,
                        onNeedTiles = vm::ensureTiles,
                        onMarkerClick = { vm.select(it.id) },
                        markerContent = { m, sel -> MapPin(m.type, sel) },
                        modifier = Modifier.fillMaxSize(),
                    )
                    Column(
                        Modifier.align(Alignment.TopEnd).padding(AppSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    ) {
                        ZoomBtn(FeatherIcons.Plus) { mapState.smoothZoomIn() }
                        ZoomBtn(FeatherIcons.Minus) { mapState.smoothZoomOut() }
                        ZoomBtn(FeatherIcons.Maximize2) { vm.toggleFullMap() }
                    }
                    Row(
                        Modifier.align(Alignment.BottomStart)
                            .padding(AppSpacing.sm)
                            .background(AppColors.cream.copy(alpha = 0.94f), RoundedCornerShape(10.dp))
                            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                    ) {
                        LegendDot(AppColors.onBackground); LegendLabel("Emergencia")
                        LegendDot(AppColors.primary); LegendLabel("Unidad")
                        LegendDot(AppColors.outline); LegendLabel("Incidencia")
                    }
                    Box(
                        Modifier.align(Alignment.BottomEnd)
                            .padding(AppSpacing.sm)
                            .background(AppColors.steel.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text("© OpenStreetMap", color = AppColors.onBackground, fontSize = 9.sp)
                    }
                }
            }

            Spacer(Modifier.height(AppSpacing.md))

            // Historial: colapsado muestra solo el seleccionado;
            // el botón chevron lo despliega.
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, AppColors.primary.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                    .background(AppColors.cream)
                    .padding(AppSpacing.md)
                    .animateContentSize(spring(dampingRatio = 0.8f, stiffness = 320f)),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                val visible = if (expanded) reports else reports.filter { it.id == selectedId }
                visible.forEachIndexed { i, r ->
                    AnimatedVisibility(
                        visible = entered,
                        enter = fadeIn(tween(350, delayMillis = 200 + i * 110)) +
                            slideInVertically(tween(400, delayMillis = 200 + i * 110) { it / 2 }),
                    ) {
                        ReportRow(
                            item = r,
                            active = r.id == selectedId,
                            expanded = expanded,
                            isFirst = i == 0,
                            onClick = { vm.onReportClick(r.id) },
                        )
                    }
                }
            }
        }

        WaykiBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = entered,
            selectedTab = tab,
            onHomeClick = {
                vm.selectTab(0)
                toast = "Sección Home — en proceso"
                onHomeClick()
            },
            onSosClick = {
                vm.selectTab(1)
                onSosClick()
            },
            // Botón mapa: activa el tab, expande el mapa y despliega el historial.
            onMapClick = {
                vm.selectTab(2)
                vm.toggleFullMap()
                onMapClick()
            },
        )

        // Avisito flotante solo para Home (esa vista aún no existe).
        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn(tween(250)) + slideInVertically(tween(350)) { it },
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 128.dp),
        ) {
            Box(
                Modifier.shadow(10.dp, RoundedCornerShape(50.dp))
                    .background(AppColors.onBackground, RoundedCornerShape(50.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(
                    toast.orEmpty(),
                    color = AppColors.cream,
                    style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }

        AnimatedVisibility(visible = menuOpen, enter = fadeIn(tween(250)), exit = fadeOut(tween(200))) {
            Box(
                Modifier.fillMaxSize()
                    .background(AppColors.onBackground.copy(alpha = 0.28f))
                    .clickable { menuOpen = false },
            )
        }
        AnimatedVisibility(
            visible = menuOpen,
            enter = slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { -it },
            exit = slideOutHorizontally(tween(280)) { -it },
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            ConductorDrawer(
                onSettings = { menuOpen = false; onSettingsClick() },
                onLogout = { menuOpen = false; onLogoutClick() },
            )
        }
    }
}

// ---------- TopBar (igual al video) ----------

@Composable
private fun ReportsTopBar(onBack: () -> Unit, onMenu: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CircleBtn(onClick = onBack) {
            Icon(FeatherIcons.ArrowLeft, null, tint = AppColors.onBackground, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(AppSpacing.sm))
        Column(Modifier.weight(1f)) {
            Text(
                "Reportes", color = AppColors.onBackground,
                style = AppTextStyles.waykiButtonLarge.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold),
                maxLines = 1,
            )
            Text(
                "consulta de ultimas emergecias en la zona", color = AppColors.onSurfaceMuted,
                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Normal),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        CircleBtn(onClick = onMenu, container = AppColors.cream) {
            Icon(FeatherIcons.Menu, null, tint = AppColors.onBackground, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(AppSpacing.sm))
        CircleBtn(onClick = {}, container = AppColors.cream) {
            Box(Modifier.size(22.dp)) {
                Icon(FeatherIcons.Bell, null, tint = AppColors.onBackground, modifier = Modifier.size(22.dp))
                Box(
                    Modifier.align(Alignment.TopEnd).size(8.dp)
                        .background(AppColors.onBackground, CircleShape)
                        .border(1.5.dp, AppColors.cream, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun CircleBtn(
    onClick: () -> Unit,
    container: androidx.compose.ui.graphics.Color = AppColors.primary.copy(alpha = 0.45f),
    content: @Composable () -> Unit,
) {
    PressScale(onClick = onClick) {
        Box(
            Modifier.size(44.dp).background(container, CircleShape),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

// ---------- Pin del mapa (iconos de librería + halo) ----------

@Composable
private fun MapPin(type: OsmMarkerType, selected: Boolean) {
    val pulse = rememberInfiniteTransition(label = "pin")
    val haloScale by pulse.animateFloat(1f, if (selected) 1.4f else 1.12f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s")
    val haloAlpha by pulse.animateFloat(0.5f, if (selected) 0.1f else 0.22f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    val glowSize = if (selected) 56.dp else 42.dp
    val coreSize = if (selected) 42.dp else 33.dp
    val coreIcon = if (selected) 22.dp else 18.dp
    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(glowSize).scale(haloScale).background(AppColors.onBackground.copy(alpha = haloAlpha), CircleShape))
        val bg = when (type) {
            OsmMarkerType.UNIDAD -> AppColors.onBackground
            OsmMarkerType.EMERGENCIA -> AppColors.secondary
            OsmMarkerType.INCIDENCIA -> AppColors.primary.copy(alpha = 0.9f)
        }
        Box(
            Modifier.size(coreSize).background(bg, CircleShape).border(2.dp, AppColors.cream, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when (type) {
                OsmMarkerType.UNIDAD -> Icon(SolidGroup.Bus, null, tint = AppColors.cream, modifier = Modifier.size(coreIcon))
                OsmMarkerType.EMERGENCIA -> Icon(FeatherIcons.Zap, null, tint = AppColors.cream, modifier = Modifier.size(coreIcon))
                OsmMarkerType.INCIDENCIA -> Icon(SolidGroup.Bus, null, tint = AppColors.onBackground, modifier = Modifier.size(coreIcon))
            }
        }
        if (type == OsmMarkerType.UNIDAD && selected) {
            Box(
                Modifier.align(Alignment.TopEnd).size(18.dp)
                    .background(AppColors.secondary, CircleShape)
                    .border(1.5.dp, AppColors.cream, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(7.dp).background(AppColors.cream, CircleShape))
            }
        }
    }
}

@Composable
private fun ZoomBtn(icon: ImageVector, onClick: () -> Unit) {
    PressScale(onClick = onClick) {
        Box(
            Modifier.size(40.dp).shadow(6.dp, CircleShape).background(AppColors.onBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = AppColors.cream, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color) {
    Box(Modifier.size(8.dp).background(color, CircleShape))
}

@Composable
private fun LegendLabel(text: String) {
    Text(text, color = AppColors.onSurfaceMuted, fontSize = 9.sp)
}

// ---------- Filas del historial ----------

@Composable
private fun ReportRow(item: ReportItem, active: Boolean, expanded: Boolean, isFirst: Boolean, onClick: () -> Unit) {
    val chev by animateFloatAsState(
        targetValue = if (expanded && isFirst) 180f else 0f,
        animationSpec = spring(stiffness = 400f, dampingRatio = 0.7f), label = "chev",
    )
    val bg = if (active) AppColors.onBackground else AppColors.cream
    val titleColor = if (active) AppColors.cream else AppColors.onBackground
    val subColor = if (active) AppColors.cream.copy(alpha = 0.75f) else AppColors.onSurfaceMuted
    PressScale(onClick = onClick) {
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(bg)
                .border(
                    if (active) 0.dp else 1.dp,
                    if (active) AppColors.transparent else AppColors.primary.copy(alpha = 0.4f),
                    RoundedCornerShape(18.dp),
                )
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp)
                    .background(
                        if (active) AppColors.primary.copy(alpha = 0.9f) else AppColors.primary.copy(alpha = 0.4f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                when (item.type) {
                    OsmMarkerType.UNIDAD -> Icon(FeatherIcons.Crosshair, null, tint = AppColors.onBackground, modifier = Modifier.size(24.dp))
                    OsmMarkerType.EMERGENCIA -> Icon(FeatherIcons.Zap, null, tint = AppColors.onBackground, modifier = Modifier.size(24.dp))
                    OsmMarkerType.INCIDENCIA -> Icon(FeatherIcons.Minus, null, tint = AppColors.onBackground, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(AppSpacing.md))
            Column(Modifier.weight(1f)) {
                Text(item.title, color = titleColor, style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(item.subtitle, color = subColor, style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Normal, fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier.background(
                    if (active) AppColors.primary.copy(alpha = 0.95f) else AppColors.primary.copy(alpha = 0.35f),
                    RoundedCornerShape(9.dp),
                ).padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(item.status.label, color = AppColors.onBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            if (isFirst) {
                Icon(
                    FeatherIcons.ChevronDown, null,
                    tint = if (active) AppColors.cream else AppColors.onSurfaceMuted,
                    modifier = Modifier.padding(start = AppSpacing.sm).size(24.dp).rotate(chev),
                )
            }
        }
    }
}

// ---------- Barra estilo video de referencia ----------
// Píldora tinta con cápsula clara deslizante: el tab activo muestra
// icono + etiqueta dentro de la cápsula. Inactivos: solo icono tenue.

@Composable
private fun WaykiBottomBar(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    selectedTab: Int = 2, // 0 home · 1 sos · 2 mapa
    onHomeClick: () -> Unit = {},
    onSosClick: () -> Unit = {},
    onMapClick: () -> Unit = {},
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400, delayMillis = 250)) + slideInVertically(tween(450, delayMillis = 250) { it }),
        modifier = modifier,
    ) {
        Box(
            Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = AppSpacing.pageHorizontal).padding(bottom = AppSpacing.md),
        ) {
            androidx.compose.foundation.layout.BoxWithConstraints(
                Modifier.fillMaxWidth()
                    .shadow(14.dp, RoundedCornerShape(50.dp))
                    .clip(RoundedCornerShape(50.dp))
                    .background(AppColors.onBackground)
                    // Margen interno lateral para que la cápsula no toque los bordes.
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                val w = maxWidth
                val side = 16.dp
                val cw = w - side * 2
                // Centros exactos con el margen descontado.
                val capX by animateDpAsState(
                    targetValue = side + (cw / 3) * (selectedTab + 0.5f) - w / 2f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 380f),
                    label = "capX",
                )
                // Cápsula clara de ancho FIJO con fundido de icono + etiqueta:
                // así no se recorta ni se asoma nada atrás al cambiar de tab.
                Box(
                    Modifier.align(Alignment.Center)
                        .offset(x = capX)
                        .size(width = 100.dp, height = 42.dp)
                        .background(AppColors.cream, RoundedCornerShape(50.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(                       targetState = selectedTab,
                        transitionSpec = {
                            fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                        },
                        label = "capTxt",
                    ) { tab ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                when (tab) {
                                    0 -> FeatherIcons.Home
                                    1 -> FeatherIcons.LifeBuoy
                                    else -> FeatherIcons.Map
                                },
                                null, tint = AppColors.onBackground, modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(AppSpacing.sm))
                            Text(
                                when (selectedTab) {
                                    0 -> "Inicio"
                                    1 -> "Alerta"
                                    else -> "Mapa"
                                },
                                color = AppColors.onBackground,
                                style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    BarTab(icon = FeatherIcons.Home, active = selectedTab == 0, onClick = onHomeClick)
                    BarTab(icon = FeatherIcons.LifeBuoy, active = selectedTab == 1, onClick = onSosClick)
                    BarTab(icon = FeatherIcons.Map, active = selectedTab == 2, onClick = onMapClick)
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BarTab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit,
) {
    // El icono de atrás se oculta con fundido al abrir su cápsula.
    val bgAlpha by animateFloatAsState(
        targetValue = if (active) 0f else 1f,
        animationSpec = tween(200), label = "bgA",
    )
    PressScale(onClick = onClick, modifier = Modifier.weight(1f)) {
        Box(
            Modifier.fillMaxWidth().padding(vertical = 12.dp).alpha(bgAlpha),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = AppColors.cream.copy(alpha = 0.65f), modifier = Modifier.size(24.dp))
        }
    }
}

// ---------- Press + drawer (igual al video) ----------

@Composable
private fun PressScale(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.6f), label = "press",
    )
    Box(modifier.scale(s).clickable(interactionSource = src, indication = null, onClick = onClick)) { content() }
}

@Composable
private fun ConductorDrawer(onSettings: () -> Unit, onLogout: () -> Unit) {
    // Orden correcto: shadow ANTES que background (si clip va primero, se come la sombra).
    // Avatar rosa sólido + textos tinta como en el video.
    Column(
        Modifier.width(292.dp).fillMaxHeight()
            .shadow(16.dp, RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
            .background(AppColors.cream, RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
            .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.xl)
            .statusBarsPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).background(AppColors.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(FeatherIcons.User, null, tint = AppColors.onBackground, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(AppSpacing.md))
            Column {
                Text("Conductor", color = AppColors.onBackground, style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 19.sp))
                Spacer(Modifier.height(2.dp))
                Text("Unidad 08 · Activa", color = AppColors.onSurfaceMuted, style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Normal, fontSize = 12.sp))
            }
        }
        Spacer(Modifier.height(AppSpacing.lg))
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.outline.copy(alpha = 0.55f)))
        Spacer(Modifier.height(AppSpacing.lg))
        DrawerRow(FeatherIcons.Settings, "Ajustes", onSettings)
        Spacer(Modifier.height(AppSpacing.sm))
        DrawerRow(FeatherIcons.LogOut, "Cerrar sesión", onLogout)
        Spacer(Modifier.weight(1f))
        Text("Wayki Nest · v1.0", color = AppColors.onSurfaceMuted, fontSize = 11.sp)
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun DrawerRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    PressScale(onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().clip(AppRadius.sm).padding(vertical = AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = AppColors.onBackground, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(AppSpacing.md))
            Text(label, color = AppColors.onBackground, style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp))
        }
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun ReportsScreenPreview() {
    // En preview no hay ViewModelStore: VM plano con remember.
    val vm = remember { ReportsViewModel() }
    ReportsScreen(vm = vm)
}
