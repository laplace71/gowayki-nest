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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
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
            .height(64.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(backgroundColor)
            .clickable(enabled = isEnabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = AppTextStyles.waykiNumpadDigit
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
            .height(64.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(backgroundColor)
            .clickable(enabled = isEnabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(32.dp)) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.15f, h * 0.5f)
                lineTo(w * 0.4f, h * 0.25f)
                lineTo(w * 0.9f, h * 0.25f)
                lineTo(w * 0.9f, h * 0.75f)
                lineTo(w * 0.4f, h * 0.75f)
                close()
            }
            drawPath(
                path = path, 
                color = iconColor, 
                style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
            )
            // La 'X' del centro
            drawLine(
                color = iconColor,
                start = Offset(w * 0.5f, h * 0.4f),
                end = Offset(w * 0.75f, h * 0.6f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = iconColor,
                start = Offset(w * 0.75f, h * 0.4f),
                end = Offset(w * 0.5f, h * 0.6f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
