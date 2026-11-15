package com.gowayki.nesh.app.common.ui.shared

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppRadius
import gowaykinesh.shared.generated.resources.Res
import gowaykinesh.shared.generated.resources.logo
import org.jetbrains.compose.resources.painterResource

/**
 * Marca de la app (logo del bus) que respira mientras se carga algo.
 *
 * Es una alternativa "de marca" al spinner genérico: fondo crema, el logo
 * con un pulso lento y un arco fino girando debajo. En gama baja se queda
 * estático para no gastar frames.
 */
@Composable
fun WaykiBrandLoader(
    message: String = "Un momento…",
    modifier: Modifier = Modifier,
) {
    val animated = WaykiPerf.heavyAnimationsEnabled

    val pulse: Float
    val spin: Float
    if (animated) {
        val transition = rememberInfiniteTransition(label = "brandLoader")
        val animatedPulse by transition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "brandLoaderPulse"
        )
        val animatedSpin by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "brandLoaderSpin"
        )
        pulse = animatedPulse
        spin = animatedSpin
    } else {
        pulse = 1f
        spin = 0f
    }

    Column(
        modifier = modifier
            .clip(AppRadius.card)
            .background(AppColors.cream)
            .padding(horizontal = 30.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.size(72.dp)) {
                if (animated) {
                    val stroke = 3.dp.toPx()
                    val diameter = size.minDimension - stroke
                    drawArc(
                        color = AppColors.primary.copy(alpha = 0.18f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = AppColors.primary,
                        startAngle = spin,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                } else {
                    val stroke = 3.dp.toPx()
                    val diameter = size.minDimension - stroke
                    drawArc(
                        color = AppColors.primary,
                        startAngle = -90f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null,
                colorFilter = ColorFilter.tint(AppColors.ink),
                modifier = Modifier
                    .size(38.dp)
                    .graphicsLayer {
                        if (animated) {
                            scaleX = pulse
                            scaleY = pulse
                        }
                    }
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = message,
            style = AppTextStyles.waykiMini.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
            color = AppColors.slate
        )
    }
}
