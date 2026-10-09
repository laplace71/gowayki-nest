package com.gowayki.nesh.app.common.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing

@Composable
fun WaykiPinDots(
    pinLength: Int,
    isError: Boolean = false,
    isBlocked: Boolean = false,
    maxLength: Int = 6,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until maxLength) {
            val isFilled = i < pinLength

            val dotColor = when {
                isError -> AppColors.error
                isBlocked -> AppColors.onSurfaceMuted
                else -> AppColors.onBackground
            }

            val borderColor = when {
                isError -> AppColors.error
                else -> AppColors.surfaceVariant
            }
            
            val boxBgColor = when {
                isError -> AppColors.error.copy(alpha = 0.1f)
                else -> AppColors.surfaceVariant.copy(alpha = 0.2f)
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(boxBgColor)
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isFilled) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }
    }
}
