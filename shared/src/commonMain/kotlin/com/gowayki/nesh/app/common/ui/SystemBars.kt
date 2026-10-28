package com.gowayki.nesh.app.common.ui

import androidx.compose.runtime.Composable

/**
 * Ajusta el color de los iconos de las barras del sistema (estado y navegación)
 * para que contrasten con el tema de la app.
 */
@Composable
expect fun SystemBarsAppearance(isDark: Boolean)
