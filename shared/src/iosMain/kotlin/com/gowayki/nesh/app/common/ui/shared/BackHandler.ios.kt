package com.gowayki.nesh.app.common.ui.shared

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    // iOS maneja el swipe back nativamente, no hace falta BackHandler
}
