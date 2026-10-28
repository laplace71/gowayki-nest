package com.gowayki.nesh.core.theme.src

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.gowayki.nesh.core.theme.src.core.WaykiPalette

// Esquemas Wayki (claro y oscuro). Las variantes Material se derivan del color
// base con opacidad, nunca declaradas aparte.
object AppColorScheme {

    fun light(p: WaykiPalette): ColorScheme = lightColorScheme(
        primary              = p.primary,
        onPrimary            = p.onPrimary,
        primaryContainer     = p.primary.copy(alpha = 0.6f),
        onPrimaryContainer   = p.onPrimary,

        secondary            = p.secondary,
        onSecondary          = p.onSecondary,
        secondaryContainer   = p.secondary.copy(alpha = 0.25f),
        onSecondaryContainer = p.onSecondary,

        tertiary             = p.tertiary,
        onTertiary           = p.onTertiary,
        tertiaryContainer    = p.tertiary.copy(alpha = 0.2f),
        onTertiaryContainer  = p.onTertiary,

        background           = p.background,
        onBackground         = p.onBackground,

        surface              = p.surface,
        onSurface            = p.onSurface,
        surfaceVariant       = p.surfaceVariant,
        onSurfaceVariant     = p.onSurfaceVariant,

        outline              = p.outline,
        outlineVariant       = p.outlineVariant,

        error                = p.error,
        onError              = p.onError,
        errorContainer       = p.error.copy(alpha = 0.12f),
        onErrorContainer     = p.error,

        scrim                = p.shadow.copy(alpha = 0.25f),
        inverseSurface       = p.ink,
        inverseOnSurface     = p.cream,
        inversePrimary       = p.primary.copy(alpha = 0.6f),
        surfaceTint          = p.primary,
    )

    fun dark(p: WaykiPalette): ColorScheme = darkColorScheme(
        primary              = p.primary,
        onPrimary            = p.onPrimary,
        primaryContainer     = p.primary.copy(alpha = 0.6f),
        onPrimaryContainer   = p.onPrimary,

        secondary            = p.secondary,
        onSecondary          = p.onSecondary,
        secondaryContainer   = p.secondary.copy(alpha = 0.3f),
        onSecondaryContainer = p.onSecondary,

        tertiary             = p.tertiary,
        onTertiary           = p.onTertiary,
        tertiaryContainer    = p.tertiary.copy(alpha = 0.25f),
        onTertiaryContainer  = p.onTertiary,

        background           = p.background,
        onBackground         = p.onBackground,

        surface              = p.surface,
        onSurface            = p.onSurface,
        surfaceVariant       = p.surfaceVariant,
        onSurfaceVariant     = p.onSurfaceVariant,

        outline              = p.outline,
        outlineVariant       = p.outlineVariant,

        error                = p.error,
        onError              = p.onError,
        errorContainer       = p.error.copy(alpha = 0.18f),
        onErrorContainer     = p.error,

        scrim                = p.shadow.copy(alpha = 0.55f),
        inverseSurface       = p.ink,
        inverseOnSurface     = p.cream,
        inversePrimary       = p.primary.copy(alpha = 0.6f),
        surfaceTint          = p.primary,
    )
}
