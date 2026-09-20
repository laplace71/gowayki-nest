package com.gowayki.nesh.core.theme.src.core

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ðŸ”¤ TamaÃ±os de Ã­conos de la app.
 *
 * Escala â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * [xs]    â†’  12 dp  (badges, indicators)
 * [sm]    â†’  16 dp  (trailing icons en chips/listas)
 * [md]    â†’  24 dp  (tamaÃ±o estÃ¡ndar Material)
 * [lg]    â†’  32 dp  (leading icons grandes)
 * [xl]    â†’  48 dp  (ilustraciones / empty states pequeÃ±os)
 * [xxl]   â†’  64 dp  (ilustraciones / avatares)
 * [huge]  â†’  96 dp  (empty state illustrations)
 *
 * Uso â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * ```kotlin
 * Icon(
 *     imageVector = Icons.Default.Map,
 *     modifier = Modifier.size(AppIconSizes.md)
 * )
 * ```
 */
object AppIconSizes {

    val xs   : Dp = 12.dp
    val sm   : Dp = 16.dp
    val md   : Dp = 24.dp
    val lg   : Dp = 32.dp
    val xl   : Dp = 48.dp
    val xxl  : Dp = 64.dp
    val huge : Dp = 96.dp

    // â”€â”€ SemÃ¡nticos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Ãcono dentro de campos de texto. */
    val textField   : Dp = md

    /** Ãcono en Ã­tem de lista. */
    val listItem    : Dp = md

    /** Ãcono en barra de navegaciÃ³n inferior. */
    val navBar      : Dp = md

    /** Ãcono en botones de acciÃ³n flotante (FAB). */
    val fab         : Dp = md

    /** Ãcono en botones pequeÃ±os / icon buttons. */
    val iconButton  : Dp = md

    /** Avatar de usuario pequeÃ±o. */
    val avatarSm    : Dp = xl

    /** Avatar de usuario grande. */
    val avatarLg    : Dp = xxl
}

