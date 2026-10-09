package com.gowayki.nesh.app.features.home.presentation.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.app.common.ui.shared.WaykiBackground
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import androidx.compose.foundation.clickable
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

@Composable
fun HomeScreen(
    onMenuClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onBack: () -> Unit = {},
    onSosClick: () -> Unit = {},
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
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
                    }
                )
            }
        }
    ) {
        WaykiBackground(withRoads = true, roadsAnimated = false) {
            
            // Massive dark blob behind the top section (Rectangle 4)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.68f) // Approx 597 out of 874
                    .background(
                        color = AppColors.slate, 
                        shape = RoundedCornerShape(bottomStart = 80.dp, bottomEnd = 80.dp)
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = AppSpacing.pageHorizontal, vertical = AppSpacing.sm)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WaykiBackButton(onClick = onBack)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(AppSpacing.buttonSecondaryHeight)
                                .background(AppColors.cream, CircleShape)
                                .clickable { coroutineScope.launch { drawerState.open() } },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FeatherIcons.Menu,
                                contentDescription = "Menu",
                                tint = AppColors.ink,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    Box(
                        modifier = Modifier
                            .size(AppSpacing.buttonSecondaryHeight)
                            .background(AppColors.cream, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FeatherIcons.Bell,
                            contentDescription = "Notifications",
                            tint = AppColors.ink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Bienvenido, ...",
                style = AppTextStyles.waykiTitle,
                color = AppColors.cream // Now white because it's on the slate background
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Card (Recorrido del dia)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = AppColors.ink.copy(alpha = 0.5f))
                    .background(AppColors.ink, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    // Thick vertical white line (Line 17)
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(AppColors.cream, RoundedCornerShape(2.dp))
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "tu recorrido del dia",
                            style = AppTextStyles.waykiLabelMedium,
                            color = AppColors.cream
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "5",
                                style = AppTextStyles.displayLarge.copy(fontWeight = FontWeight.Bold, color = AppColors.cream),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "min",
                                style = AppTextStyles.waykiTitle.copy(color = AppColors.cream),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        Text(
                            text = "adelante de Unidad 08",
                            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.cream
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Dato Gowayki Pulse • actualizado hace 45 s",
                            style = AppTextStyles.waykiMini,
                            color = AppColors.slate
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Divider line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(AppColors.slate.copy(alpha = 0.3f))
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "PRÓXIMO PUNTO DE CONTROL",
                            style = AppTextStyles.waykiMini,
                            color = AppColors.cream
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
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
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
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
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PillStat(top = "84%", bottom = "Cumplimiento")
                            PillStat(top = "6", bottom = "Puntos hoy")
                            PillStat(top = "L12", bottom = "Línea activa")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Floating Buses
            BusCard(
                title = "combi de atras",
                line = "linea 7",
                time = "margen de tiempo 5m",
                icon = FeatherIcons.ArrowLeft
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            BusCard(
                title = "combi de adelante",
                line = "linea 11",
                time = "margen de tiempo 8m",
                icon = FeatherIcons.ArrowRight
            )
            
            Spacer(modifier = Modifier.height(140.dp)) // space for bottom nav
        }
        
        // Bottom Navigation Bar anchored to the bottom
        WaykiBottomNav(
            selectedTab = 0, // "Inicio" is selected by default in Home
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
}

@Composable
fun WaykiBottomNav(
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.pageHorizontal, vertical = AppSpacing.pageHorizontal)
            .shadow(24.dp, RoundedCornerShape(32.dp), spotColor = AppColors.ink.copy(alpha = 0.4f))
            .background(
                color = AppColors.slate.copy(alpha = 0.95f), 
                shape = RoundedCornerShape(32.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = FeatherIcons.Home,
                label = "Inicio",
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )
            BottomNavItem(
                // Using Radio/Wifi equivalent from Feather
                icon = FeatherIcons.Radio,
                label = "Comunicacion",
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )
            BottomNavItem(
                // Using GitCommit or similar for the route icon
                icon = FeatherIcons.GitCommit,
                label = "Recorrido",
                isSelected = selectedTab == 2,
                onClick = { onTabSelected(2) }
            )
            BottomNavItem(
                icon = FeatherIcons.Map,
                label = "Mapa",
                isSelected = selectedTab == 3,
                onClick = { onTabSelected(3) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) AppColors.mauve else androidx.compose.ui.graphics.Color.Transparent
    val contentColor = if (isSelected) AppColors.ink else AppColors.cream

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = AppTextStyles.labelSmall.copy(fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                color = contentColor
            )
        }
    }
}

@Composable
private fun PillStat(top: String, bottom: String) {
    Box(
        modifier = Modifier
            .background(AppColors.mauve, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = top,
                style = AppTextStyles.waykiButtonLarge,
                color = AppColors.ink
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = bottom,
                style = AppTextStyles.labelSmall.copy(fontSize = 10.sp),
                color = AppColors.ink
            )
        }
    }
}

@Composable
private fun BusCard(
    title: String,
    line: String,
    time: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = AppColors.slate.copy(alpha = 0.4f), ambientColor = AppColors.slate.copy(alpha = 0.1f))
            .background(AppColors.mauve, RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column {
            // Top row: Title and Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    color = AppColors.ink
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppColors.ink,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Bottom row: Image and Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                // Bus image placeholder (large and rounded)
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(64.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .background(AppColors.slate.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🚌", fontSize = 32.sp)
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Line and Time
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = line,
                        style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.ink
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = time,
                        style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.ink
                    )
                }
            }
        }
    }
}


@Composable
fun WaykiSidebarContent(onClose: () -> Unit, onSosClick: () -> Unit = {}) {
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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AppColors.mauve, CircleShape)
                    .clickable(onClick = onClose),
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
            isSelected = false
        )
        DrawerMenuItem(
            icon = FeatherIcons.Radio, 
            title = "Comunicación", 
            subtitle = "Canal de la Línea 7", 
            isSelected = false
        )
        DrawerMenuItem(
            icon = FeatherIcons.GitCommit, 
            title = "Recorrido", 
            subtitle = "Tus recorridos y horarios", 
            isSelected = false
        )
        DrawerMenuItem(
            icon = FeatherIcons.Map, 
            title = "Mapa", 
            subtitle = "Informacion de tus rutas", 
            isSelected = true
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 5. Separador
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.slate.copy(alpha = 0.2f)))
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 6. Preferencias y ayuda
        DrawerSimpleItem(icon = FeatherIcons.Sliders, title = "Configuración")
        DrawerSimpleItem(icon = FeatherIcons.HelpCircle, title = "Soporte técnico")
        DrawerSimpleItem(icon = FeatherIcons.LogOut, title = "Cerrar sesión")
        
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp)) // ensure space if small screen
        
        // 7. SOS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.ink, RoundedCornerShape(16.dp))
                .clickable { onSosClick() }
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
    isSelected: Boolean
) {
    val bgColor = if (isSelected) AppColors.ink else androidx.compose.ui.graphics.Color.Transparent
    val contentColor = if (isSelected) AppColors.cream else AppColors.ink
    val subtitleColor = if (isSelected) AppColors.cream.copy(alpha = 0.7f) else AppColors.slate

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(16.dp))
            .clickable { }
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
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
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
