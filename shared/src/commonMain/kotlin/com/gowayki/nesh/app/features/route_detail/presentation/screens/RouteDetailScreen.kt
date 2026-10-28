package com.gowayki.nesh.app.features.route_detail.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
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
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bell
import compose.icons.feathericons.Menu
import compose.icons.feathericons.GitCommit
import compose.icons.feathericons.Minus

data class ControlPointData(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val district: String?
)

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
    var routeStops by remember { mutableStateOf<List<SitRouteStop>>(emptyList()) }
    LaunchedEffect(selectedRoute?.routeId) {
        val id = selectedRoute?.routeId
        var stops = id?.let { RouteStopsStore.stopsFor(it) }.orEmpty()
        if (stops.isEmpty()) {
            val fallback = ArequipaRoutes.all.firstOrNull { route ->
                route.isSub && route.routeId != null &&
                    RouteStopsStore.stopsFor(route.routeId!!).isNotEmpty()
            }
            if (fallback != null) {
                SelectedRouteHolder.select(fallback)
                stops = RouteStopsStore.stopsFor(fallback.routeId!!)
            }
        }
        routeStops = stops
    }
    val controlPoints = routeStops.map {
        ControlPointData(it.id, it.name, it.latitude, it.longitude, it.district)
    }

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
                .padding(horizontal = AppSpacing.pageHorizontal, vertical = AppSpacing.sm)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Encabezado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WaykiBackButton(onClick = onBack)
                    Text(
                        text = "Tu recorrido",
                        style = AppTextStyles.waykiHeader,
                        color = AppColors.ink
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Menu hamburguesa -> abre el sidebar
                    Icon(
                        imageVector = FeatherIcons.Menu,
                        contentDescription = "Menu",
                        tint = AppColors.ink,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { coroutineScope.launch { drawerState.open() } }
                    )
                    Icon(
                        imageVector = FeatherIcons.Bell,
                        contentDescription = "Notificaciones",
                        tint = AppColors.ink,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onNotificationsClick() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtítulo de fecha y estado
            Text(
                text = "Paraderos del recorrido seleccionado",
                style = AppTextStyles.waykiMini.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = AppColors.slate
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Ficha de ruta seleccionada
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = FeatherIcons.GitCommit,
                    contentDescription = null,
                    tint = AppColors.ink,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedRoute?.name ?: "Selecciona una ruta",
                        style = AppTextStyles.waykiLabelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = AppColors.ink
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selectedRoute?.operator ?: "Sistema Integrado de Transporte",
                        style = AppTextStyles.waykiMini.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = AppColors.slate
                    )
                }

                Text(
                    text = "${routeStops.size} paraderos",
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = AppColors.ink
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Resumen de puntos disponibles para esta ruta.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.softSage, RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${routeStops.size}",
                        style = AppTextStyles.waykiHeroTitle.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                        color = AppColors.ink
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Puntos de control de la ruta",
                            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                            color = AppColors.ink
                        )
                        Text(
                            text = "Ordenados según el recorrido y sus coordenadas",
                            style = AppTextStyles.waykiMini.copy(fontSize = 12.sp),
                            color = AppColors.slate
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 4. Encabezado de Puntos de Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Puntos de control",
                    style = AppTextStyles.waykiLabelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = AppColors.ink
                )

                Text(
                    text = "${controlPoints.size} registros",
                    style = AppTextStyles.waykiMini.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = AppColors.slate
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Lista de Puntos de Control
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                controlPoints.forEachIndexed { index, point ->
                    ControlPointCard(point = point, number = index + 1, isInitial = index == 0)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 5. Criterio de cumplimiento al pie
            Text(
                text = if (controlPoints.isEmpty()) "No hay paraderos registrados para esta ruta." else "Coordenadas de los paraderos registrados para esta ruta.",
                style = AppTextStyles.waykiMini.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = AppColors.slate,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(130.dp))
        }

        // Barra de Navegación Inferior
        WaykiBottomNav(
            selectedTab = 2, // "Recorrido" activo
            onTabSelected = onTabSelected,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    }
}

@Composable
private fun ControlPointCard(point: ControlPointData, number: Int, isInitial: Boolean) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(7.dp, shape, spotColor = AppColors.ink.copy(alpha = 0.08f))
            .clip(shape)
            .border(1.dp, AppColors.outline, shape)
            .background(AppColors.surface, shape)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(if (isInitial) AppColors.secondary else AppColors.softSage, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number.toString(),
                        style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = AppColors.ink
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = point.name,
                        style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = AppColors.ink
                    )
                    if (!point.district.isNullOrBlank()) {
                        Text(
                            text = point.district,
                            style = AppTextStyles.waykiMini.copy(fontSize = 12.sp),
                            color = AppColors.slate
                        )
                    }
                }
                if (isInitial) {
                    Text(
                        text = "INICIO",
                        style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = AppColors.ink,
                        modifier = Modifier
                            .background(AppColors.softSage, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.softSage, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(FeatherIcons.GitCommit, contentDescription = null, tint = AppColors.slate, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${point.latitude}, ${point.longitude}",
                    style = AppTextStyles.waykiMini.copy(fontSize = 12.sp),
                    color = AppColors.slate
                )
            }
        }
    }
}
