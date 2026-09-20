package com.gowayki.nesh.core.theme.src.core

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ðŸ“ Escala de espaciado (spacing scale) de la app.
 *
 * Escala â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * [none]  â†’   0 dp
 * [xxs]   â†’   2 dp
 * [xs]    â†’   4 dp
 * [sm]    â†’   8 dp
 * [md]    â†’  12 dp
 * [lg]    â†’  16 dp
 * [xl]    â†’  24 dp
 * [xxl]   â†’  32 dp
 * [xxxl]  â†’  48 dp
 * [huge]  â†’  64 dp
 *
 * Uso â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * ```kotlin
 * Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm)
 * Spacer(modifier = Modifier.height(AppSpacing.xl))
 * Arrangement.spacedBy(AppSpacing.md)
 * ```
 */
object AppSpacing {

    val none : Dp =  0.dp
    val xxs  : Dp =  2.dp
    val xs   : Dp =  4.dp
    val sm   : Dp =  8.dp
    val md   : Dp = 12.dp
    val lg   : Dp = 16.dp
    val xl   : Dp = 24.dp
    val xxl  : Dp = 32.dp
    val xxxl : Dp = 48.dp
    val huge : Dp = 64.dp

    // â”€â”€ SemÃ¡nticos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Padding interno de cards. */
    val cardPadding     : Dp = lg

    /** Padding horizontal global de la pantalla. */
    val screenPadding   : Dp = lg

    /** SeparaciÃ³n entre Ã­tems de una lista. */
    val listItemSpacing : Dp = sm

    /** SeparaciÃ³n entre Ã­tems de una fila de chips. */
    val chipSpacing     : Dp = xs

    /** SeparaciÃ³n entre Ã­cono y texto en botones/items. */
    val iconText        : Dp = sm

    /** Padding interno de inputs y text fields. */
    val inputPadding    : Dp = md

    /** Margen inferior del FAB (above bottom nav). */
    val fabBottomMargin : Dp = xxl

    /** Alto mÃ­nimo de un Ã­tem de lista tÃ¡ctil. */
    val minTouchTarget  : Dp = 48.dp
}

