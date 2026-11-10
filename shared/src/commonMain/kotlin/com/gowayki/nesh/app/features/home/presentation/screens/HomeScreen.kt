package com.gowayki.nesh.app.features.home.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.ui.shared.WaykiBackground
import com.gowayki.nesh.app.common.ui.shared.ToastManager
import com.gowayki.nesh.app.common.ui.shared.WaykiBottomNav
import com.gowayki.nesh.app.common.util.currentHourMinute
import com.gowayki.nesh.core.AppSettings
import com.gowayki.nesh.core.session.TurnoSession
import com.gowayki.nesh.core.session.UserSession
import com.gowayki.nesh.app.features.home.presentation.components.RouteAssignmentDialog
import com.gowayki.nesh.app.common.ui.shared.WaykiReveal
import com.gowayki.nesh.app.common.ui.shared.WaykiSkeleton
import com.gowayki.nesh.app.features.map.data.RouteStopsStore
import com.gowayki.nesh.app.features.map.data.SitRouteStop
import com.gowayki.nesh.app.features.map.presentation.SelectedRouteHolder
import com.gowayki.nesh.app.features.map.domain.model.CombiRoute
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppRadius
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.ArrowRight
import compose.icons.feathericons.Bell
import compose.icons.feathericons.Menu
import compose.icons.feathericons.Home
import compose.icons.feathericons.Radio
import compose.icons.feathericons.GitCommit
import compose.icons.feathericons.Map

import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import compose.icons.feathericons.X
import compose.icons.feathericons.Sliders
import compose.icons.feathericons.HelpCircle
import compose.icons.feathericons.LogOut
import compose.icons.feathericons.AlertTriangle
import compose.icons.feathericons.ChevronRight
import compose.icons.fontawesomeicons.SolidGroup
import compose.icons.fontawesomeicons.solid.Bus

@Composable
fun HomeScreen(
    onMenuClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onBack: () -> Unit = {},
    onSosClick: () -> Unit = {},
    onCommunicationClick: () -> Unit = {},
    onRecorridoClick: () -> Unit = {},
    onMapaClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var showRouteDialog by remember { mutableStateOf(false) }
    val selectedRoute by SelectedRouteHolder.selected.collectAsState()
    val direction by SelectedRouteHolder.direction.collectAsState()
    var routeStops by remember { mutableStateOf<List<SitRouteStop>>(emptyList()) }
    var idaStops by remember { mutableStateOf<List<SitRouteStop>>(emptyList()) }
    androidx.compose.runtime.LaunchedEffect(selectedRoute, direction) {
        val r = selectedRoute
        val routeId = if (direction == com.gowayki.nesh.app.features.map.presentation.RouteDirection.RETORNO) {
            r?.returnRouteId ?: r?.routeId
        } else {
            r?.routeId
        }
        routeStops = if (routeId != null) {
            runCatching { RouteStopsStore.stopsFor(routeId) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
    }
    androidx.compose.runtime.LaunchedEffect(selectedRoute) {
        val r = selectedRoute
        idaStops = if (r?.routeId != null) {
            runCatching { RouteStopsStore.stopsFor(r.routeId!!) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
    }

    // Simulate initial data load
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(800) // Brief skeleton flash
        isLoading = false
        if (AppSettings.notificationsEnabled.value && AppSettings.isOnRoute.value) {
            ToastManager.show("Ruta 12 a 7 min atrás • Ruta 5 a 3 min adelante")
        }
    }

    // Turno real del chofer (línea + unidad + placa). Se muestra una sola vez
    // por sesión la ventana "Tu ruta de hoy" / "Escoge tu ruta".
    androidx.compose.runtime.LaunchedEffect(Unit) {
        TurnoSession.refresh()
        if (!TurnoSession.promptShown) {
            TurnoSession.promptShown = true
            showRouteDialog = true
        }
    }

    // Aviso de hora de almuerzo y de fin próximo.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        var lunchNotified = ""
        var endNotified = ""
        fun toMinutes(t: String): Int {
            val parts = t.split(":")
            return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
        }
        while (true) {
            val now = currentHourMinute()
            val nowMin = toMinutes(now)
            val startMin = toMinutes(AppSettings.lunchTime.value)
            val endMin = startMin + AppSettings.lunchDurationMinutes.value
            if (AppSettings.notificationsEnabled.value && AppSettings.lunchEnabled.value) {
                val startMin = toMinutes(AppSettings.lunchTime.value)
                val fiveBefore = (startMin - 5 + 1440) % 1440
                if (nowMin == fiveBefore && now != lunchNotified) {
                    lunchNotified = now
                    ToastManager.show("Tu almuerzo empieza en 5 min")
                }
                if (now == AppSettings.lunchTime.value && now != lunchNotified) {
                    lunchNotified = now
                    ToastManager.show("Hora de almuerzo: ${AppSettings.lunchTime.value}")
                }
                // Notificar cuando falten 5 minutos para terminar.
                if (nowMin in (endMin - 5)..(endMin - 1) && now != endNotified) {
                    endNotified = now
                    ToastManager.show("Faltan 5 min para terminar tu almuerzo")
                }
            }
            kotlinx.coroutines.delay(30000)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = AppRadius.modal,
                drawerContainerColor = AppColors.cream,
                modifier = Modifier.width(328.dp).fillMaxHeight()
            ) {
                WaykiSidebarContent(
                    selectedTab = 0,
                    onClose = {
                        coroutineScope.launch { drawerState.close() }
                    },
                    onSosClick = {
                        coroutineScope.launch { drawerState.close() }
                        onSosClick()
                    },
                    onInicioClick = {
                        coroutineScope.launch { drawerState.close() }
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
        WaykiBackground(withRoads = true, roadsAnimated = true) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Header azul pastel que se desplaza con el scroll ──
                WaykiReveal(delayMillis = 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = AppColors.primary,
                                shape = RoundedCornerShape(
                                    topStart = 0.dp,
                                    topEnd = 0.dp,
                                    bottomStart = 40.dp,
                                    bottomEnd = 40.dp
                                )
                            )
                            .statusBarsPadding()
                            .padding(horizontal = AppSpacing.pageHorizontal)
                            .padding(top = AppSpacing.sm, bottom = 32.dp)
                    ) {
                        Column {
                        // Barra superior
                        // Sin boton de retroceso: la navegacion (incluido
                        // "cerrar sesion") vive en el menu lateral.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                                TopCircleButton(
                                    icon = FeatherIcons.Menu,
                                    description = "Menu",
                                    onClick = { coroutineScope.launch { drawerState.open() } }
                                )
                                TopCircleButton(
                                    icon = FeatherIcons.Bell,
                                    description = "Notificaciones",
                                    onClick = onNotificationsClick
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Saludo
                        Text(
                            text = "Sigamos",
                            style = AppTextStyles.waykiHeader,
                            color = AppColors.cream
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = UserSession.displayName,
                                style = AppTextStyles.waykiLabelMedium,
                                color = AppColors.cream.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val turno = TurnoSession.turno
                            val lineaResumen = if (turno != null) {
                                TurnoSession.resumen
                            } else {
                                selectedRoute?.name?.substringBefore(" · ")
                            }
                            if (lineaResumen != null) {
                                Spacer(Modifier.width(AppSpacing.sm))
                                Text(
                                    text = "•",
                                    style = AppTextStyles.waykiLabelMedium,
                                    color = AppColors.cream.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    SolidGroup.Bus,
                                    contentDescription = "Ruta de combi",
                                    tint = AppColors.cream.copy(alpha = 0.85f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = lineaResumen,
                                    style = AppTextStyles.waykiLabelMedium,
                                    color = AppColors.cream.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Etiqueta de sección, sobre el fondo azul.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tu recorrido del día",
                                style = AppTextStyles.waykiLabelMedium,
                                color = AppColors.cream
                            )
                            LiveBadge()
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Selector rápido de sentido (ida/vuelta) sin entrar a Recorrido.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isIda = direction == com.gowayki.nesh.app.features.map.presentation.RouteDirection.IDA
                            listOf(
                                com.gowayki.nesh.app.features.map.presentation.RouteDirection.IDA to "Ida",
                                com.gowayki.nesh.app.features.map.presentation.RouteDirection.RETORNO to "Vuelta"
                            ).forEach { (dir, label) ->
                                val selected = direction == dir
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (selected) AppColors.cream else AppColors.cream.copy(alpha = 0.18f),
                                            RoundedCornerShape(50)
                                        )
                                        .clickable { SelectedRouteHolder.setDirection(dir) }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                        color = if (selected) AppColors.ink else AppColors.cream
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tarjeta principal
                        if (isLoading) {
                            WaykiSkeleton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                color = AppColors.cream.copy(alpha = 0.2f),
                                shape = AppRadius.card
                            )
                        } else {
                            RouteCard(
                                route = selectedRoute,
                                stops = routeStops,
                                offsetMinutes = if (direction == com.gowayki.nesh.app.features.map.presentation.RouteDirection.RETORNO) routeDurationMinutes(idaStops, selectedRoute) else 0,
                                isOnRoute = AppSettings.isOnRoute.value,
                                onClick = onRecorridoClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

                // ── Contenido sobre fondo claro ─────────────────────────────
                WaykiReveal(delayMillis = 80) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.pageHorizontal)
                            .padding(top = 28.dp)
                    ) {
                    // Turno del chofer: si aún no tiene unidad, se le invita a escogerla.
                    if (TurnoSession.loaded && !TurnoSession.hasTurno) {
                        androidx.compose.material3.Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppRadius.card,
                            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = AppColors.primary.copy(alpha = 0.08f)),
                            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(AppColors.primary.copy(alpha = 0.14f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = SolidGroup.Bus,
                                            contentDescription = null,
                                            tint = AppColors.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Sin ruta asignada",
                                            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                            color = AppColors.ink
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Escoge tu línea y tu combi para comenzar el turno.",
                                            style = AppTextStyles.waykiMini,
                                            color = AppColors.slate
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                androidx.compose.material3.TextButton(
                                    onClick = { showRouteDialog = true },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(
                                        text = "Escoger ruta",
                                        color = AppColors.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Otras combis de la línea, según los turnos activos reales.
                    val miTurno = TurnoSession.turno
                    val companeros = if (miTurno != null) {
                        TurnoSession.activeTurnos.filter {
                            it.lineaId == miTurno.lineaId && it.choferId != miTurno.choferId
                        }
                    } else {
                        emptyList()
                    }
                    Text(
                        text = if (miTurno?.lineaNombre != null) "Combis en ${miTurno.lineaNombre}" else "Combis en tu línea",
                        style = AppTextStyles.waykiLabelMedium,
                        color = AppColors.ink
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppRadius.card,
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = AppColors.cream),
                        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            if (companeros.isEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(AppColors.primary.copy(alpha = 0.12f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = SolidGroup.Bus,
                                            contentDescription = null,
                                            tint = AppColors.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Esperando otras combis",
                                            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                            color = AppColors.ink
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Aún no hay otros conductores en ruta dentro de tu línea.",
                                            style = AppTextStyles.waykiMini,
                                            color = AppColors.slate
                                        )
                                    }
                                }
                            } else {
                                companeros.forEachIndexed { index, turno ->
                                    if (index > 0) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(AppColors.slate.copy(alpha = 0.15f))
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(AppColors.primary.copy(alpha = 0.12f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = SolidGroup.Bus,
                                                contentDescription = null,
                                                tint = AppColors.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Unidad ${turno.numeroPadron ?: "—"} · ${turno.placa ?: "Sin placa"}",
                                                style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                                color = AppColors.ink
                                            )
                                            Text(
                                                text = turno.choferNombre ?: "Conductor",
                                                style = AppTextStyles.waykiMini,
                                                color = AppColors.slate
                                            )
                                        }
                                        Text(
                                            text = "En ruta",
                                            style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                                            color = AppColors.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Espacio para la barra inferior flotante
                    Spacer(modifier = Modifier.height(140.dp))
                }
            }
            }

            // Barra de navegación inferior, anclada al fondo
            WaykiBottomNav(
                selectedTab = 0, // "Inicio" es la pestaña activa
                onTabSelected = { tab ->
                    if (tab == 1) onCommunicationClick()
                    else if (tab == 2) onRecorridoClick()
                    else if (tab == 3) onMapaClick()
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    RouteAssignmentDialog(
        visible = showRouteDialog && !isLoading,
        onDismiss = { showRouteDialog = false },
        onAssigned = {
            showRouteDialog = false
            SelectedRouteHolder.selectLineByName(it.lineaNombre)
        },
    )
}

// ---------- Botón circular de la barra superior (menú, campana) ----------
@Composable
private fun TopCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(AppSpacing.buttonSecondaryHeight)
            .background(AppColors.cream.copy(alpha = 0.18f), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AppColors.cream.copy(alpha = 0.15f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = AppColors.cream,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ---------- Badge "EN VIVO" ----------
@Composable
private fun LiveBadge(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(AppColors.cream, AppRadius.card)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(AppColors.ink, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "EN VIVO",
            style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
            color = AppColors.ink
        )
    }
}

// ---------- Tarjeta del recorrido del día ----------
@Composable
private fun RouteCard(
    route: CombiRoute? = null,
    stops: List<SitRouteStop> = emptyList(),
    offsetMinutes: Int = 0,
    isOnRoute: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val compliance = compliancePercent(isOnRoute)
    val points = todayControlPoints(route, stops, offsetMinutes)
    val next = nextControlPoint(points)
    val oneWay = routeDurationMinutes(stops, route)
    val roundTrip = oneWay * 2
    // Línea activa: código corto (ej. C2, A15).
    val lineCode = route?.id ?: "—"
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = AppRadius.card,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = AppColors.cream),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp),
        interactionSource = interactionSource
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Cabecera
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(AppColors.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = SolidGroup.Bus,
                        contentDescription = null,
                        tint = AppColors.cream,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = route?.name ?: "Sin ruta seleccionada",
                        style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = AppColors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = route?.operator?.let { "Operador: $it" } ?: "Selecciona una ruta en Mapa",
                        style = AppTextStyles.waykiMini,
                        color = AppColors.slate
                    )
                }
                LiveBadge()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Próximo punto de control (relacionado con el recorrido del mapa)
            Text(
                text = "PRÓXIMO PUNTO DE CONTROL",
                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                color = AppColors.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = next?.name ?: "Sin puntos",
                        style = AppTextStyles.waykiButtonLarge,
                        color = AppColors.ink
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = next?.let { "Hora programada: ${it.time}" } ?: "",
                        style = AppTextStyles.waykiMini,
                        color = AppColors.slate
                    )
                }
                next?.let {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = it.time,
                            style = AppTextStyles.waykiHeader,
                            color = AppColors.ink
                        )
                        Text(
                            text = "hora",
                            style = AppTextStyles.waykiMini,
                            color = AppColors.ink
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Puntos de hoy (cada 20 min desde la hora de inicio)
            Text(
                text = "PUNTOS DE HOY",
                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                color = AppColors.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                points.forEach { pt ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pt.name,
                            style = AppTextStyles.waykiMini,
                            color = AppColors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = pt.time,
                            style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.slate,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Duración ida y vuelta: ~$roundTrip min",
                style = AppTextStyles.waykiMini,
                color = AppColors.slate
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Estadísticas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PillStat(top = "$compliance%", bottom = "Cumplimiento", modifier = Modifier.weight(1f))
                PillStat(top = "${points.size}", bottom = "Puntos hoy", modifier = Modifier.weight(1f))
                PillStat(top = lineCode, bottom = "Línea activa", modifier = Modifier.weight(1f))
            }
        }
    }
}

private data class ControlPoint(val name: String, val time: String)

private fun shiftStartMinutes(): Int {
    val t = AppSettings.shiftStartTime.value
    val p = t.split(":")
    return (p.getOrNull(0)?.toIntOrNull() ?: 5) * 60 + (p.getOrNull(1)?.toIntOrNull() ?: 0)
}

private fun toMinutesHome(t: String): Int {
    val p = t.split(":")
    return (p.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (p.getOrNull(1)?.toIntOrNull() ?: 0)
}

private fun formatMinutesHome(m: Int): String {
    val mm = ((m % 1440) + 1440) % 1440
    val h = (mm / 60).toString().padStart(2, '0')
    val min = (mm % 60).toString().padStart(2, '0')
    return "$h:$min"
}

private fun todayControlPoints(route: CombiRoute? = null, stops: List<SitRouteStop> = emptyList(), offsetMinutes: Int = 0): List<ControlPoint> {
    val chosenStops = pickImportantStops(stops, 6)
    val names = if (chosenStops.isNotEmpty()) {
        chosenStops.map { it.name }
    } else {
        val coords = route?.waypoints ?: emptyList()
        if (coords.isNotEmpty()) {
            sampleRoutePoints(coords, 6).mapIndexed { i, _ ->
                when (i) {
                    0 -> "Terminal salida"
                    5 -> "Terminal"
                    else -> "Punto ${i + 1}"
                }
            }
        } else {
            listOf("Terminal salida", "Av. Dolores", "Plaza de Armas", "Av. Ejército", "Terminal retorno", "Terminal")
        }
    }
    val count = names.size.coerceAtLeast(2)
    val oneWay = routeDurationMinutes(stops, route)
    val interval = (oneWay / (count - 1)).coerceAtLeast(5)
    val start = shiftStartMinutes() + offsetMinutes
    return names.mapIndexed { i, n -> ControlPoint(n, formatMinutesHome(start + i * interval)) }
}

private fun routeDurationMinutes(stops: List<SitRouteStop>, route: CombiRoute?): Int {
    // Estimación: ~3 min por paradero (ida). Mínimo 60 min.
    val base = if (stops.isNotEmpty()) stops.size * 3 else (route?.waypoints?.size ?: 6) * 3
    return base.coerceAtLeast(60)
}

private fun pickImportantStops(stops: List<SitRouteStop>, count: Int = 6): List<SitRouteStop> {
    if (stops.isEmpty()) return emptyList()
    if (stops.size <= count) return stops
    val result = mutableListOf<SitRouteStop>()
    for (i in 0 until count) {
        val idx = (i * (stops.size - 1)) / (count - 1)
        result.add(stops[idx])
    }
    return result.distinctBy { it.id }
}

private fun sampleRoutePoints(waypoints: List<Pair<Double, Double>>, count: Int): List<Pair<Double, Double>> {
    if (waypoints.isEmpty()) return emptyList()
    if (waypoints.size == 1) return List(count) { waypoints.first() }
    val result = mutableListOf<Pair<Double, Double>>()
    for (i in 0 until count) {
        val t = i.toDouble() / (count - 1)
        val pos = t * (waypoints.size - 1)
        val lo = pos.toInt().coerceIn(0, waypoints.size - 2)
        val hi = (lo + 1).coerceAtMost(waypoints.size - 1)
        val frac = pos - lo
        val lat = waypoints[lo].first + (waypoints[hi].first - waypoints[lo].first) * frac
        val lon = waypoints[lo].second + (waypoints[hi].second - waypoints[lo].second) * frac
        result.add(Pair(lat, lon))
    }
    return result
}

private fun compliancePercent(isOnRoute: Boolean): Int {
    if (!isOnRoute) return 0
    val now = toMinutesHome(currentHourMinute())
    val start = shiftStartMinutes()
    var elapsed = now - start
    if (elapsed < 0) elapsed += 1440
    val total = AppSettings.shiftDurationMinutes.value.coerceAtLeast(1)
    return (elapsed.toFloat() / total * 100).toInt().coerceIn(0, 100)
}

private fun nextControlPoint(points: List<ControlPoint>): ControlPoint? {
    val now = toMinutesHome(currentHourMinute())
    return points.firstOrNull { toMinutesHome(it.time) > now } ?: points.lastOrNull()
}

@Composable
private fun PillStat(top: String, bottom: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(AppColors.secondary, AppRadius.card)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = top,
                style = AppTextStyles.waykiButtonLarge.copy(fontSize = 16.sp),
                color = AppColors.ink
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = bottom,
                style = AppTextStyles.labelSmall.copy(fontSize = 9.sp),
                color = AppColors.ink.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun BusCard(
    title: String,
    line: String,
    time: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AppRadius.card,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = AppColors.cream),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp),
        interactionSource = interactionSource,
    ) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bus emoji as compact circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(AppColors.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = SolidGroup.Bus,
                    contentDescription = null,
                    tint = AppColors.cream,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title and line info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = AppColors.ink
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = line,
                    style = AppTextStyles.waykiMini,
                    color = AppColors.slate
                )
            }

            // Time and arrow
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = time,
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color = AppColors.ink
                )
                Spacer(modifier = Modifier.height(2.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppColors.ink,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
    }
}


@Composable
fun WaykiSidebarContent(
    onClose: () -> Unit,
    selectedTab: Int = 0,
    onSosClick: () -> Unit = {},
    onInicioClick: () -> Unit = {},
    onCommunicationClick: () -> Unit = {},
    onRecorridoClick: () -> Unit = {},
    onMapaClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.cream)
            .statusBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Marca y cierre
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Wayki Nest", 
                style = AppTextStyles.waykiHeader, 
                color = AppColors.ink
            )
            val closeInteractionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AppColors.mauve, CircleShape)
                    .clickable(
                        interactionSource = closeInteractionSource,
                        indication = ripple(color = AppColors.ink.copy(alpha = 0.2f)),
                        onClick = onClose
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FeatherIcons.X, 
                    contentDescription = "Cerrar", 
                    tint = AppColors.ink,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 2. Conductor
        Text(
            text = UserSession.displayName, 
            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), 
            color = AppColors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (TurnoSession.hasTurno) {
                "${TurnoSession.lineaNombre} • ${TurnoSession.unidadTexto} • ${TurnoSession.placa ?: "Sin placa"}"
            } else {
                "Sin unidad asignada • Arequipa"
            },
            style = AppTextStyles.waykiMini,
            color = AppColors.slate,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 3. Menu Principal label
        Text(
            text = "Menu Principal", 
            style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold), 
            color = AppColors.slate
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // 4. Navegacion Principal
        DrawerMenuItem(
            icon = FeatherIcons.Home, 
            title = "Inicio", 
            subtitle = "Tu ritmo y próximo control", 
            isSelected = selectedTab == 0,
            onClick = onInicioClick
        )
        DrawerMenuItem(
            icon = FeatherIcons.Radio, 
            title = "Comunicación", 
            subtitle = if (TurnoSession.hasTurno) "Canal de ${TurnoSession.lineaNombre}" else "Canal de tu línea", 
            isSelected = selectedTab == 1,
            onClick = onCommunicationClick
        )
        DrawerMenuItem(
            icon = FeatherIcons.GitCommit, 
            title = "Recorrido", 
            subtitle = "Tus recorridos y horarios", 
            isSelected = selectedTab == 2,
            onClick = onRecorridoClick
        )
        DrawerMenuItem(
            icon = FeatherIcons.Map, 
            title = "Mapa", 
            subtitle = "Informacion de tus rutas", 
            isSelected = selectedTab == 3,
            onClick = onMapaClick
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 5. Separador
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.slate.copy(alpha = 0.2f)))
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 6. Preferencias y ayuda
        DrawerSimpleItem(icon = FeatherIcons.Sliders, title = "Configuración", onClick = onSettingsClick)
        DrawerSimpleItem(icon = FeatherIcons.HelpCircle, title = "Soporte técnico", onClick = onSupportClick)
        DrawerSimpleItem(icon = FeatherIcons.LogOut, title = "Cerrar sesión", onClick = onLogoutClick)
        
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp)) // ensure space if small screen
        
        // 7. SOS
        val sosInteractionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.primary, RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = sosInteractionSource,
                    indication = ripple(color = AppColors.ink.copy(alpha = 0.2f)),
                    onClick = onSosClick
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = FeatherIcons.AlertTriangle, 
                    contentDescription = "SOS", 
                    tint = AppColors.cream,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "S.O.S", 
                        style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp), 
                        color = AppColors.cream
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Informacion de emergencias.", 
                        style = AppTextStyles.waykiMini, 
                        color = AppColors.cream.copy(alpha = 0.8f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Recorrido en curso • GPS activo", 
            style = AppTextStyles.waykiMini, 
            color = AppColors.slate,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DrawerMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> AppColors.ink
            isPressed -> AppColors.mauve.copy(alpha = 0.4f)
            else -> androidx.compose.ui.graphics.Color.Transparent
        },
        animationSpec = tween(260, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "drawerItemBg"
    )
    val contentColor = if (isSelected) AppColors.cream else AppColors.ink
    val subtitleColor = if (isSelected) AppColors.cream.copy(alpha = 0.7f) else AppColors.slate

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AppColors.primary.copy(alpha = 0.15f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = AppTextStyles.waykiMini,
                color = subtitleColor
            )
        }
    }
}

@Composable
private fun DrawerSimpleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AppColors.primary.copy(alpha = 0.15f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.ink,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
            color = AppColors.ink,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = FeatherIcons.ChevronRight,
            contentDescription = null,
            tint = AppColors.ink,
            modifier = Modifier.size(20.dp)
        )
    }
}
