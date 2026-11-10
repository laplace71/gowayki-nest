package com.gowayki.nesh.app.common.ui.shared

// Creador de PIN con doble verificación (crear + repetir) y estética M3.
// Compartido por la pantalla de creación de PIN y el paso final del registro.

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing

@Composable
fun WaykiPinCreator(
    modifier: Modifier = Modifier,
    submitText: String = "GUARDAR",
    isSaving: Boolean = false,
    error: String? = null,
    onConfirm: (String) -> Unit = {},
) {
    var step by remember { mutableStateOf(0) } // 0 = crear, 1 = repetir
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }

    val matched = step == 1 && pin1.length == 6 && pin2 == pin1
    val current = if (step == 0) pin1 else pin2
    val hint = when {
        mismatch -> "Los PIN no coinciden. Inténtalo de nuevo."
        matched -> "Los PIN coinciden."
        step == 0 -> "Elige un PIN de 6 dígitos."
        else -> "Repite el mismo PIN para confirmarlo."
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (step == 0) "Crea tu PIN" else "Repite tu PIN",
            color = AppColors.onSurfaceMuted,
            style = AppTextStyles.waykiLabelMedium,
        )

        Spacer(Modifier.height(AppSpacing.lg))

        WaykiPinKeypad(
            value = current,
            onValueChange = { next ->
                mismatch = false
                if (step == 0) {
                    pin1 = next
                } else {
                    pin2 = next
                }
            },
            isError = mismatch,
            isBlocked = isSaving,
            onComplete = { completed ->
                if (step == 0) {
                    pin1 = completed
                    step = 1
                } else {
                    if (completed == pin1) {
                        pin2 = completed
                    } else {
                        pin2 = ""
                        mismatch = true
                    }
                }
            },
        )

        Spacer(Modifier.height(AppSpacing.lg))

        Text(
            text = hint,
            modifier = Modifier.fillMaxWidth(),
            color = when {
                mismatch -> AppColors.error
                matched -> AppColors.primary
                else -> AppColors.onSurfaceMuted
            },
            style = AppTextStyles.waykiLabelMedium,
            textAlign = TextAlign.Center,
        )

        if (step == 1) {
            TextButton(onClick = {
                step = 0
                pin1 = ""
                pin2 = ""
                mismatch = false
            }) {
                Text(
                    text = "Volver a escribir el PIN",
                    style = AppTextStyles.waykiMini,
                    color = AppColors.onSurfaceMuted,
                )
            }
        } else {
            Spacer(Modifier.height(AppSpacing.xxxl))
        }

        WaykiErrorText(error)

        Spacer(Modifier.height(AppSpacing.xs))

        WaykiPrimaryButton(
            text = submitText,
            onClick = { if (matched) onConfirm(pin1) },
            enabled = matched,
            isLoading = isSaving,
            showArrow = false,
            fullWidth = false,
        )
    }
}
