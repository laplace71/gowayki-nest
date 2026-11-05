package com.gowayki.nesh.core.theme.src.core

import androidx.compose.ui.graphics.Color
import com.gowayki.nesh.core.theme.ThemeManager

/**
 * Paleta Nesh — soporta modo claro y oscuro.
 *
 * Los tokens se resuelven dinámicamente según [ThemeManager.isDark], por lo que
 * los componentes que referencian `AppColors.xxx` se recomponen al cambiar de tema.
 */
object AppColors {

    private val isDark: Boolean get() = ThemeManager.isDark.value

    // ---------- Modo claro ----------
    private val lightPrimary = Color(0xFF7B82A0)
    private val lightSecondary = Color(0xFFD5C5D5)
    private val lightAccent = Color(0xFFD5C5D5)
    private val lightNeutral = Color(0xFF3D4365)
    private val lightWhite = Color(0xFFFFFFFF)
    private val lightSoftSage = Color(0xFFF1ECF0)
    private val lightAppBackground = Color(0xFFFFFDF6)
    private val lightCream = Color(0xFFFFFFFF)
    private val lightMauve = Color(0xFF7B82A0)
    private val lightSteel = Color(0xFFD5C5D5)
    private val lightSlate = Color(0xFF6B6E8A)
    private val lightInk = Color(0xFF3D4365)
    private val lightOnPrimary = Color(0xFFFFFFFF)
    private val lightOnSecondary = Color(0xFF3D4365)
    private val lightTertiary = Color(0xFF6B6E8A)
    private val lightOnTertiary = Color(0xFFFFFFFF)
    private val lightBackground = Color(0xFFFFFDF6)
    private val lightOnBackground = Color(0xFF3D4365)
    private val lightSurface = Color(0xFFFFFFFF)
    private val lightOnSurface = Color(0xFF3D4365)
    private val lightSurfaceVariant = Color(0xFFF1ECF0)
    private val lightOnSurfaceVariant = Color(0xFF6B6E8A)
    private val lightOnSurfaceMuted = Color(0xFF6B6E8A)
    private val lightOutline = Color(0xFF7B82A0)
    private val lightOutlineVariant = Color(0xFF7B82A0).copy(alpha = 0.5f)
    private val lightError = Color(0xFFB42318)
    private val lightOnError = Color(0xFFFFFFFF)

    // ---------- Modo oscuro ----------
    private val darkPrimary = Color(0xFF9BA3C0)
    private val darkSecondary = Color(0xFF4A4560)
    private val darkAccent = Color(0xFF4A4560)
    private val darkNeutral = Color(0xFFE8E9F2)
    private val darkWhite = Color(0xFFFFFFFF)
    private val darkSoftSage = Color(0xFF2A2B38)
    private val darkAppBackground = Color(0xFF12131A)
    private val darkCream = Color(0xFF1E1F2A)
    private val darkMauve = Color(0xFF9BA3C0)
    private val darkSteel = Color(0xFF4A4560)
    private val darkSlate = Color(0xFFA0A3B8)
    private val darkInk = Color(0xFFE8E9F2)
    private val darkOnPrimary = Color(0xFF12131A)
    private val darkOnSecondary = Color(0xFFE8E9F2)
    private val darkTertiary = Color(0xFFA0A3B8)
    private val darkOnTertiary = Color(0xFF12131A)
    private val darkBackground = Color(0xFF12131A)
    private val darkOnBackground = Color(0xFFE8E9F2)
    private val darkSurface = Color(0xFF1E1F2A)
    private val darkOnSurface = Color(0xFFE8E9F2)
    private val darkSurfaceVariant = Color(0xFF2A2B38)
    private val darkOnSurfaceVariant = Color(0xFFA0A3B8)
    private val darkOnSurfaceMuted = Color(0xFFA0A3B8)
    private val darkOutline = Color(0xFF9BA3C0)
    private val darkOutlineVariant = Color(0xFF9BA3C0).copy(alpha = 0.5f)
    private val darkError = Color(0xFFCF6679)
    private val darkOnError = Color(0xFF12131A)

    // ---------- Tokens públicos ----------

    val primary: Color get() = if (isDark) darkPrimary else lightPrimary
    val secondary: Color get() = if (isDark) darkSecondary else lightSecondary
    val accent: Color get() = if (isDark) darkAccent else lightAccent
    val neutral: Color get() = if (isDark) darkNeutral else lightNeutral
    val white: Color get() = lightWhite
    val softSage: Color get() = if (isDark) darkSoftSage else lightSoftSage
    val appBackground: Color get() = if (isDark) darkAppBackground else lightAppBackground

    val cream: Color get() = if (isDark) darkCream else lightCream
    val mauve: Color get() = if (isDark) darkMauve else lightMauve
    val steel: Color get() = if (isDark) darkSteel else lightSteel
    val slate: Color get() = if (isDark) darkSlate else lightSlate
    val ink: Color get() = if (isDark) darkInk else lightInk

    val onPrimary: Color get() = if (isDark) darkOnPrimary else lightOnPrimary
    val onSecondary: Color get() = if (isDark) darkOnSecondary else lightOnSecondary
    val tertiary: Color get() = if (isDark) darkTertiary else lightTertiary
    val onTertiary: Color get() = if (isDark) darkOnTertiary else lightOnTertiary
    val background: Color get() = if (isDark) darkBackground else lightBackground
    val onBackground: Color get() = if (isDark) darkOnBackground else lightOnBackground
    val surface: Color get() = if (isDark) darkSurface else lightSurface
    val onSurface: Color get() = if (isDark) darkOnSurface else lightOnSurface
    val surfaceVariant: Color get() = if (isDark) darkSurfaceVariant else lightSurfaceVariant
    val onSurfaceVariant: Color get() = if (isDark) darkOnSurfaceVariant else lightOnSurfaceVariant
    val onSurfaceMuted: Color get() = if (isDark) darkOnSurfaceMuted else lightOnSurfaceMuted
    val outline: Color get() = if (isDark) darkOutline else lightOutline
    val outlineVariant: Color get() = if (isDark) darkOutlineVariant else lightOutlineVariant
    val error: Color get() = if (isDark) darkError else lightError
    val onError: Color get() = if (isDark) darkOnError else lightOnError

    val transparent: Color = Color(0x00000000)
}
