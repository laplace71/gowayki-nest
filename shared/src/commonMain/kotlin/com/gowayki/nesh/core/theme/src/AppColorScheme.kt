package com.gowayki.nesh.core.theme.src

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import com.gowayki.nesh.core.theme.src.core.AppColors

// Esquema claro Wayki en la estructura del git (el KMP usaba el oscuro "Puya").
object AppColorScheme {

    val light: ColorScheme = lightColorScheme(

        primary              = AppColors.primary,
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