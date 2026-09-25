package com.gowayki.nesh.app.features.sign_in.presentation.screens

// Inicio de sesión — solo con tu PIN.

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.app.common.ui.shared.WaykiBackground
import com.gowayki.nesh.app.common.ui.shared.WaykiErrorText
import com.gowayki.nesh.app.common.ui.shared.WaykiLink
import com.gowayki.nesh.app.common.ui.shared.WaykiLoadingOverlay
import com.gowayki.nesh.app.common.ui.shared.WaykiPinInput
import com.gowayki.nesh.app.common.ui.shared.WaykiPrimaryButton
import com.gowayki.nesh.app.common.ui.shared.WaykiSwitchAuth
import com.gowayki.nesh.app.common.ui.shared.WaykiTitle

@Composable
fun LoginScreen(
    onLoginClick: (pin: String) -> Unit = {},
    onRegisterClick: () -> Unit = {},
    onForgotClick: () -> Unit = {},
    onBack: () -> Unit = {},
    isLoading: Boolean = false,
    error: String? = null,
) {
    var pin by remember { mutableStateOf("") }

    WaykiBackground(withRoads = true) {
        // Arriba: flecha + título en 2 líneas.
        Column(
            Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp),
        ) {
            WaykiBackButton(onClick = onBack)
            Spacer(Modifier.height(28.dp))
            WaykiTitle("Bienvenido\nde nuevo")
        }

        // Centro: PIN, ENTRAR y olvidé mi PIN.
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 200.dp, bottom = 80.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            WaykiPinInput(length = 6, boxSize = 44.dp, revealLast = false, onComplete = { pin = it })
            Spacer(Modifier.height(16.dp))
            WaykiErrorText(error)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WaykiPrimaryButton(
                    text = "ENTRAR",
                    onClick = { if (pin.length == 6) onLoginClick(pin) },
                    showArrow = false,
                    fullWidth = false,
                )
            }
        }

        // Abajo: olvidé mi PIN + cambio a registro.
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            WaykiLink(text = "¿Olvidaste tu PIN de ingreso?", onClick = onForgotClick)
            WaykiSwitchAuth(
                prefix = "¿No tienes cuenta?",
                link = "Regístrate",
                onClick = onRegisterClick,
            )
        }

        WaykiLoadingOverlay(isLoading)
    }
}