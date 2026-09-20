package com.gowayki.nesh.core.theme.src

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import com.gowayki.nesh.core.theme.src.core.AppColors

/**
 * 🌑 Esquema de colores Material 3 — Tema Oscuro "Puya" (Gowayki Nesh).
 */
object AppColorScheme {

    val dark: ColorScheme = darkColorScheme(

        primary              = AppColors.primaryLight,
        onPrimary            = AppColors.onPrimary,
        primaryContainer     = AppColors.primaryContainer,
        onPrimaryContainer   = AppColors.onPrimaryContainer,

        secondary            = AppColors.secondary,
        onSecondary          = AppColors.onSecondary,
        secondaryContainer   = AppColors.secondaryContainer,
        onSecondaryContainer = AppColors.onSecondaryContainer,

        tertiary             = AppColors.accent,
        onTertiary           = AppColors.onAccent,
        tertiaryContainer    = AppColors.accentDark,
        onTertiaryContainer  = AppColors.accent,

        background           = AppColors.background,
        onBackground         = AppColors.onBackground,

        surface              = AppColors.surface,
        onSurface            = AppColors.onSurface,
        surfaceVariant       = AppColors.surfaceVariant,
        onSurfaceVariant     = AppColors.onSurfaceMuted,

        outline              = AppColors.support,
        outlineVariant       = AppColors.outline,

        error                = AppColors.error,
        onError              = AppColors.onError,
        errorContainer       = AppColors.errorContainer,
        onErrorContainer     = AppColors.errorLight,

        scrim                = AppColors.scrim,
        inverseSurface       = AppColors.onBackground,
        inverseOnSurface     = AppColors.surface,
        inversePrimary       = AppColors.primaryDark,
        surfaceTint          = AppColors.primaryLight,
    )
}
