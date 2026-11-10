package com.gowayki.nesh.app.features.sign_in.presentation.screens

// Login por PIN con enfoque Material 3 sobre la paleta Wayki.
// Incluye un selector de persona (DNI) para cambiar de usuario sin salir
// de la pantalla y un acceso directo al registro.

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.app.common.ui.shared.WaykiLink
import com.gowayki.nesh.app.common.ui.shared.WaykiNumpad
import com.gowayki.nesh.app.common.ui.shared.WaykiPinDots
import com.gowayki.nesh.app.common.ui.shared.WaykiSubtitle
import com.gowayki.nesh.app.common.ui.shared.WaykiSwitchAuth
import com.gowayki.nesh.app.common.ui.shared.WaykiTextField
import com.gowayki.nesh.app.common.ui.shared.WaykiTitle
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppRadius
import com.gowayki.nesh.core.theme.src.core.AppSpacing

enum class LoginStatus {
    NORMAL, ERROR, BLOCKED
}

data class LoginUiState(
    val pin: String = "",
    val status: LoginStatus = LoginStatus.NORMAL,
    val attemptsLeft: Int = 3,
    val blockTimerSeconds: Int = 0,
)

@Composable
fun LoginScreen(
    state: LoginUiState,
    activeDni: String,
    onNumberClick: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    onForgotPinClick: () -> Unit,
    onBackClick: () -> Unit,
    onSupportClick: () -> Unit,
    onDniChange: (String) -> Unit = {},
    onRegisterClick: () -> Unit = {},
) {
    val isBlocked = state.status == LoginStatus.BLOCKED
    val isError = state.status == LoginStatus.ERROR
    var showDniDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = AppSpacing.pageHorizontal, vertical = AppSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Cabecera: volver + marca.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WaykiBackButton(onClick = onBackClick)
            Text(
                text = "Wayki Nest",
                style = AppTextStyles.waykiBrand,
                color = AppColors.onBackground,
            )
        }

        Spacer(Modifier.height(AppSpacing.lg))

        // Contenido superior desplazable: en pantallas bajas no se corta
        // el título ni los indicadores; el teclado queda siempre anclado.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Selector de persona: toca la tarjeta para cambiar el DNI.
            PersonCard(
                dni = activeDni,
                onClick = { showDniDialog = true },
            )

            Spacer(Modifier.height(AppSpacing.xl))

            val titleText = when (state.status) {
                LoginStatus.NORMAL -> "Ingresa tu PIN"
                LoginStatus.ERROR -> "Probemos otra vez"
                LoginStatus.BLOCKED -> "Una pausa segura"
            }
            val subtitleText = when (state.status) {
                LoginStatus.BLOCKED -> "Por tu seguridad, el acceso está bloqueado\ntemporalmente."
                else -> "Tu código de 6 dígitos te conecta con tu ruta."
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WaykiTitle(text = titleText, textAlign = TextAlign.Center)
                Spacer(Modifier.height(AppSpacing.xs))
                WaykiSubtitle(text = subtitleText, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(AppSpacing.lg))

            WaykiPinDots(
                pinLength = state.pin.length,
                isError = isError,
                isBlocked = isBlocked,
            )

            Spacer(Modifier.height(AppSpacing.md))

            LoginStatusBanner(state)

            Spacer(Modifier.height(AppSpacing.xl))
        }

        WaykiNumpad(
            isEnabled = !isBlocked,
            onNumberClick = onNumberClick,
            onDeleteClick = onDeleteClick,
            modifier = Modifier.padding(horizontal = AppSpacing.sm),
        )

        Spacer(Modifier.height(AppSpacing.lg))

        WaykiLink(
            text = "¿Olvidaste tu PIN?",
            onClick = onForgotPinClick,
        )

        WaykiSwitchAuth(
            prefix = "¿No tienes cuenta?",
            link = "Crear cuenta",
            onClick = onRegisterClick,
        )
    }

    if (showDniDialog) {
        ChangeDniDialog(
            currentDni = activeDni,
            onDismiss = { showDniDialog = false },
            onConfirm = { dni ->
                onDniChange(dni)
                showDniDialog = false
            },
        )
    }
}

// ---------- Tarjeta de persona activa ----------
@Composable
private fun PersonCard(
    dni: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AppRadius.lg,
        color = AppColors.surface,
        contentColor = AppColors.onBackground,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AppColors.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                PersonGlyph(AppColors.primary)
            }

            Spacer(Modifier.width(AppSpacing.md))

            Column(Modifier.weight(1f)) {
                Text(
                    text = "Ingresando como",
                    style = AppTextStyles.waykiMini,
                    color = AppColors.onSurfaceMuted,
                )
                Text(
                    text = formatDni(dni),
                    style = AppTextStyles.waykiLabelMedium,
                    color = AppColors.onBackground,
                )
            }

            Text(
                text = "Cambiar",
                style = AppTextStyles.waykiLabelMedium,
                color = AppColors.primary,
            )
        }
    }
}

// ---------- Diálogo para cambiar de persona ----------
@Composable
private fun ChangeDniDialog(
    currentDni: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var draft by remember(currentDni) { mutableStateOf(currentDni) }
    val isValid = draft.length == 8

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppRadius.xl,
        containerColor = AppColors.surface,
        titleContentColor = AppColors.onBackground,
        textContentColor = AppColors.onSurfaceMuted,
        title = {
            Text(
                text = "Cambiar de persona",
                style = AppTextStyles.waykiButtonSmall,
                color = AppColors.onBackground,
            )
        },
        text = {
            Column {
                Text(
                    text = "Ingresa el DNI de la persona que usará la app.",
                    style = AppTextStyles.waykiLabel,
                    color = AppColors.onSurfaceMuted,
                )
                Spacer(Modifier.height(AppSpacing.md))
                WaykiTextField(
                    value = draft,
                    onValueChange = {
                        if (it.length <= 8) draft = it.filter(Char::isDigit)
                    },
                    label = "DNI (8 dígitos)",
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    onImeAction = { if (isValid) onConfirm(draft) },
                )
                if (draft.isNotEmpty() && !isValid) {
                    Spacer(Modifier.height(AppSpacing.xs))
                    Text(
                        text = "El DNI debe tener 8 dígitos.",
                        style = AppTextStyles.waykiMini,
                        color = AppColors.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(draft) },
                enabled = isValid,
            ) {
                Text(
                    text = "Usar DNI",
                    style = AppTextStyles.waykiLabelMedium,
                    color = if (isValid) AppColors.primary else AppColors.onSurfaceMuted,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancelar",
                    style = AppTextStyles.waykiLabelMedium,
                    color = AppColors.onSurfaceMuted,
                )
            }
        },
    )
}

// ---------- Banner de estado compacto ----------
@Composable
private fun LoginStatusBanner(state: LoginUiState) {
    val isBlocked = state.status == LoginStatus.BLOCKED
    val isError = state.status == LoginStatus.ERROR

    val container = when {
        isError -> AppColors.error.copy(alpha = 0.12f)
        isBlocked -> AppColors.surfaceVariant.copy(alpha = 0.6f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .padding(horizontal = AppSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isError -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "PIN incorrecto • Te quedan ${state.attemptsLeft} intentos",
                    style = AppTextStyles.waykiLabelMedium,
                    color = AppColors.onBackground,
                )
                Text(
                    text = "Revisa tu código antes de continuar.",
                    style = AppTextStyles.waykiMini,
                    color = AppColors.onSurfaceMuted,
                )
            }

            isBlocked -> {
                val minutes = state.blockTimerSeconds / 60
                val seconds = state.blockTimerSeconds % 60
                val timeString = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Vuelve a intentarlo en $timeString",
                        style = AppTextStyles.waykiLabelMedium,
                        color = AppColors.onBackground,
                    )
                    Text(
                        text = "El teclado se habilitará automáticamente.",
                        style = AppTextStyles.waykiMini,
                        color = AppColors.onSurfaceMuted,
                    )
                }
            }

            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Tu PIN es personal. No lo compartas.",
                    style = AppTextStyles.waykiLabelMedium,
                    color = AppColors.onBackground,
                )
                Text(
                    text = "Línea 7 • Unidad 08",
                    style = AppTextStyles.waykiMini,
                    color = AppColors.onSurfaceMuted,
                )
            }
        }
    }
}

// ---------- Glifo de persona (sin dependencias de iconos) ----------
@Composable
private fun PersonGlyph(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val s = size.minDimension
        val stroke = 1.8.dp.toPx()
        drawCircle(
            color = color,
            radius = s * 0.18f,
            center = Offset(s / 2f, s * 0.30f),
            style = Stroke(width = stroke),
        )
        drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(s * 0.18f, s * 0.50f),
            size = Size(s * 0.64f, s * 0.42f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

private fun formatDni(dni: String): String =
    if (dni.length == 8) "${dni.take(4)} ${dni.drop(4)}" else dni
