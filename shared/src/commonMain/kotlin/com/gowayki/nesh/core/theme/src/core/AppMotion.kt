package com.gowayki.nesh.core.theme.src.core

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing

/**
 * ðŸŽ¬ Tokens de animaciÃ³n/movimiento â€” duraciones y curvas.
 *
 * Duraciones â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * [instant]  â†’   0 ms  (sin animaciÃ³n)
 * [fast]     â†’ 150 ms  (micro-interacciones: ripple, icon swap)
 * [normal]   â†’ 250 ms  (transiciones estÃ¡ndar: fade, slide corto)
 * [medium]   â†’ 350 ms  (modales, bottom sheets)
 * [slow]     â†’ 500 ms  (transiciones de pÃ¡gina, onboarding)
 * [xslow]    â†’ 800 ms  (animaciones de carga, loaders)
 *
 * Curvas â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * [standard]   â†’ FastOutSlowIn   (bidireccional estÃ¡ndar)
 * [enter]      â†’ LinearOutSlowIn (elementos que entran)
 * [exit]       â†’ FastOutLinearIn (elementos que salen)
 * [emphasized] â†’ CubicBezier M3 (transiciones de pÃ¡gina M3)
 * [linear]     â†’ Linear          (loaders, progreso)
 *
 * Uso â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * ```kotlin
 * AnimatedVisibility(
 *     enter = fadeIn(animationSpec = tween(AppMotion.normalMs, easing = AppMotion.enter)),
 * )
 *
 * animate*AsState(
 *     targetValue = target,
 *     animationSpec = tween(AppMotion.normalMs, easing = AppMotion.standard)
 * )
 * ```
 */
object AppMotion {

    // â”€â”€ Duraciones (ms) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** 0 ms â€” sin animaciÃ³n, usar solo cuando sea imprescindible. */
    const val instantMs : Int = 0

    /** 150 ms â€” micro-interacciones (ripple, icon swap, toggle). */
    const val fastMs    : Int = 150

    /** 250 ms â€” transiciones estÃ¡ndar (fade, scale, slide corto). */
    const val normalMs  : Int = 250

    /** 350 ms â€” modales, bottom sheets, snackbars. */
    const val mediumMs  : Int = 350

    /** 500 ms â€” transiciones de pÃ¡gina, onboarding steps. */
    const val slowMs    : Int = 500

    /** 800 ms â€” animaciones de carga, loaders, ilustraciones. */
    const val xslowMs   : Int = 800

    // â”€â”€ Curvas â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Curva estÃ¡ndar â€” transiciones bidireccionales (expand/collapse). */
    val standard   : Easing = FastOutSlowInEasing

    /** Curva de entrada â€” elementos que aparecen en pantalla. */
    val enter      : Easing = LinearOutSlowInEasing

    /** Curva de salida â€” elementos que desaparecen de pantalla. */
    val exit       : Easing = FastOutLinearInEasing

    /**
     * Curva Material 3 "emphasized" â€” transiciones de pÃ¡gina principales.
     * Equivale a cubic-bezier(0.2, 0.0, 0.0, 1.0).
     */
    val emphasized : Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /** Curva lineal â€” para animaciones de progreso o loaders. */
    val linear     : Easing = LinearEasing
}

