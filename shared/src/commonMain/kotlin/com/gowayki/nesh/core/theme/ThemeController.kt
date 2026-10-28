package com.gowayki.nesh.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Estado del tema para que cualquier pantalla (por ejemplo Configuración)
 * pueda leerlo y cambiarlo sin pasar parámetros por toda la navegación.
 */
@Immutable
class ThemeController(
    val isDark: Boolean,
    val setDark: (Boolean) -> Unit,
)

val LocalThemeController = staticCompositionLocalOf {
    ThemeController(isDark = false, setDark = {})
}
