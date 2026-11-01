package com.gowayki.nesh.app.common.ui.shared

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.FeatherIcons
import compose.icons.feathericons.Home
import compose.icons.feathericons.Map
import compose.icons.feathericons.Radio

/**
 * Barra de navegación flotante, migrada del diseño de la app original:
 * tarjeta M3 con esquinas redondeadas, sombra suave, pastilla primary en el
 * ítem activo e icono que crece al seleccionarse.
 *
 * Mantiene la misma API (`selectedTab` / `onTabSelected`) para no tocar las
 * pantallas que ya la usan.
 */
@Composable
fun WaykiBottomNav(
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(28.dp),
        color = AppColors.cream,
        shadowElevation = 8.dp,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = FeatherIcons.Home,
                label = "Inicio",
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                modifier = Modifier.weight(1f)
            )
            BottomNavItem(
                icon = FeatherIcons.Radio,
                label = "Comunicación",
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f)
            )
            BottomNavItem(
                icon = WaykiRouteIcon,
                label = "Recorrido",
                isSelected = selectedTab == 2,
                onClick = { onTabSelected(2) },
                modifier = Modifier.weight(1f)
            )
            BottomNavItem(
                icon = FeatherIcons.Map,
                label = "Mapa",
                isSelected = selectedTab == 3,
                onClick = { onTabSelected(3) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val contentColor by animateColorAsState(
        targetValue = when {
            isSelected -> AppColors.primary
            isPressed -> AppColors.slate
            else -> AppColors.slate.copy(alpha = 0.85f)
        },
        animationSpec = tween(280, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "navContent"
    )
    val iconSize by animateDpAsState(
        targetValue = if (isSelected) 26.dp else 22.dp,
        animationSpec = tween(280, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "navIconSize"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AppColors.primary),
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (isSelected) {
                        Modifier.background(AppColors.primary.copy(alpha = 0.14f), CircleShape)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 14.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = AppTextStyles.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = contentColor,
            maxLines = 1
        )
    }
}

/**
 * Icono "Recorrido" (route) tomado del diseño de Figma.
 * Se construye como ImageVector a partir del path exacto (trazo) para lograr
 * paridad 1:1 con el diseño sin depender de iconos externos.
 */
private val WaykiRouteIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "WaykiRoute",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 28f,
        viewportHeight = 28f
    ).addPath(
        pathData = PathData {
            // Círculo inferior izquierdo.
            moveTo(10.5f, 22.167f)
            curveTo(10.5f, 24.1001f, 8.933f, 25.6672f, 7f, 25.6672f)
            curveTo(5.067f, 25.6672f, 3.5f, 24.1001f, 3.5f, 22.167f)
            curveTo(3.5f, 20.2338f, 5.067f, 18.6667f, 7f, 18.6667f)
            curveTo(8.933f, 18.6667f, 10.5f, 20.2338f, 10.5f, 22.167f)
            close()
            // Serpenteo central.
            moveTo(10.5f, 22.167f)
            horizontalLineTo(20.4167f)
            curveTo(21.4996f, 22.167f, 22.5382f, 21.7367f, 23.304f, 20.9709f)
            curveTo(24.0698f, 20.205f, 24.5f, 19.1663f, 24.5f, 18.0833f)
            curveTo(24.5f, 17.0002f, 24.0698f, 15.9615f, 23.304f, 15.1957f)
            curveTo(22.5382f, 14.4299f, 21.4996f, 13.9996f, 20.4167f, 13.9996f)
            horizontalLineTo(7.58333f)
            curveTo(6.50037f, 13.9996f, 5.46175f, 13.5694f, 4.69598f, 12.8036f)
            curveTo(3.93021f, 12.0377f, 3.5f, 10.999f, 3.5f, 9.91597f)
            curveTo(3.5f, 8.83292f, 3.93021f, 7.79422f, 4.69598f, 7.02839f)
            curveTo(5.46175f, 6.26255f, 6.50037f, 5.83231f, 7.58333f, 5.83231f)
            horizontalLineTo(17.5f)
            // Círculo superior derecho.
            moveTo(17.5f, 5.83231f)
            curveTo(17.5f, 7.76546f, 19.067f, 9.33259f, 21f, 9.33259f)
            curveTo(22.933f, 9.33259f, 24.5f, 7.76546f, 24.5f, 5.83231f)
            curveTo(24.5f, 3.89916f, 22.933f, 2.33203f, 21f, 2.33203f)
            curveTo(19.067f, 2.33203f, 17.5f, 3.89916f, 17.5f, 5.83231f)
            close()
        },
        stroke = SolidColor(AppColors.white),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()
}
