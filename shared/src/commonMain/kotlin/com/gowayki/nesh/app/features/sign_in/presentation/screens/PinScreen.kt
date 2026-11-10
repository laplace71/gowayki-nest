package com.gowayki.nesh.app.features.sign_in.presentation.screens

// Crear PIN — doble verificación: crea y repite, deben coincidir.
// Será la contraseña para ingresar después. Diseño Material 3 con la paleta
// Wayki: indicadores animados + teclado tonal.

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.app.common.ui.shared.WaykiBackground
import com.gowayki.nesh.app.common.ui.shared.WaykiLoadingOverlay
import com.gowayki.nesh.app.common.ui.shared.WaykiPinCreator
import com.gowayki.nesh.app.common.ui.shared.WaykiSubtitle
import com.gowayki.nesh.app.common.ui.shared.WaykiTitle
import com.gowayki.nesh.core.theme.src.core.AppSpacing

@Composable
fun PinScreen(
    onConfirm: (pin: String) -> Unit = {},
    onBack: () -> Unit = {},
    isLoading: Boolean = false,
    error: String? = null,
) {
    WaykiBackground(withRoads = true) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = AppSpacing.pageHorizontal,
                    end = AppSpacing.pageHorizontal,
                    top = AppSpacing.sm,
                    bottom = AppSpacing.xxl,
                ),
        ) {
            WaykiBackButton(onClick = onBack)
            Spacer(Modifier.height(AppSpacing.topSection))
            WaykiTitle("Crea\ntu PIN")
            Spacer(Modifier.height(AppSpacing.xs))
            WaykiSubtitle("Será tu contraseña para ingresar después")
            Spacer(Modifier.height(AppSpacing.xl))
            WaykiPinCreator(
                submitText = "GUARDAR",
                isSaving = isLoading,
                error = error,
                onConfirm = onConfirm,
            )
        }

        WaykiLoadingOverlay(isLoading)
    }
}
