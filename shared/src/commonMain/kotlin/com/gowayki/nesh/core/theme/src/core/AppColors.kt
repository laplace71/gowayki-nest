package com.gowayki.nesh.core.theme.src.core

import androidx.compose.ui.graphics.Color

/**
 * Paleta original de Gowayki Nesh — gama lila-azul.
 *
 * Basada en la escala original (#FFFDF6 → #4C5372), reforzada un punto
 * para que los colores tengan presencia sin saturar.
 *
 * | Token        | Hex       | Origen     | Uso principal                          |
 * |-------------|-----------|------------|---------------------------------------|
 * | background  | #FFFDF6   | Original 1 | Fondo cálido de la app                |
 * | cream       | #FFFFFF   | —          | Tarjetas (blanco puro, sobresalen)     |
 * | primary     | #7B82A0   | Original 3 | Acciones, botones, elementos primarios|
 * | secondary   | #D5C5D5   | Original 2 | Acento lila, superficies suaves        |
 * | slate       | #6B6E8A   | Original 4 | Texto secundario, íconos apagados      |
 * | neutral/ink | #3D4365   | Original 5 | Texto principal, contraste            |
 */
object AppColors {
    /** Lila-azul medio — acciones y elementos primarios de toda la app. */
    val primary = Color(0xFF7B82A0)
    /** Lila suave — acento y superficies suaves. */
    val secondary = Color(0xFFD5C5D5)
    /** Lila suave — detalle (alias de secondary). */
    val accent = Color(0xFFD5C5D5)
    /** Azul-gris profundo — texto principal y fondo oscuro. */
    val neutral = Color(0xFF3D4365)
    val white = Color(0xFFFFFFFF)

    /** Lila muy claro — fondo suave derivado del secondary. */
    val softSage = Color(0xFFF1ECF0)
    /** Crema cálido — fondo general de la app. */
    val appBackground = Color(0xFFFFFDF6)

    /** Blanco puro — tarjetas y contenedores que destacan sobre el fondo. */
    val cream = white
    // Alias heredado para que los componentes que todavía usan `mauve`
    // también adopten el lila-azul sin perder la paleta compartida.
    val mauve = primary
    val steel = secondary
    val slate = Color(0xFF6B6E8A)
    val ink = neutral

    val onPrimary = white
    val onSecondary = ink
    val tertiary = slate
    val onTertiary = white
    val background = appBackground
    val onBackground = ink
    val surface = white
    val onSurface = ink
    val surfaceVariant = softSage
    val onSurfaceVariant = slate
    val onSurfaceMuted = slate
    val outline = Color(0xFF7B82A0)
    val outlineVariant = outline.copy(alpha = 0.5f)
    val error = Color(0xFFB42318)
    val onError = white
    val transparent = Color(0x00000000)
}
