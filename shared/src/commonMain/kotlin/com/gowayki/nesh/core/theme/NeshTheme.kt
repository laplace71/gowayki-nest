package com.gowayki.nesh.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.gowayki.nesh.core.theme.src.AppColorScheme
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppBreakpoints
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppMotion
import com.gowayki.nesh.core.theme.src.core.AppRadius
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import com.gowayki.nesh.core.theme.src.core.DarkPalette
import com.gowayki.nesh.core.theme.src.core.LightPalette
import com.gowayki.nesh.core.theme.src.core.LocalWaykiPalette

/**
 * 🎨 Tema raíz de Wayki — paleta clara u oscura.
 *
 * ```kotlin
 * NeshTheme(darkTheme = isDark) {
 *     Scaffold { ... }
 * }
 *
 * // Acceso a tokens:
 * Text(color = NeshTheme.colors.primary)
 * Modifier.padding(NeshTheme.spacing.lg)
 * Surface(shape = NeshTheme.radius.card)
 * ```
 */
@Composable
fun NeshTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    CompositionLocalProvider(LocalWaykiPalette provides palette) {
        MaterialTheme(
            colorScheme = if (darkTheme) {
                AppColorScheme.dark(palette)
            } else {
                AppColorScheme.light(palette)
            },
            typography  = AppTextStyles.typography,
            content     = content
        )
    }
}

object NeshTheme {
    val colors      = AppColors
    val spacing     = AppSpacing
    val radius      = AppRadius
    val motion      = AppMotion
    val breakpoints = AppBreakpoints

    val typography
        @ReadOnlyComposable @Composable
        get() = MaterialTheme.typography

    val colorScheme
        @ReadOnlyComposable @Composable
        get() = MaterialTheme.colorScheme

    val shapes
        @ReadOnlyComposable @Composable
        get() = MaterialTheme.shapes
}
