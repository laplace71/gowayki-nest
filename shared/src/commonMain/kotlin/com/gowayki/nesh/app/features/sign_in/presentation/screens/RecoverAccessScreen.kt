package com.gowayki.nesh.app.features.sign_in.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.app.common.ui.shared.WaykiBackground
import com.gowayki.nesh.app.common.ui.shared.WaykiLoadingOverlay
import com.gowayki.nesh.app.common.ui.shared.WaykiPrimaryButton
import com.gowayki.nesh.app.common.ui.shared.WaykiTextField
import com.gowayki.nesh.app.common.ui.shared.WaykiTitle
import com.gowayki.nesh.app.common.ui.shared.WaykiSubtitle
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import com.gowayki.nesh.core.theme.src.core.AppRadius

@Composable
fun RecoverAccessScreen(
    onSendCodeClick: (dni: String) -> Unit = {},
    onBack: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    isLoading: Boolean = false,
) {
    var dni by remember { mutableStateOf("") }

    WaykiBackground(withRoads = false) {
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = AppSpacing.pageHorizontal)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WaykiBackButton(onClick = onBack)
                Text(
                    text = "Wayki Nest",
                    style = AppTextStyles.waykiBrand,
                    color = AppColors.ink
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title & Subtitle
            Row(verticalAlignment = Alignment.CenterVertically) {
                KeyIcon()
                Spacer(modifier = Modifier.width(12.dp))
                WaykiTitle("Recupera tu acceso")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            WaykiSubtitle(
                text = "Te enviaremos un código al celular registrado con tu línea para crear un nuevo PIN."
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Celular Registrado Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.cream, AppRadius.card)
                    .border(1.dp, AppColors.outline.copy(alpha = 0.2f), AppRadius.card)
                    .padding(16.dp)
            ) {
                Text(
                    text = "CELULAR REGISTRADO",
                    style = AppTextStyles.waykiMini,
                    color = AppColors.slate
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "+51 9•• ••• 482",
                        style = AppTextStyles.waykiButtonLarge,
                        color = AppColors.ink
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .background(AppColors.mauve, RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Línea 7 • Unidad 08",
                            style = AppTextStyles.waykiMini,
                            color = AppColors.ink
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Documento de identidad input
            Text(
                text = "Documento de identidad",
                style = AppTextStyles.waykiLabelMedium,
                color = AppColors.ink
            )
            Spacer(modifier = Modifier.height(8.dp))
            WaykiTextField(
                value = dni,
                onValueChange = { if (it.length <= 8) dni = it.filter(Char::isDigit) },
                label = "70821436",
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                onImeAction = { onSendCodeClick(dni) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Usa el DNI asociado a tu cuenta de conductor.",
                style = AppTextStyles.waykiMini,
                color = AppColors.slate
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            WaykiPrimaryButton(
                text = "Enviar código por SMS",
                onClick = { onSendCodeClick(dni) },
                showArrow = true,
                fullWidth = true
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Informational steps
            Column(modifier = Modifier.fillMaxWidth()) {
                InfoStep(number = "1", text = "Verifica tu identidad")
                Spacer(modifier = Modifier.height(16.dp))
                InfoStep(number = "2", text = "Recibe tu código de seguridad")
                Spacer(modifier = Modifier.height(16.dp))
                InfoStep(number = "3", text = "Crea tu nuevo PIN de 6 dígitos")
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Support link
            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp).navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "¿Ya no tienes ese número? Contacta a soporte",
                    style = AppTextStyles.waykiLabelMedium.copy(textDecoration = TextDecoration.Underline),
                    color = AppColors.ink,
                    modifier = Modifier.clickable { onSupportClick() }
                )
            }
        }
        
        WaykiLoadingOverlay(isLoading)
    }
}

@Composable
private fun KeyIcon() {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(28.dp)) {
        val strokeWidth = 2.dp.toPx()
        val color = AppColors.ink
        
        // Circular head
        drawCircle(
            color = color,
            radius = 7.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(10.dp.toPx(), 10.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        // Shaft
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(14.95.dp.toPx(), 14.95.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(24.dp.toPx(), 24.dp.toPx()),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        // Teeth
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(20.dp.toPx(), 20.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(23.dp.toPx(), 17.dp.toPx()),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(23.dp.toPx(), 23.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(26.dp.toPx(), 20.dp.toPx()),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
private fun InfoStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(AppColors.mauve, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = AppTextStyles.waykiMini,
                color = AppColors.ink
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = AppTextStyles.waykiLabelMedium,
            color = AppColors.ink
        )
    }
}
