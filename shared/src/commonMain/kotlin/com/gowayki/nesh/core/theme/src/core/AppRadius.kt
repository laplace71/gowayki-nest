package com.gowayki.nesh.core.theme.src.core

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * ðŸ”² Radios de borde (border radius) de la app.
 *
 * Escala â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * [none]  â†’   0 dp  (sin redondeo)
 * [xs]    â†’   4 dp
 * [sm]    â†’   8 dp
 * [md]    â†’  12 dp  (cards estÃ¡ndar)
 * [lg]    â†’  16 dp  (modales, sheets)
 * [xl]    â†’  24 dp
 * [full]  â†’ 999 dp  (pill / cÃ­rculo)
 *
 * Uso â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * ```kotlin
 * Surface(shape = AppRadius.cardShape)
 * Box(modifier = Modifier.clip(AppRadius.md))
 * ```
 */
object AppRadius {

    // â”€â”€ Escala base â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    val none  = RoundedCornerShape(0.dp)
    val xs    = RoundedCornerShape(4.dp)
    val sm    = RoundedCornerShape(8.dp)
    val md    = RoundedCornerShape(12.dp)
    val lg    = RoundedCornerShape(16.dp)
    val xl    = RoundedCornerShape(24.dp)
    val full  = CircleShape

    // â”€â”€ SemÃ¡nticos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Radio estÃ¡ndar para cards. */
    val card   : Shape = md

    /** Radio estÃ¡ndar para botones. */
    val button : Shape = sm

    /** Radio estÃ¡ndar para inputs / text fields. */
    val input  : Shape = sm

    /** Radio estÃ¡ndar para modales y bottom sheets (solo borde superior). */
    val modal  : Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

    /** Radio estÃ¡ndar para chips y badges. */
    val chip   : Shape = full

    /** Radio estÃ¡ndar para snackbars. */
    val snackbar: Shape = sm

    /** Radio estÃ¡ndar para tooltips. */
    val tooltip : Shape = xs

    /** Radio estÃ¡ndar para FABs extendidos. */
    val fabExtended: Shape = full
}

