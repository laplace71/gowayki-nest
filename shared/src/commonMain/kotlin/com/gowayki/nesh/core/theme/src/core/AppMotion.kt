package com.gowayki.nesh.core.theme.src.core

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing

/**
 * Tokens de animacion/movimiento: duraciones y curvas.
 *
 * Duraciones
 * [instant]  ->   0 ms  (sin animacion)
 * [fast]     -> 150 ms  (micro-interacciones: ripple, icon swap)
 * [normal]   -> 250 ms  (transiciones estandar: fade, slide corto)
 * [medium]   -> 350 ms  (modales, bottom sheets)
 * [slow]     -> 500 ms  (transiciones de pagina, onboarding)
 * [xslow]    -> 800 ms  (animaciones de carga, loaders)
 *
 * Curvas
 * [standard]   -> FastOutSlowIn   (bidireccional estandar)
 * [enter]      -> LinearOutSlowIn (elementos que entran)
 * [exit]       -> FastOutLinearIn (elementos que salen)
 * [emphasized] -> CubicBezier M3 (transiciones de pagina M3)
 * [linear]     -> Linear          (loaders, progreso)
 *
 * Uso
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

    // --- Duraciones (ms) ---

    /** 0 ms: sin animacion, usar solo cuando sea imprescindible. */
    const val instantMs : Int = 0

    /** 150 ms: micro-interacciones (ripple, icon swap, toggle). */
    const val fastMs    : Int = 150

    /** 250 ms: transiciones estandar (fade, scale, slide corto). */
    const val normalMs  : Int = 250

    /** 350 ms: modales, bottom sheets, snackbars. */
    const val mediumMs  : Int = 350

    /** 500 ms: transiciones de pagina, onboarding steps. */
    const val slowMs    : Int = 500

    /** 800 ms: animaciones de carga, loaders, ilustraciones. */
    const val xslowMs   : Int = 800

    // --- Curvas ---

    /** Curva estandar: transiciones bidireccionales (expand/collapse). */
    val standard   : Easing = FastOutSlowInEasing

    /** Curva de entrada: elementos que aparecen en pantalla. */
    val enter      : Easing = LinearOutSlowInEasing

    /** Curva de salida: elementos que desaparecen de pantalla. */
    val exit       : Easing = FastOutLinearInEasing

    /**
     * Curva Material 3 "emphasized": transiciones de pagina principales.
     * Equivale a cubic-bezier(0.2, 0.0, 0.0, 1.0).
     */
    val emphasized : Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /** Curva lineal: para animaciones de progreso o loaders. */
    val linear     : Easing = LinearEasing
}