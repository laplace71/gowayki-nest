package com.gowayki.nesh.app.features.sign_in.presentation.screens

// Registro en dos pasos:
//   1) datos personales (nombre, DNI, teléfono, email)
//   2) creación del PIN con confirmación (deben coincidir)
//
// NOTA: por ahora el PIN se acepta directo. La aprobación desde el panel de
// administrador queda pendiente para cuando ese panel esté disponible.

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.gowayki.nesh.app.common.ui.shared.WaykiLoadingOverlay
import com.gowayki.nesh.app.common.ui.shared.WaykiPinCreator
import com.gowayki.nesh.app.common.ui.shared.WaykiPrimaryButton
import com.gowayki.nesh.app.common.ui.shared.WaykiSubtitle
import com.gowayki.nesh.app.common.ui.shared.WaykiSwitchAuth
import com.gowayki.nesh.app.common.ui.shared.WaykiTextField
import com.gowayki.nesh.app.common.ui.shared.WaykiTitle
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing

@Composable
fun RegisterScreen(
    onRegisterClick: (name: String, dni: String, phone: String, email: String, pin: String) -> Unit = { _, _, _, _, _ -> },
    onLoginClick: () -> Unit = {},
    onBack: () -> Unit = {},
    onClearError: () -> Unit = {},
    isLoading: Boolean = false,
    error: String? = null,
) {
    var step by remember { mutableStateOf(0) } // 0 = datos, 1 = PIN
    var name by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    // Si el backend rechaza los datos (DNI/teléfono ya registrados), volvemos
    // al paso 1 para que el mensaje se vea junto a los campos.
    LaunchedEffect(error) {
        if (error != null) step = 0
    }

    val dataValid = name.trim().isNotBlank() &&
        !name.trim().contains("@") &&
        name.trim().length >= 2 &&
        dni.length == 8 &&
        phone.length == 9 &&
        emailRegex.matches(email.trim())

    WaykiBackground(withRoads = true) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = AppSpacing.pageHorizontal),
        ) {
            Spacer(Modifier.height(AppSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WaykiBackButton(
                    onClick = {
                        if (step == 1) {
                            step = 0
                        } else {
                            onBack()
                        }
                    },
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Wayki Nest",
                    style = AppTextStyles.waykiBrand,
                    color = AppColors.onBackground,
                )
            }

            Spacer(Modifier.height(AppSpacing.topSection))

            if (step == 0) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    WaykiTitle("Crear\ncuenta")
                    Spacer(Modifier.height(AppSpacing.xs))
                    WaykiSubtitle("Completa tus datos para registrarte.")
                    Spacer(Modifier.height(AppSpacing.xl))

                    WaykiTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Nombre completo",
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    )
                    if (name.trim().contains("@")) {
                        WaykiErrorText("El nombre no puede ser un correo electrónico.")
                    }
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
                        label = "Teléfono (9 dígitos)",
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                    )
                    Spacer(Modifier.height(AppSpacing.formFieldGap))
                    WaykiTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Correo electrónico",
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    )

                    Spacer(Modifier.height(AppSpacing.lg))
                    WaykiErrorText(error)
                    Spacer(Modifier.height(AppSpacing.sm))

                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        WaykiPrimaryButton(
                            text = "CONTINUAR",
                            onClick = {
                                onClearError()
                                step = 1
                            },
                            enabled = dataValid,
                            showArrow = true,
                            fullWidth = false,
                        )
                    }

                    Spacer(Modifier.height(AppSpacing.sm))

                    WaykiSwitchAuth(
                        prefix = "¿Ya tienes cuenta?",
                        link = "Inicia sesión",
                        onClick = onLoginClick,
                    )
                }
            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    WaykiTitle("Crea\ntu PIN")
                    Spacer(Modifier.height(AppSpacing.xs))
                    WaykiSubtitle(
                        "Para ${name.trim()}. Repite el PIN para confirmarlo.",
                    )
                    Spacer(Modifier.height(AppSpacing.xl))

                    WaykiPinCreator(
                        submitText = "CREAR CUENTA",
                        isSaving = isLoading,
                        error = error,
                        onConfirm = { pin ->
                            onRegisterClick(name.trim(), dni, phone, email.trim(), pin)
                        },
                    )
                }
            }

            Spacer(Modifier.height(AppSpacing.sm))
        }

        WaykiLoadingOverlay(isLoading)
    }
}

private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
