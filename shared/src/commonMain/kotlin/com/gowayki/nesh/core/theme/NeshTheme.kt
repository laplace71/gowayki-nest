package com.gowayki.nesh.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.gowayki.nesh.core.theme.src.AppColorScheme
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppBreakpoints
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppIconSizes
import com.gowayki.nesh.core.theme.src.core.AppMotion
import com.gowayki.nesh.core.theme.src.core.AppRadius
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import com.gowayki.nesh.core.theme.src.core.StatusColors

/**
 * 🎨 Tema raíz de Gowayki Nesh — paleta "Puya".
 *
 * ```kotlin
 * NeshTheme {
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
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme.dark,
        typography  = AppTextStyles.typography,
        content     = content
    )
}

object NeshTheme {
    val colors      = AppColors
    val spacing     = AppSpacing
    val radius      = AppRadius
    val motion      = AppMotion
    val iconSizes   = AppIconSizes
    val breakpoints = AppBreakpoints
    val status      = StatusColors

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
