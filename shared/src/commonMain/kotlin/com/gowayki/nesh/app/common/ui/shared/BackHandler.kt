package com.gowayki.nesh.app.common.ui.shared

import androidx.compose.runtime.Composable

/**
 * Maneja el gesto de retroceso del sistema (Android 14+ predictive back, iOS swipe back).
 * En Android usa BackHandler, en iOS no hace nada (iOS maneja el swipe back nativamente).
 */
@Composable
expect fun BackHandler(
    enabled: Boolean,
    onBack: () -> Unit
)
