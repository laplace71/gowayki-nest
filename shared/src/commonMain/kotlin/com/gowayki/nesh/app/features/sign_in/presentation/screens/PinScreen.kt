package com.gowayki.nesh.app.features.sign_in.presentation.screens

// Crear PIN — doble verificación: crea y repite, deben coincidir.
// Será la contraseña para ingresar después.

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.ui.shared.Inter
import com.gowayki.nesh.app.common.ui.shared.Muted
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.app.common.ui.shared.WaykiBackground
import com.gowayki.nesh.app.common.ui.shared.WaykiErrorText
import com.gowayki.nesh.app.common.ui.shared.WaykiLoadingOverlay
import com.gowayki.nesh.app.common.ui.shared.WaykiPinInput
import com.gowayki.nesh.app.common.ui.shared.WaykiPrimaryButton
import com.gowayki.nesh.app.common.ui.shared.WaykiSubtitle
import com.gowayki.nesh.app.common.ui.shared.WaykiTitle

@Composable
fun PinScreen(
    onConfirm: (pin: String) -> Unit = {},
    onBack: () -> Unit = {},
    isLoading: Boolean = false,
    error: String? = null,
) {
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    val match = pin1.length == 6 && pin1 == pin2
    val showError = pin1.length == 6 && pin2.length == 6 && pin1 != pin2

    WaykiBackground(withRoads = true) {
        // Arriba: flecha + título en 2 líneas.
        Column(
            Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp),
        ) {
            WaykiBackButton(onClick = onBack)
            Spacer(Modifier.height(28.dp))
            WaykiTitle("Crea\ntu PIN")
            Spacer(Modifier.height(8.dp))
            WaykiSubtitle("Será tu contraseña para ingresar después")
        }

        // Centro: crea + repite + guardar.
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 230.dp, bottom = 80.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Crea tu PIN",
                color = Muted, fontFamily = Inter,
                fontWeight = FontWeight.Medium, fontSize = 14.sp,
            )
            Spacer(Modifier.height(12.dp))
            WaykiPinInput(length = 6, boxSize = 44.dp, onComplete = { pin1 = it })
            Spacer(Modifier.height(20.dp))
            Text(
                "Repite tu PIN",
                color = Muted, fontFamily = Inter,
                fontWeight = FontWeight.Medium, fontSize = 14.sp,
            )
            Spacer(Modifier.height(12.dp))
            WaykiPinInput(length = 6, boxSize = 44.dp, onComplete = { pin2 = it })
            if (showError) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Los PIN no coinciden",
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFBA1A1A), fontFamily = Inter,
                    fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(20.dp))
            WaykiErrorText(error)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WaykiPrimaryButton(
                    text = "GUARDAR",
                    onClick = { if (match) onConfirm(pin1) },
                    showArrow = false,
                    fullWidth = false,
                )
            }
        }

        WaykiLoadingOverlay(isLoading)
    }
}