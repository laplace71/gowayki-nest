package com.gowayki.nesh.app.common.ui.shared

// Teclado numérico con estilo Material 3: botones circulares tonales,
// elevación sutil y ripple. Mantiene la paleta Wayki (AppColors).
//
// Cada celda ocupa un tercio del ancho para que los círculos queden
// centrados y nunca se deformen en pantallas angostas.

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing

@Composable
fun WaykiNumpad(
    isEnabled: Boolean = true,
    onNumberClick: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val numberBg = if (isEnabled) AppColors.surfaceVariant else AppColors.surfaceVariant.copy(alpha = 0.4f)
    val numberFg = if (isEnabled) AppColors.onBackground else AppColors.onSurfaceMuted
    val actionBg = if (isEnabled) AppColors.secondary else AppColors.secondary.copy(alpha = 0.4f)
    val actionFg = if (isEnabled) AppColors.onSecondary else AppColors.onSurfaceMuted

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        val keys = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9),
        )

        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                row.forEach { number ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        NumpadButton(
                            text = number.toString(),
                            backgroundColor = numberBg,
                            textColor = numberFg,
                            isEnabled = isEnabled,
                            onClick = { onNumberClick(number) },
                        )
                    }
                }
            }
        }

        // Última fila: espacio vacío, cero y borrar.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            Spacer(Modifier.weight(1f))

            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NumpadButton(
                    text = "0",
                    backgroundColor = numberBg,
                    textColor = numberFg,
                    isEnabled = isEnabled,
                    onClick = { onNumberClick(0) },
                )
            }

            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NumpadDeleteButton(
                    backgroundColor = actionBg,
                    iconColor = actionFg,
                    isEnabled = isEnabled,
                    onClick = onDeleteClick,
                )
            }
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
) {
    Surface(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
        color = backgroundColor,
        contentColor = textColor,
        tonalElevation = 1.dp,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = textColor,
                style = AppTextStyles.waykiNumpadDigit,
            )
        }
    }
}

@Composable
private fun NumpadDeleteButton(
    backgroundColor: Color,
    iconColor: Color,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
        color = backgroundColor,
        contentColor = iconColor,
        tonalElevation = 1.dp,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(28.dp)) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w * 0.15f, h * 0.5f)
                    lineTo(w * 0.4f, h * 0.25f)
                    lineTo(w * 0.85f, h * 0.25f)
                    lineTo(w * 0.85f, h * 0.75f)
                    lineTo(w * 0.4f, h * 0.75f)
                    close()
                }
                drawPath(
                    path = path,
                    color = iconColor,
                    style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round),
                )
                // La 'X' del centro.
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.5f, h * 0.4f),
                    end = Offset(w * 0.75f, h * 0.6f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.75f, h * 0.4f),
                    end = Offset(w * 0.5f, h * 0.6f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
