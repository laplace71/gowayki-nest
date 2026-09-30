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
import com.gowayki.nesh.core.theme.src.core.AppSpacing

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
                .padding(
                    start = AppSpacing.pageHorizontal,
                    end = AppSpacing.pageHorizontal,
                    top = AppSpacing.sm,
                ),
        ) {
            WaykiBackButton(onClick = onBack)
            Spacer(Modifier.height(AppSpacing.topSection))
            WaykiTitle("Crear\ncuenta")
        }

        // Centro: datos, REGISTRARSE y Google abajo.
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth()
                .padding(
                    start = AppSpacing.pageHorizontal,
                    end = AppSpacing.pageHorizontal,
                    top = AppSpacing.authContentTop,
                    bottom = AppSpacing.contentBottomMargin,
                )
                .verticalScroll(rememberScrollState()),
        ) {
            WaykiTextField(
                value = name,
                onValueChange = { name = it },
                label = "Nombre completo",
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(AppSpacing.formFieldGap))
            WaykiTextField(
                value = dni,
                onValueChange = { if (it.length <= 8) dni = it.filter(Char::isDigit) },
                label = "DNI (8 dígitos)",
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(AppSpacing.formFieldGap))
            WaykiTextField(
                value = phone,
                onValueChange = { if (it.length <= 9) phone = it.filter(Char::isDigit) },
                label = "Celular (9 dígitos)",
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
                onImeAction = { onRegisterClick(name, dni, phone) },
            )
            Spacer(Modifier.height(AppSpacing.formFieldGap))
            WaykiErrorText(error)
            Spacer(Modifier.height(AppSpacing.xs))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WaykiPrimaryButton(
                    text = "REGISTRARSE",
                    onClick = { onRegisterClick(name, dni, phone) },
                    showArrow = false,
                    fullWidth = false,
                )
            }
            Spacer(Modifier.height(AppSpacing.sm))
            WaykiOrDivider()
            Spacer(Modifier.height(AppSpacing.sm))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WaykiGoogleCircle(onClick = onGoogleClick)
            }
        }

        // Abajo: cambio a login.
        Box(
            Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = AppSpacing.md),
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