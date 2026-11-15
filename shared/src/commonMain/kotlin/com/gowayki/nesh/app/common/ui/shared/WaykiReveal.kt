package com.gowayki.nesh.app.common.ui.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Revela contenido con una animación suave de entrada (fade + slide vertical).
 *
 * Importante: antes se usaba `AnimatedVisibility(visible = true)`, que en la
 * primera composición NO anima nada (el estado inicial ya es visible). Ahora
 * arrancamos desde [MutableTransitionState] en `false` y lo encendemos en el
 * primer frame, así la entrada realmente se ve.
 *
 * En gama baja la animación se desactiva para evitar recomposición por frame
 * y mantener fluidez.
 */
@Composable
fun WaykiReveal(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    offsetY: Dp = 24.dp,
    content: @Composable () -> Unit,
) {
    if (!WaykiPerf.heavyAnimationsEnabled) {
        Box(modifier = modifier) { content() }
        return
    }

    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { visibleState.targetState = true }

    val offsetPx = with(LocalDensity.current) { offsetY.roundToPx() }

    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 420,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            )
        ) + slideInVertically(
            initialOffsetY = { offsetPx },
            animationSpec = tween(
                durationMillis = 520,
                delayMillis = delayMillis,
                easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
            )
        ),
    ) {
        content()
    }
}
