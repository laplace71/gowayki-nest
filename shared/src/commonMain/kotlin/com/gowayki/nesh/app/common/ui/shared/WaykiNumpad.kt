package com.gowayki.nesh.app.common.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing

@Composable
fun WaykiNumpad(
    isEnabled: Boolean = true,
    onNumberClick: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonBg = if (isEnabled) AppColors.onBackground else AppColors.surfaceVariant
    val textColor = if (isEnabled) AppColors.cream else AppColors.onSurfaceMuted

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        val keys = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9)
        )

        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                row.forEach { number ->
                    NumpadButton(
                        text = number.toString(),
                        backgroundColor = buttonBg,
                        textColor = textColor,
                        isEnabled = isEnabled,
                        onClick = { onNumberClick(number) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Ultima fila: Vacio, Cero, Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            NumpadButton(
                text = "0",
                backgroundColor = buttonBg,
                textColor = textColor,
                isEnabled = isEnabled,
                onClick = { onNumberClick(0) },
                modifier = Modifier.weight(1f)
            )
            
            NumpadDeleteButton(
                backgroundColor = if (isEnabled) AppColors.surfaceVariant else AppColors.surfaceVariant.copy(alpha = 0.5f),
                iconColor = if (isEnabled) AppColors.onBackground else AppColors.onSurfaceMuted,
                isEnabled = isEnabled,
                onClick = onDeleteClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NumpadButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1.8f) // Ajuste para que se vean rectangulares redondeados segun Figma
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable(enabled = isEnabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = AppTextStyles.waykiTitle // Usando una tipografia grande
        )
    }
}

@Composable
private fun NumpadDeleteButton(
    backgroundColor: Color,
    iconColor: Color,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1.8f)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable(enabled = isEnabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "Borrar",
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
