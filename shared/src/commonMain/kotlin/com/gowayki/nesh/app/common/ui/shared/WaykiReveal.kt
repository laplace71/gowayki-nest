package com.gowayki.nesh.app.common.ui.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Revela contenido con una animación suave de entrada (fade + slide vertical).
 *
 * En gama baja (Helio G99 / Mali-G57) la animación se desactiva para evitar
 * recomposición por frame y mantener fluidez.
 */
@Composable
fun WaykiReveal(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit,
) {
    if (!WaykiPerf.heavyAnimationsEnabled) {
        // Gama baja: sin animación (solo layout).
        Box(modifier = modifier) { content() }
        return
    }
    AnimatedVisibility(
        visible = true,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(350, delayMillis = delayMillis)) + slideInVertically(
            initialOffsetY = { 50 },
            animationSpec = tween(350, delayMillis = delayMillis),
        ),
    ) {
        content()
    }
}
