package com.gowayki.nesh.core.theme.src.core

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ðŸ“± Breakpoints de la app para diseÃ±o responsive.
 *
 * Inspirado en Material Design Adaptive Layout:
 *
 * Escala â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * [compact]   â†’  < 600 dp  (telÃ©fonos portrait)
 * [medium]    â†’  600â€“839 dp  (telÃ©fonos landscape / tablets pequeÃ±as)
 * [expanded]  â†’  â‰¥ 840 dp  (tablets y desktop)
 *
 * Uso â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

    /** MÃ¡ximo ancho de pantalla "compact" (< 600 dp). */
    val compact  : Dp = 600.dp

    /** MÃ¡ximo ancho de pantalla "medium" (< 840 dp). */
    val medium   : Dp = 840.dp

    /** Ancho mÃ­nimo de pantalla "expanded" (â‰¥ 840 dp). */
    val expanded : Dp = 840.dp

    // â”€â”€ Columnas de grilla sugeridas â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Columnas de grilla para pantalla compact. */
    const val COLUMNS_COMPACT  = 4

    /** Columnas de grilla para pantalla medium. */
    const val COLUMNS_MEDIUM   = 8

    /** Columnas de grilla para pantalla expanded. */
    const val COLUMNS_EXPANDED = 12

    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    enum class Tier { COMPACT, MEDIUM, EXPANDED }

    /** Devuelve el [Tier] correspondiente al [width] dado. */
    fun from(width: Dp): Tier = when {
        width < compact  -> Tier.COMPACT
        width < expanded -> Tier.MEDIUM
        else             -> Tier.EXPANDED
    }
}

