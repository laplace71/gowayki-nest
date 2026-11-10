package com.gowayki.nesh.app.common.ui.shared

// Indicadores del PIN al estilo del bloqueo de Android/Pixel:
//  - puntos pequeños y discretos cuando están vacíos,
//  - al escribir, una forma geométrica (triángulo, cuadrado, pentágono,
//    hexágono…) aparece y se "pliega" hasta convertirse en el punto,
//  - el punto lleno hace un pequeño rebote,
//  - si el PIN falla, la fila hace un shake y todo se tiñe de error.
//
// Los colores siguen viniendo de la paleta Wayki (AppColors).

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val PIN_SLOT_DP = 20f

@Composable
fun WaykiPinDots(
    pinLength: Int,
    isError: Boolean = false,
    isBlocked: Boolean = false,
    maxLength: Int = 6,
    activeIndex: Int = pinLength,
    modifier: Modifier = Modifier,
) {
    // Animación de "forma que se convierte en punto" al escribir.
    var previousLength by remember { mutableIntStateOf(pinLength) }
    var burstIndex by remember { mutableIntStateOf(-1) }
    var burstSides by remember { mutableIntStateOf(4) }
    var burstSpin by remember { mutableFloatStateOf(0f) }
    val burst = remember { Animatable(0f) }

    LaunchedEffect(pinLength) {
        val grew = pinLength > previousLength
        previousLength = pinLength
        if (grew && !isError && !isBlocked) {
            burstIndex = pinLength - 1
            burstSides = SHAPES[Random.nextInt(SHAPES.size)]
            burstSpin = Random.nextFloat() * 70f - 35f
            burst.snapTo(0f)
            burst.animateTo(
                targetValue = 1f,
                animationSpec = tween(420, easing = FastOutSlowInEasing),
            )
            burstIndex = -1
        }
    }

    // Shake cuando el PIN es incorrecto.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(isError) {
        if (isError) {
            repeat(3) {
                shake.animateTo(7f, tween(45, easing = LinearEasing))
                shake.animateTo(-7f, tween(45, easing = LinearEasing))
            }
            shake.animateTo(0f, tween(45, easing = LinearEasing))
        }
    }

    Row(
        modifier = modifier.offset { IntOffset(shake.value.roundToInt(), 0) },
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(maxLength) { i ->
            val isFilled = i < pinLength
            val isActive = !isFilled &&
                !isBlocked &&
                i == activeIndex.coerceIn(0, maxLength - 1)

            val dotColor = when {
                isError -> AppColors.error
                isBlocked -> AppColors.onSurfaceMuted.copy(alpha = 0.35f)
                isFilled -> AppColors.onBackground
                else -> AppColors.onBackground.copy(alpha = if (isActive) 0.32f else 0.16f)
            }

            val dotSize by animateDpAsState(
                targetValue = when {
                    isFilled -> 14.dp
                    isActive -> 10.dp
                    else -> 8.dp
                },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "pinDotSize$i",
            )
            val dotScale by animateFloatAsState(
                targetValue = if (isFilled) 1f else 0.85f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "pinDotScale$i",
            )

            Box(
                modifier = Modifier.size(PIN_SLOT_DP.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .graphicsLayer {
                            scaleX = dotScale
                            scaleY = dotScale
                        }
                        .background(dotColor, CircleShape),
                )

                if (i == burstIndex) {
                    BurstShape(
                        progress = burst.value,
                        sides = burstSides,
                        spin = burstSpin,
                        color = dotColor,
                    )
                }
            }
        }
    }
}

// Forma geométrica que aparece al teclear y se encoge hasta el punto.
@Composable
private fun BurstShape(
    progress: Float,
    sides: Int,
    spin: Float,
    color: Color,
) {
    Canvas(Modifier.fillMaxSize()) {
        val t = progress.coerceIn(0f, 1f)

        // Entra con fuerza, se mantiene y luego se pliega hacia el punto.
        val appear = (t / 0.22f).coerceIn(0f, 1f)
        val shrink = if (t > 0.62f) {
            1f - ((t - 0.62f) / 0.38f) * 0.55f
        } else {
            1f
        }
        val scale = (0.55f + 0.45f * appear) * shrink
        val alpha = if (t > 0.70f) {
            (1f - (t - 0.70f) / 0.30f).coerceIn(0f, 1f)
        } else {
            1f
        }

        val radius = (size.minDimension / 2f) * 0.85f * scale
        if (radius <= 0f || alpha <= 0f) return@Canvas

        val path = polygonPath(
            sides = sides,
            radius = radius,
            rotationDeg = spin + t * 24f,
            center = Offset(size.width / 2f, size.height / 2f),
        )
        val paint = color.copy(alpha = color.alpha * alpha)

        // Relleno + borde redondeado = esquinas suaves tipo Android.
        drawPath(path = path, color = paint)
        drawPath(
            path = path,
            color = paint,
            style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round),
        )
    }
}

private fun polygonPath(
    sides: Int,
    radius: Float,
    rotationDeg: Float,
    center: Offset,
): Path {
    val path = Path()
    val step = 360f / sides
    repeat(sides) { n ->
        val angle = ((rotationDeg + n * step) * PI / 180.0).toFloat()
        val x = center.x + radius * cos(angle)
        val y = center.y + radius * sin(angle)
        if (n == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

// Triángulo, cuadrado, pentágono u hexágono, como la animación de Android 14.
private val SHAPES = intArrayOf(3, 4, 5, 6)
