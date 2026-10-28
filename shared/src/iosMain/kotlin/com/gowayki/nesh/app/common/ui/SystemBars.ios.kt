package com.gowayki.nesh.app.common.ui

import androidx.compose.runtime.Composable

@Composable
actual fun SystemBarsAppearance(isDark: Boolean) {
    // iOS: el estilo de la barra de estado lo controla el UIViewController raíz.
    // Se deja como no-op para no alterar el comportamiento actual.
}
