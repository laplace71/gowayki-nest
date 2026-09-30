package com.gowayki.nesh.core.theme.src

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import com.gowayki.nesh.core.theme.src.core.AppColors

// Esquema claro Wayki. Las variantes Material se derivan del color base con
// opacidad (AppColors.*.copy(alpha)), nunca declaradas aparte.
object AppColorScheme {

    val light: ColorScheme = lightColorScheme(

        primary              = AppColors.primary,
        onPrimary            = AppColors.onPrimary,
        primaryContainer     = AppColors.primary.copy(alpha = 0.6f),
        onPrimaryContainer   = AppColors.onPrimary,

        secondary            = AppColors.secondary,
        onSecondary          = AppColors.onSecondary,
        secondaryContainer   = AppColors.secondary.copy(alpha = 0.25f),
        onSecondaryContainer = AppColors.onSecondary,

        tertiary             = AppColors.tertiary,
        onTertiary           = AppColors.onTertiary,
        tertiaryContainer    = AppColors.tertiary.copy(alpha = 0.2f),
        onTertiaryContainer  = AppColors.onTertiary,

        background           = AppColors.background,
        onBackground         = AppColors.onBackground,

        surface              = AppColors.surface,
        onSurface            = AppColors.onSurface,
        surfaceVariant       = AppColors.surfaceVariant,
        onSurfaceVariant     = AppColors.onSurfaceVariant,

        outline              = AppColors.outline,
        outlineVariant       = AppColors.outlineVariant,

        error                = AppColors.error,
        onError              = AppColors.onError,
        errorContainer       = AppColors.error.copy(alpha = 0.12f),
        onErrorContainer     = AppColors.error,

        scrim                = AppColors.ink.copy(alpha = 0.25f),
        inverseSurface       = AppColors.ink,
        inverseOnSurface     = AppColors.cream,
        inversePrimary       = AppColors.primary.copy(alpha = 0.6f),
        surfaceTint          = AppColors.primary,
    )
}