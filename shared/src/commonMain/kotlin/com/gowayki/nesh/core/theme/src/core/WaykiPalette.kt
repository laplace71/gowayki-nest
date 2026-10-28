package com.gowayki.nesh.core.theme.src.core

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Paleta viva de Wayki.
 *
 * Los nombres (`cream`, `mauve`, `steel`, `slate`, `ink`) son los 5 colores
 * base de la marca: sus valores cambian entre el tema claro y el oscuro, pero
 * los roles de contraste se mantienen (fondo/texto, superficie/contenido).
 */
@Immutable
data class WaykiPalette(
    val isDark: Boolean,
    val cream: Color,
    val mauve: Color,
    val steel: Color,
    val slate: Color,
    val ink: Color,
    /** Superficies suaves (tarjetas de Configuración). */
    val card: Color,
    /** Color de las sombras (siempre oscuro). */
    val shadow: Color,
) {
    val primary get() = mauve
    val onPrimary get() = ink

    val secondary get() = steel
    val onSecondary get() = ink

    val tertiary get() = slate
    val onTertiary get() = cream

    val background get() = cream
    val onBackground get() = ink

    val surface get() = cream
    val onSurface get() = ink

    val surfaceVariant get() = mauve
    val onSurfaceVariant get() = slate

    // Alias historico de Wayki: texto secundario/muted.
    val onSurfaceMuted get() = slate

    val outline get() = steel
    val outlineVariant get() = steel.copy(alpha = 0.45f)

    // Semantico de estado: se deriva del base mas oscuro (contraste sobre claro).
    val error get() = ink
    val onError get() = cream

    /** Velo de carga: oscurece el contenido en ambos temas. */
    val overlayScrim get() = shadow.copy(alpha = if (isDark) 0.55f else 0.25f)

    /** Contenido sobre el velo de carga (siempre claro). */
    val onOverlay get() = if (isDark) ink else cream

    val transparent get() = Color.Transparent
}

/** Tema claro original de Figma. */
val LightPalette = WaykiPalette(
    isDark = false,
    cream = Color(0xFFFFFDF6),
    mauve = Color(0xFFE2D4E0),
    steel = Color(0xFF949AB1),
    slate = Color(0xFF7C7E9D),
    ink = Color(0xFF2D2B4E),
    card = Color(0xFFF2EDF2),
    shadow = Color(0xFF2D2B4E),
)

/**
 * Tema oscuro: invierte los roles de contraste (fondo oscuro / texto claro)
 * manteniendo la identidad lavanda de la marca.
 */
val DarkPalette = WaykiPalette(
    isDark = true,
    cream = Color(0xFF141221), // fondo
    mauve = Color(0xFF3E3866), // superficies y acciones
    steel = Color(0xFF5C5678), // bordes
    slate = Color(0xFFA9A5C9), // texto secundario
    ink = Color(0xFFF3F0FA),   // texto principal
    card = Color(0xFF1E1B2E),  // tarjetas suaves
    shadow = Color(0xFF000000),
)

/** Paleta disponible para toda la composición. */
val LocalWaykiPalette = staticCompositionLocalOf { LightPalette }
