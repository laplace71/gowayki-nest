// Diálogo de confirmación reutilizable (eliminar, cerrar sesión, etc.).
// Se usa desde varias pantallas para no repetir el AlertDialog.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.common.ui.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors

@Composable
fun WaykiConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirmar",
    dismissLabel: String = "Cancelar",
    destructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.cream,
        titleContentColor = AppColors.ink,
        textContentColor = AppColors.slate,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = title,
                style = AppTextStyles.waykiLabelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text(
                    text = message,
                    style = AppTextStyles.waykiMini.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            WaykiTextButton(
                text = confirmLabel,
                onClick = onConfirm,
                fullWidth = false,
            )
        },
        dismissButton = {
            WaykiTextButton(
                text = dismissLabel,
                onClick = onDismiss,
                fullWidth = false,
            )
        }
    )
}