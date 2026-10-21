package com.gowayki.nesh.app.features.home.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.ui.shared.WaykiBackground
import com.gowayki.nesh.app.common.ui.shared.WaykiBottomNav
import com.gowayki.nesh.app.common.ui.shared.WaykiReveal
import com.gowayki.nesh.app.common.ui.shared.WaykiSkeleton
import com.gowayki.nesh.app.features.map.presentation.SelectedRouteHolder
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
    val selectedRoute by SelectedRouteHolder.selected.collectAsState()

    // Simulate initial data load
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(800) // Brief skeleton flash
        isLoading = false
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
                // ── Header: fondo oscuro que se desplaza con el scroll ──
                WaykiReveal(delayMillis = 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = AppColors.ink,
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
                                text = "Carlos Mendoza",
                                style = AppTextStyles.waykiLabelMedium,
                                color = AppColors.cream.copy(alpha = 0.85f)
                            )
                            selectedRoute?.let { route ->
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
                                    text = route.name.substringBefore(" · "),
                                    style = AppTextStyles.waykiLabelMedium,
                                    color = AppColors.cream.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Etiqueta de sección, sobre el fondo oscuro
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

                        // Tarjeta principal
                        if (isLoading) {
                            WaykiSkeleton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                color = AppColors.cream.copy(alpha = 0.12f),
                                shape = AppRadius.card
                            )
                        } else {
                            RouteCard(
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
                    // Etiqueta de sección: combis
                    Text(
                        text = "Combis en tu línea",
                        style = AppTextStyles.waykiLabelMedium,
                        color = AppColors.ink
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tarjetas de combis
                    if (isLoading) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            WaykiSkeleton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(78.dp),
                                shape = AppRadius.card
                            )
                            WaykiSkeleton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(78.dp),
                                shape = AppRadius.card
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            WaykiReveal(delayMillis = 120) {
                                BusCard(
                                    title = "combi de atrás",
                                    line = "línea 7",
                                    time = "5m",
                                    icon = FeatherIcons.ArrowLeft,
                                    onClick = onRecorridoClick
                                )
                            }
                            WaykiReveal(delayMillis = 180) {
                                BusCard(
                                    title = "combi de adelante",
                                    line = "línea 11",
                                    time = "8m",
                                    icon = FeatherIcons.ArrowRight,
                                    onClick = onRecorridoClick
                                )
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
            .background(AppColors.cream, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AppColors.ink.copy(alpha = 0.15f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = AppColors.ink,
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
            .background(AppColors.ink, AppRadius.card)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(AppColors.cream, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "EN VIVO",
            style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
            color = AppColors.cream
        )
    }
}

// ---------- Tarjeta del recorrido del día ----------
@Composable
private fun RouteCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .shadow(4.dp, AppRadius.card, spotColor = AppColors.ink.copy(alpha = 0.12f))
            .background(AppColors.ink, AppRadius.card)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AppColors.cream.copy(alpha = 0.15f)),
                onClick = onClick
            )
            .padding(24.dp)
    ) {
        Column {
            // Tiempo adelante
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "5",
                    style = AppTextStyles.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppColors.cream
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "min",
                    style = AppTextStyles.waykiTitle.copy(color = AppColors.cream),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "adelante de Unidad 08",
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.cream
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Dato Gowayki Pulse • actualizado hace 45 s",
                style = AppTextStyles.waykiMini,
                color = AppColors.slate
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Separador
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.slate.copy(alpha = 0.3f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Próximo punto de control
            Text(
                text = "PRÓXIMO PUNTO DE CONTROL",
                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                color = AppColors.cream
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Av. Dolores / Cercado",
                        style = AppTextStyles.waykiButtonLarge,
                        color = AppColors.cream
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Programado: 08:47",
                        style = AppTextStyles.waykiMini,
                        color = AppColors.slate
                    )
                }
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "6",
                        style = AppTextStyles.waykiHeader,
                        color = AppColors.cream
                    )
                    Text(
                        text = "min",
                        style = AppTextStyles.waykiMini,
                        color = AppColors.cream
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Estadísticas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PillStat(top = "84%", bottom = "Cumplimiento", modifier = Modifier.weight(1f))
                PillStat(top = "6", bottom = "Puntos hoy", modifier = Modifier.weight(1f))
                PillStat(top = "L12", bottom = "Línea activa", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PillStat(top: String, bottom: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(AppColors.mauve.copy(alpha = 0.25f), AppRadius.card)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = top,
                style = AppTextStyles.waykiButtonLarge.copy(fontSize = 16.sp),
                color = AppColors.cream
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = bottom,
                style = AppTextStyles.labelSmall.copy(fontSize = 9.sp),
                color = AppColors.cream.copy(alpha = 0.7f)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, AppRadius.card, spotColor = AppColors.ink.copy(alpha = 0.12f))
            .background(AppColors.cream, AppRadius.card)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AppColors.ink.copy(alpha = 0.1f)),
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bus emoji as compact circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(AppColors.ink, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🚌", fontSize = 20.sp)
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


@Composable
fun WaykiSidebarContent(
    onClose: () -> Unit,
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
            text = "Carlos Mendoza", 
            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), 
            color = AppColors.ink
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Línea 7 • Unidad 08 • Arequipa", 
            style = AppTextStyles.waykiMini, 
            color = AppColors.slate
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
            isSelected = false,
            onClick = onInicioClick
        )
        DrawerMenuItem(
            icon = FeatherIcons.Radio, 
            title = "Comunicación", 
            subtitle = "Canal de la Línea 7", 
            isSelected = false,
            onClick = onCommunicationClick
        )
        DrawerMenuItem(
            icon = FeatherIcons.GitCommit, 
            title = "Recorrido", 
            subtitle = "Tus recorridos y horarios", 
            isSelected = false,
            onClick = onRecorridoClick
        )
        DrawerMenuItem(
            icon = FeatherIcons.Map, 
            title = "Mapa", 
            subtitle = "Informacion de tus rutas", 
            isSelected = true,
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
                .background(AppColors.ink, RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = sosInteractionSource,
                    indication = ripple(color = AppColors.cream.copy(alpha = 0.2f)),
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
                        color = AppColors.cream
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
        animationSpec = tween(150),
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
