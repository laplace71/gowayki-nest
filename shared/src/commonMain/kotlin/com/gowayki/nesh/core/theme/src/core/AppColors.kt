package com.gowayki.nesh.core.theme.src.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Paleta minima de Wayki: solo 5 colores base y sus roles.
 *
 * Las variantes (containers, borders suaves, scrim, blobs) se derivan del
 * color base con opacidad (`Color.copy(alpha)`) en el punto de uso o en
 * [com.gowayki.nesh.core.theme.src.AppColorScheme] — no se declara un color
 * por variante.
 *
 * Cada propiedad lee la paleta vigente ([LocalWaykiPalette]), por lo que
 * cambia automaticamente al activar/desactivar el modo oscuro.
 */
object AppColors {

    private val palette: WaykiPalette
        @Composable @ReadOnlyComposable
        get() = LocalWaykiPalette.current

    // ── 5 colores base de la marca ───────────────────────────────────────────

    /** Fondo calido casi blanco (claro) / fondo profundo (oscuro). */
    val cream: Color
        @Composable @ReadOnlyComposable
        get() = palette.cream

    /** Malva lavanda: acciones y superficies suaves. */
    val mauve: Color
        @Composable @ReadOnlyComposable
        get() = palette.mauve

    /** Acero gris-azulado: bordes y secundario. */
    val steel: Color
        @Composable @ReadOnlyComposable
        get() = palette.steel

    /** Gris azulado: texto secundario. */
    val slate: Color
        @Composable @ReadOnlyComposable
        get() = palette.slate

    /** Pizarra: textos y tarjeta hero. */
    val ink: Color
        @Composable @ReadOnlyComposable
        get() = palette.ink

    /** Superficies suaves (tarjetas de Configuracion). */
    val card: Color
        @Composable @ReadOnlyComposable
        get() = palette.card

    /** Color de sombras (negro en oscuro para no generar halos). */
    val shadow: Color
        @Composable @ReadOnlyComposable
        get() = palette.shadow

    /** Indica si el tema activo es oscuro. */
    val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = palette.isDark

    /** Velo de carga: oscurece el contenido en ambos temas. */
    val overlayScrim: Color
        @Composable @ReadOnlyComposable
        get() = palette.overlayScrim

    /** Contenido sobre el velo de carga (siempre claro). */
    val onOverlay: Color
        @Composable @ReadOnlyComposable
        get() = palette.onOverlay

    // ── Roles semanticos (reutilizan solo los colores base) ──────────────────

    val primary: Color
        @Composable @ReadOnlyComposable
        get() = palette.primary

    val onPrimary: Color
        @Composable @ReadOnlyComposable
        get() = palette.onPrimary

    val secondary: Color
        @Composable @ReadOnlyComposable
        get() = palette.secondary

    val onSecondary: Color
        @Composable @ReadOnlyComposable
        get() = palette.onSecondary

    val tertiary: Color
        @Composable @ReadOnlyComposable
        get() = palette.tertiary

    val onTertiary: Color
        @Composable @ReadOnlyComposable
        get() = palette.onTertiary

    val background: Color
        @Composable @ReadOnlyComposable
        get() = palette.background

    val onBackground: Color
        @Composable @ReadOnlyComposable
        get() = palette.onBackground

    val surface: Color
        @Composable @ReadOnlyComposable
        get() = palette.surface

    val onSurface: Color
        @Composable @ReadOnlyComposable
        get() = palette.onSurface

    val surfaceVariant: Color
        @Composable @ReadOnlyComposable
        get() = palette.surfaceVariant

    val onSurfaceVariant: Color
        @Composable @ReadOnlyComposable
        get() = palette.onSurfaceVariant

    // Alias historico de Wayki: texto secundario/muted.
    val onSurfaceMuted: Color
        @Composable @ReadOnlyComposable
        get() = palette.onSurfaceMuted

    val outline: Color
        @Composable @ReadOnlyComposable
        get() = palette.outline

    val outlineVariant: Color
        @Composable @ReadOnlyComposable
        get() = palette.outlineVariant

    // Semantico de estado: se deriva del base mas oscuro (contraste sobre claro).
    val error: Color
        @Composable @ReadOnlyComposable
        get() = palette.error

    val onError: Color
        @Composable @ReadOnlyComposable
        get() = palette.onError

    val transparent: Color
        @Composable @ReadOnlyComposable
        get() = palette.transparent
}
