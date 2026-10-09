package com.gowayki.nesh.app.features.sign_in.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.app.common.ui.shared.WaykiLink
import com.gowayki.nesh.app.common.ui.shared.WaykiNumpad
import com.gowayki.nesh.app.common.ui.shared.WaykiPinDots
import com.gowayki.nesh.app.common.ui.shared.WaykiSubtitle
import com.gowayki.nesh.app.common.ui.shared.WaykiTitle
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing

enum class LoginStatus {
    NORMAL, ERROR, BLOCKED
}

data class LoginUiState(
    val pin: String = "",
    val status: LoginStatus = LoginStatus.NORMAL,
    val attemptsLeft: Int = 3,
    val blockTimerSeconds: Int = 0
)

@Composable
fun LoginScreen(
    state: LoginUiState,
    onNumberClick: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    onForgotPinClick: () -> Unit,
    onBackClick: () -> Unit,
    onSupportClick: () -> Unit
) {
    val isBlocked = state.status == LoginStatus.BLOCKED
    val isError = state.status == LoginStatus.ERROR

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = AppSpacing.pageHorizontal, vertical = AppSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabecera: Boton Volver + Logo Texto (Wayki Nest)
        Box(modifier = Modifier.fillMaxWidth()) {
            WaykiBackButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Text(
                text = "Wayki Nest",
                style = AppTextStyles.waykiTitle,
                color = AppColors.onBackground,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.xxl))

        // Titulo y Subtitulo Dinamico
        val titleText = when (state.status) {
            LoginStatus.NORMAL -> "Ingresa tu PIN"
            LoginStatus.ERROR -> "Probemos otra vez"
            LoginStatus.BLOCKED -> "Una pausa segura"
        }
        
        val subtitleText = when (state.status) {
            LoginStatus.BLOCKED -> "Por tu seguridad, el acceso est bloqueado\ntemporalmente."
            else -> "Tu cdigo de 6 dgitos te conecta con tu ruta."
        }

        WaykiTitle(text = titleText)
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        WaykiSubtitle(text = subtitleText)

        Spacer(modifier = Modifier.height(AppSpacing.xl))

        // Dots Visualizer
        WaykiPinDots(
            pinLength = state.pin.length,
            isError = isError,
            isBlocked = isBlocked
        )

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        // Banner Dinamico (Centro)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp), // Espacio reservado para evitar saltos de UI
            contentAlignment = Alignment.Center
        ) {
            when (state.status) {
                LoginStatus.ERROR -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.error.copy(alpha = 0.15f))
                            .padding(vertical = AppSpacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PIN incorrecto  Te quedan ${state.attemptsLeft} intentos",
                            style = AppTextStyles.waykiLabelMedium,
                            color = AppColors.onBackground
                        )
                        Text(
                            text = "Revisa tu cdigo antes de continuar.",
                            style = AppTextStyles.waykiCaption,
                            color = AppColors.onSurfaceMuted
                        )
                    }
                }
                LoginStatus.BLOCKED -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.surfaceVariant.copy(alpha = 0.5f))
                            .padding(vertical = AppSpacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Format seconds to mm:ss
                        val minutes = state.blockTimerSeconds / 60
                        val seconds = state.blockTimerSeconds % 60
                        val timeString = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
                        
                        Text(
                            text = "Vuelve a intentarlo en $timeString",
                            style = AppTextStyles.waykiLabelMedium,
                            color = AppColors.onBackground
                        )
                        Text(
                            text = "El teclado se habilitar automticamente.",
                            style = AppTextStyles.waykiCaption,
                            color = AppColors.onSurfaceMuted
                        )
                    }
                }
                LoginStatus.NORMAL -> {
                    // Texto normal si es necesario, o vacio
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Tu PIN es personal. No lo compartas.",
                            style = AppTextStyles.waykiLabelMedium,
                            color = AppColors.onBackground
                        )
                        Text(
                            text = "Lnea 7  Unidad 08",
                            style = AppTextStyles.waykiCaption,
                            color = AppColors.onSurfaceMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Teclado Numerico Custom
        WaykiNumpad(
            isEnabled = !isBlocked,
            onNumberClick = onNumberClick,
            onDeleteClick = onDeleteClick,
            modifier = Modifier.padding(horizontal = AppSpacing.md)
        )

        Spacer(modifier = Modifier.height(AppSpacing.xl))

        // Link Olvidaste tu PIN
        WaykiLink(
            text = "Olvidaste tu PIN?",
            onClick = onForgotPinClick
        )

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        // Banner de Seguridad / Soporte (Fondo oscuro)
        val bottomBannerBg = if (isBlocked) AppColors.onBackground else AppColors.onBackground
        val bottomBannerText = if (isBlocked) "Demasiados intentos fallidos. Si necesitas ayuda, contacta a soporte." else "Acceso seguro para acompaarte en cada recorrido."
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(bottomBannerBg)
                .padding(AppSpacing.md),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bottomBannerText,
                style = AppTextStyles.waykiCaption,
                color = AppColors.cream,
                textAlign = TextAlign.Center
            )
        }
    }
}