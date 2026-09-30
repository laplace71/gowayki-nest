package com.gowayki.nesh.core.theme.src.core

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Breakpoints de la app para diseno responsive.
 *
 * Inspirado en Material Design Adaptive Layout:
 *
 * Escala
 * [compact]   ->  < 600 dp  (telefonos portrait)
 * [medium]    ->  600-839 dp (telefonos landscape / tablets pequenas)
 * [expanded]  ->  >= 840 dp  (tablets y desktop)
 *
 * Uso
 * ```kotlin
 * val windowSize = LocalWindowSizeClass.current
 * val isCompact = windowSize.widthSizeClass == WindowWidthSizeClass.Compact
 *
 * // O con WindowSizeClass:
 * when (AppBreakpoints.from(containerWidth)) {
 *     AppBreakpoints.Tier.COMPACT  -> SinglePaneLayout()
 *     AppBreakpoints.Tier.MEDIUM   -> TwoPaneLayout()
 *     AppBreakpoints.Tier.EXPANDED -> ThreePaneLayout()
 * }
 * ```
 */
object AppBreakpoints {

    /** Maximo ancho de pantalla "compact" (< 600 dp). */
    val compact  : Dp = 600.dp

    /** Maximo ancho de pantalla "medium" (< 840 dp). */
    val medium   : Dp = 840.dp

    /** Ancho minimo de pantalla "expanded" (>= 840 dp). */
    val expanded : Dp = 840.dp

    // --- Columnas de grilla sugeridas ---

    /** Columnas de grilla para pantalla compact. */
    const val COLUMNS_COMPACT  = 4

    /** Columnas de grilla para pantalla medium. */
    const val COLUMNS_MEDIUM   = 8

    /** Columnas de grilla para pantalla expanded. */
    const val COLUMNS_EXPANDED = 12

    // --- Helpers ---

    enum class Tier { COMPACT, MEDIUM, EXPANDED }

    /** Devuelve el [Tier] correspondiente al [width] dado. */
    fun from(width: Dp): Tier = when {
        width < compact  -> Tier.COMPACT
        width < expanded -> Tier.MEDIUM
        else             -> Tier.EXPANDED
    }
}