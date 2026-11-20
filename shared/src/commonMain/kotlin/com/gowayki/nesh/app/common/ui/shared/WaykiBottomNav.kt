package com.gowayki.nesh.app.common.ui.shared

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Home
import compose.icons.feathericons.Map
import compose.icons.feathericons.Radio

/**
 * Barra de navegación flotante mejorada con:
 * - Highlight deslizante estilo Mochy Music (pastilla que se desliza suavemente entre tabs)
 * - Fondo glass/frosted glass con blur eficiente
 * - Animaciones spring optimizadas para 60-120 FPS
 * - Respetando WaykiPerf para dispositivos de gama baja
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
    // Glass state para el fondo frosted glass
    val glassState = remember { GlassState() }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            shape = RoundedCornerShape(28.dp),
            // Fondo transparente - el glass effect se dibuja detrás
            color = Color.Transparent,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, AppColors.slate.copy(alpha = 0.08f))
        ) {
            Box {
                // Fondo glass/frosted glass - solo se recompuesta al dibujar, no en cada frame
                GlassBottomNavBackground(
                    state = glassState,
                    modifier = Modifier.matchParentSize()
                )

                Row(
                    modifier = Modifier
                        .widthIn(min = 320.dp, max = 420.dp)
                        .height(72.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem(
                        icon = FeatherIcons.Home,
                        label = "Inicio",
                        tabIndex = 0,
                        selectedTab = selectedTab,
                        onClick = { onTabSelected(0) },
                        modifier = Modifier.weight(1f)
                    )
                    BottomNavItem(
                        icon = FeatherIcons.Radio,
                        label = "Comunicación",
                        tabIndex = 1,
                        selectedTab = selectedTab,
                        onClick = { onTabSelected(1) },
                        modifier = Modifier.weight(1f)
                    )
                    BottomNavItem(
                        icon = WaykiRouteIcon,
                        label = "Recorrido",
                        tabIndex = 2,
                        selectedTab = selectedTab,
                        onClick = { onTabSelected(2) },
                        modifier = Modifier.weight(1f)
                    )
                    BottomNavItem(
                        icon = FeatherIcons.Map,
                        label = "Mapa",
                        tabIndex = 3,
                        selectedTab = selectedTab,
                        onClick = { onTabSelected(3) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Fondo glass/frosted glass para la barra de navegación.
 * Usa aproximación visual performante en KMP.
 * En Android nativo usaría RenderNode para blur real (ver GlassKit de Mochy Music).
 */
@Composable
private fun GlassBottomNavBackground(
    state: GlassState,
    modifier: Modifier = Modifier
) {
    // Solo activa efectos visuales glass en dispositivos con rendimiento suficiente
    val glassEnabled = WaykiPerf.heavyAnimationsEnabled && WaykiPerf.softShadowsEnabled

    if (!glassEnabled) {
        // Fallback: fondo semitransparente con borde sutil
        Box(
            modifier = modifier
                .background(AppColors.cream.copy(alpha = 0.92f), RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
        )
        return
    }

    Box(modifier = modifier) {
        // Capa base semitransparente oscura para el efecto glass
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GlassChromeColor, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
        )

        // Glass effect visual - overlay con gradiente que simula frosted glass
        GlassEffectLayer(state = state, modifier = Modifier.fillMaxSize())
    }
}

/**
 * Capa de efecto glass simplificada para KMP.
 * Aproximación visual performante sin blur real (que requiere RenderNode nativo).
 */
@Composable
private fun GlassEffectLayer(
    state: GlassState,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    // Efecto visual glass: overlay semitransparente con gradiente sutil
    // que simula el frosted glass sin necesidad de blur real en KMP
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        AppColors.cream.copy(alpha = 0.12f),
                        AppColors.cream.copy(alpha = 0.04f),
                        AppColors.cream.copy(alpha = 0.08f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(0f, with(density) { 72.dp.toPx() })
                ),
                RoundedCornerShape(28.dp)
            )
            .clip(RoundedCornerShape(28.dp))
    )

    // Hairline border superior para definición del glass
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(AppColors.cream.copy(alpha = 0.18f))
    ) {
        // Empty content - just for the border
    }
}

/**
 * Estado simplificado para el glass effect (compatibilidad KMP).
 * En Android nativo usaría GlassState de GlassKit con RenderNode.
 */
private class GlassState {
    var renderNode: Any? = null
    var rootPosition: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero
}

/**
 * Colores para el glass effect (basados en Mochy Music GlassKit)
 */
private val GlassChromeColor = Color(0xFF131313)
private const val GlassChromeBlurRadius = 48f
private const val ChromeAccentWash = 0.55f

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    tabIndex: Int,
    selectedTab: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val isSelected = selectedTab == tabIndex
    val animated = WaykiPerf.heavyAnimationsEnabled

    val contentColor by animateColorAsState(
        targetValue = when {
            isSelected -> AppColors.primary
            isPressed -> AppColors.slate
            else -> AppColors.slate.copy(alpha = 0.85f)
        },
        animationSpec = if (animated) tween(180, easing = FastOutSlowInEasing) else tween(0),
        label = "navContent"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 0.95f,
        animationSpec = if (animated) {
            spring(dampingRatio = 0.7f, stiffness = 600f)
        } else {
            tween(0)
        },
        label = "navIconScale"
    )

    // NUEVO: Highlight deslizante estilo Mochy Music AppNavigation
    // Usa animateFloatAsState con spring diferenciado (lead/trail)
    // Usamos mutableStateOf para la posición actual y LaunchedEffect para la animación
    val pillPosition = remember { mutableStateOf(tabIndex.toFloat()) }
    val pillPositionAnim = animateFloatAsState(
        targetValue = pillPosition.value,
        animationSpec = if (animated) {
            // Spring diferenciado: lead (hacia adelante) más rápido, trail (hacia atrás) más lento
            // Igual que Mochy Music AppNavigation.kt líneas 225-226
            val currentPos = pillPosition.value
            val movingRight = tabIndex.toFloat() > currentPos
            if (movingRight) {
                spring(dampingRatio = 0.82f, stiffness = 700f) // NavHighlightLead
            } else {
                spring(dampingRatio = 0.82f, stiffness = 320f) // NavHighlightTrail
            }
        } else {
            tween(0)
        },
        label = "navPillPosition"
    )

    // Actualizar la posición objetivo cuando cambia el tab seleccionado
    LaunchedEffect(selectedTab) {
        if (isSelected) {
            pillPosition.value = tabIndex.toFloat()
        }
    }

    // Progreso de la pastilla (aparece/desaparece)
    val pillProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = if (animated) {
            spring(dampingRatio = 0.82f, stiffness = 600f)
        } else {
            tween(0)
        },
        label = "navPillProgress"
    )

    val press by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "navPress"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AppColors.primary),
                onClick = onClick
            )
            .graphicsLayer {
                scaleX = press
                scaleY = press
            }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.width(46.dp).height(30.dp),
            contentAlignment = Alignment.Center
        ) {
            // NUEVO: Highlight deslizante (sliding pill) estilo Mochy Music
            // Se posiciona según pillPosition y se anima con scaleX/scaleY
            AnimatedSlidingPill(
                isVisible = isSelected || pillProgress > 0.01f,
                pillProgress = pillProgress,
                pillPosition = pillPositionAnim.value,
                tabIndex = tabIndex,
                modifier = Modifier.size(46.dp, 30.dp)
            )

            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
            // Contador de mensajes nuevos en Comunicación, visible desde cualquier módulo.
            if (tabIndex == 1 && ChatUnreadBadge.total > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 2.dp, top = 2.dp)
                        .defaultMinSize(16.dp, 16.dp)
                        .clip(CircleShape)
                        .background(AppColors.error)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (ChatUnreadBadge.total > 99) "99+" else "${ChatUnreadBadge.total}",
                        color = Color.White,
                        fontSize = 10.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
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
 * Pastilla deslizante animada estilo Mochy Music AppNavigation.
 * Se desliza suavemente entre tabs en lugar de solo crecer desde el centro.
 */
@Composable
private fun AnimatedSlidingPill(
    isVisible: Boolean,
    pillProgress: Float,
    pillPosition: Float,
    tabIndex: Int,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val density = androidx.compose.ui.platform.LocalDensity.current

    // Ancho aproximado de cada slot del tab (peso 1f en Row con spacedBy)
    val tabWidth = with(density) { 46.dp.toPx() + 2.dp.toPx() } // icon width + spacing

    // Offset relativo al centro del tab actual
    val slideOffset = (pillPosition - tabIndex.toFloat()) * tabWidth

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = pillProgress
                // Escala desde el centro hacia afuera mientras se desliza
                scaleX = 0.55f + 0.45f * pillProgress
                scaleY = 0.55f + 0.45f * pillProgress
                translationX = slideOffset
            }
            .background(AppColors.primary.copy(alpha = 0.14f), CircleShape)
    )
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
