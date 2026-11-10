package com.gowayki.nesh.app.common.ui.shared

// Bloque reutilizable de captura de PIN: indicadores M3 + teclado numérico.
// Lo usan el login, el registro y la creación de PIN para que todos se vean
// y se comporten igual.

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.gowayki.nesh.core.theme.src.core.AppSpacing

@Composable
fun WaykiPinKeypad(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false,
    isBlocked: Boolean = false,
    onComplete: (String) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WaykiPinDots(
            pinLength = value.length,
            isError = isError,
            isBlocked = isBlocked,
            maxLength = length,
            activeIndex = value.length,
        )

        Spacer(Modifier.height(AppSpacing.xxl))

        WaykiNumpad(
            isEnabled = !isBlocked,
            onNumberClick = { digit ->
                if (value.length < length) {
                    val next = value + digit
                    onValueChange(next)
                    if (next.length == length) onComplete(next)
                }
            },
            onDeleteClick = {
                if (value.isNotEmpty()) onValueChange(value.dropLast(1))
            },
        )
    }
}
