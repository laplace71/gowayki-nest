package com.gowayki.nesh.app.features.sign_in.presentation.screens

// Registro — nombre + DNI + celular (+ Google). Sin correo/contraseña.

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.app.common.ui.shared.WaykiBackground
import com.gowayki.nesh.app.common.ui.shared.WaykiErrorText
import com.gowayki.nesh.app.common.ui.shared.WaykiGoogleCircle
import com.gowayki.nesh.app.common.ui.shared.WaykiLoadingOverlay
import com.gowayki.nesh.app.common.ui.shared.WaykiOrDivider
import com.gowayki.nesh.app.common.ui.shared.WaykiPrimaryButton
import com.gowayki.nesh.app.common.ui.shared.WaykiSwitchAuth
import com.gowayki.nesh.app.common.ui.shared.WaykiTextField
import com.gowayki.nesh.app.common.ui.shared.WaykiTitle

@Composable
fun RegisterScreen(
    onRegisterClick: (name: String, dni: String, phone: String) -> Unit = { _, _, _ -> },
    onGoogleClick: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onBack: () -> Unit = {},
    isLoading: Boolean = false,
    error: String? = null,
) {
    var name by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    WaykiBackground(withRoads = true) {
        // Arriba: flecha + título en 2 líneas.
        Column(
            Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp),
        ) {
            WaykiBackButton(onClick = onBack)
            Spacer(Modifier.height(28.dp))
            WaykiTitle("Crear\ncuenta")
        }

        // Centro: datos, REGISTRARSE y Google abajo.
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 200.dp, bottom = 80.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            WaykiTextField(
                value = name,
                onValueChange = { name = it },
                label = "Nombre completo",
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(16.dp))
            WaykiTextField(
                value = dni,
                onValueChange = { if (it.length <= 8) dni = it.filter(Char::isDigit) },
                label = "DNI (8 dígitos)",
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(16.dp))
            WaykiTextField(
                value = phone,
                onValueChange = { if (it.length <= 9) phone = it.filter(Char::isDigit) },
                label = "Celular (9 dígitos)",
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
                onImeAction = { onRegisterClick(name, dni, phone) },
            )
            Spacer(Modifier.height(16.dp))
            WaykiErrorText(error)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WaykiPrimaryButton(
                    text = "REGISTRARSE",
                    onClick = { onRegisterClick(name, dni, phone) },
                    showArrow = false,
                    fullWidth = false,
                )
            }
            Spacer(Modifier.height(8.dp))
            WaykiOrDivider()
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WaykiGoogleCircle(onClick = onGoogleClick)
            }
        }

        // Abajo: cambio a login.
        Box(
            Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            WaykiSwitchAuth(
                prefix = "¿Ya tienes cuenta?",
                link = "Inicia sesión",
                onClick = onLoginClick,
            )
        }

        WaykiLoadingOverlay(isLoading)
    }
}