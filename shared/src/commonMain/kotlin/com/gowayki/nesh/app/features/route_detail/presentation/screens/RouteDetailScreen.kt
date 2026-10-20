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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.app.common.ui.shared.WaykiBottomNav
import com.gowayki.nesh.app.features.home.presentation.screens.WaykiSidebarContent
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bell
import compose.icons.feathericons.Menu
import compose.icons.feathericons.GitCommit
import compose.icons.feathericons.Minus
import compose.icons.feathericons.TrendingDown
import compose.icons.feathericons.TrendingUp

enum class PointToleranceType {
    NEUTRAL,
    LATE,
    EARLY
}

data class ControlPointData(
    val name: String,
    val scheduledTime: String,
    val actualTime: String,
    val diffText: String,
    val type: PointToleranceType,
    val isFirstTwo: Boolean
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
    onSettingsClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val controlPoints = listOf(
        ControlPointData(
            name = "Pte. Grau / Av. Ejército",
            scheduledTime = "06:14",
            actualTime = "06:16",
            diffText = "+2 min",
            type = PointToleranceType.NEUTRAL,
            isFirstTwo = true
        ),
        ControlPointData(
            name = "Terminal Terrestre",
            scheduledTime = "06:31",
            actualTime = "06:30",
            diffText = "-1 min",
            type = PointToleranceType.NEUTRAL,
            isFirstTwo = true
        ),
        ControlPointData(
            name = "Plaza de Armas",
            scheduledTime = "06:48",
            actualTime = "06:53",
            diffText = "+5 min",
            type = PointToleranceType.LATE,
            isFirstTwo = false
        ),
        ControlPointData(
            name = "Av. Independencia",
            scheduledTime = "07:05",
            actualTime = "07:06",
            diffText = "+1 min",
            type = PointToleranceType.NEUTRAL,
            isFirstTwo = false
        ),
        ControlPointData(
            name = "Mercado San Camilo",
            scheduledTime = "07:22",
            actualTime = "07:20",
            diffText = "-2 min",
            type = PointToleranceType.EARLY,
            isFirstTwo = false
        ),
        ControlPointData(
            name = "Óvalo Paucarpata",
            scheduledTime = "07:41",
            actualTime = "07:41",
            diffText = "±0 min",
            type = PointToleranceType.NEUTRAL,
            isFirstTwo = false
        )
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
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
                text = "Jueves 8 de octubre · Recorrido finalizado",
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
                        text = "Línea 7 · Unidad 08",
                        style = AppTextStyles.waykiLabelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = AppColors.ink
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "06:14–07:41 · Grau → Paucarpata",
                        style = AppTextStyles.waykiMini.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = AppColors.slate
                    )
                }

                Text(
                    text = "87 min",
                    style = AppTextStyles.waykiLabelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = AppColors.ink
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Tarjeta Hero de Cumplimiento
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.ink, RoundedCornerShape(28.dp))
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "5/6",
                        style = AppTextStyles.waykiHeroTitle.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = AppColors.cream
                    )

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "83% de cumplimiento",
                            style = AppTextStyles.waykiLabelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = AppColors.cream
                        )
                        Text(
                            text = "5 de 6 puntos en tolerancia",
                            style = AppTextStyles.waykiLabelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = AppColors.cream
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Barra de progreso segmentada (6 segmentos)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            for (i in 0 until 6) {
                                val segmentColor = if (i < 5) AppColors.slate else AppColors.steel.copy(alpha = 0.5f)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(segmentColor)
                                )
                            }
                        }
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
                    text = "6 registros",
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                controlPoints.forEach { point ->
                    ControlPointCard(point = point)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 5. Criterio de cumplimiento al pie
            Text(
                text = "En tolerancia: entre −2 y +2 min del horario.",
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
private fun ControlPointCard(point: ControlPointData) {
    val cardBg = if (point.isFirstTwo) AppColors.mauve else AppColors.cream
    val cardBorder = when {
        point.isFirstTwo -> Modifier.border(1.dp, AppColors.mauve, RoundedCornerShape(22.dp))
        point.type == PointToleranceType.LATE -> Modifier.border(1.dp, AppColors.slate, RoundedCornerShape(22.dp))
        else -> Modifier.border(1.dp, AppColors.mauve, RoundedCornerShape(22.dp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .then(cardBorder)
            .background(cardBg, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de estado
            val iconCircleBg = if (point.type == PointToleranceType.LATE) AppColors.slate else AppColors.mauve
            val iconTint = if (point.type == PointToleranceType.LATE) AppColors.cream else AppColors.ink

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(iconCircleBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val iconVector = when (point.type) {
                    PointToleranceType.LATE -> FeatherIcons.TrendingUp
                    PointToleranceType.EARLY -> FeatherIcons.TrendingDown
                    PointToleranceType.NEUTRAL -> FeatherIcons.Minus
                }

                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Detalle del punto
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = point.name,
                    style = AppTextStyles.waykiLabelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    ),
                    color = AppColors.ink,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Prog. ${point.scheduledTime} · Real ${point.actualTime}",
                    style = AppTextStyles.waykiMini.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = AppColors.slate
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Diferencia de tiempo
            Text(
                text = point.diffText,
                style = AppTextStyles.waykiLabelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                ),
                color = AppColors.ink
            )
        }
    }
}
