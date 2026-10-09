package com.gowayki.nesh.core.theme.src.core

import androidx.compose.ui.graphics.Color

/**
 * Paleta minima de Gowayki Nesh: solo 5 colores base y sus roles.
 *
 * Las variantes (containers, borders suaves, scrim, blobs) se derivan del
 * color base con opacidad (`Color.copy(alpha)`) en el punto de uso o en
 * [AppColorScheme] — no se declara un color por variante.
 */
object AppColors {

    // ── 5 colores base de la marca ───────────────────────────────────────────

    /** Fondo calido casi blanco. */
    val cream = Color(0xFFFFFDF6)

    /** Malva claro (lavanda): acciones, superficies suaves. */
    val mauve = Color(0xFFE2D4E0)

    /** Acero gris-azulado: bordes y secundario. */
    val steel = Color(0xFF949AB1)

    /** Gris azulado: texto secundario. */
    val slate = Color(0xFF7C7E9D)

    /** Pizarra oscura: textos y tarjeta hero. */
    val ink   = Color(0xFF2D2B4E)

    // ── Roles semanticos (reutilizan solo los 5 colores) ─────────────────────

    val primary   = mauve
    val onPrimary = ink

    val secondary = steel
    val onSecondary = ink

    val tertiary  = slate
    val onTertiary = cream

    val background = cream
    val onBackground = ink

    val surface = cream
    val onSurface = ink

    val surfaceVariant = mauve
    val onSurfaceVariant = slate

    // Alias historico de Wayki: texto secundario/muted.
    val onSurfaceMuted = slate

    val outline = steel
    val outlineVariant = steel.copy(alpha = 0.45f)

    // Semantico de estado: se deriva del base mas oscuro (contraste sobre claro).
    val error   = ink
    val onError = cream

    val transparent = Color(0x00000000)
}