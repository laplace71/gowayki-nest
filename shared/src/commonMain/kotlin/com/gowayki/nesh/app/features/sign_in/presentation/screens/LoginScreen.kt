package com.gowayki.nesh.app.features.sign_in.presentation.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
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
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            WaykiBackButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Text(
                text = "GoWayki",
                style = AppTextStyles.waykiButtonSmall,
                color = AppColors.onBackground,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Titulo y Subtitulo Dinamico
        val titleText = when (state.status) {
            LoginStatus.NORMAL -> "Ingresa tu PIN"
            LoginStatus.ERROR -> "Probemos otra vez"
            LoginStatus.BLOCKED -> "Una pausa segura"
        }
        
        val subtitleText = when (state.status) {
            LoginStatus.BLOCKED -> "Por tu seguridad, el acceso está bloqueado\ntemporalmente."
            else -> "Tu código de 6 dígitos te conecta con tu ruta."
        }

        // We wrap these in a Column to ensure they are centered visually
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WaykiTitle(
                text = titleText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(AppSpacing.md))
            WaykiSubtitle(
                text = subtitleText,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.xxl))

        // Dots Visualizer
        WaykiPinDots(
            pinLength = state.pin.length,
            isError = isError,
            isBlocked = isBlocked
        )

        Spacer(modifier = Modifier.height(AppSpacing.xl))

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
                            text = "PIN incorrecto • Te quedan ${state.attemptsLeft} intentos",
                            style = AppTextStyles.waykiLabelMedium,
                            color = AppColors.onBackground
                        )
                        Text(
                            text = "Revisa tu código antes de continuar.",
                            style = AppTextStyles.waykiMini,
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
                            text = "El teclado se habilitará automáticamente.",
                            style = AppTextStyles.waykiMini,
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
                            text = "Línea 7 • Unidad 08",
                            style = AppTextStyles.waykiMini,
                            color = AppColors.onSurfaceMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.xl))

        // Teclado Numerico Custom
        WaykiNumpad(
            isEnabled = !isBlocked,
            onNumberClick = onNumberClick,
            onDeleteClick = onDeleteClick,
            modifier = Modifier.padding(horizontal = AppSpacing.xl)
        )

        Spacer(modifier = Modifier.height(AppSpacing.xl))

        // Link Olvidaste tu PIN
        WaykiLink(
            text = "¿Olvidaste tu PIN?",
            onClick = onForgotPinClick
        )

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        // Banner de Seguridad / Soporte (Fondo oscuro)
        val bottomBannerText = if (isBlocked) "Demasiados intentos fallidos. Si necesitas ayuda, contacta a soporte." else "Acceso seguro para acompañarte en cada recorrido."

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.onBackground)
                .padding(AppSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = WaykiShieldCheckIcon,
                contentDescription = null,
                tint = AppColors.mauve,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = bottomBannerText,
                style = AppTextStyles.waykiMini,
                color = AppColors.cream,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Escudo con check (shield-check) tomado del diseño de Figma — se usa en el
 * banner "Acceso seguro…". Trazo de 1.8, puntas redondeadas, viewport 22x22.
 */
private val WaykiShieldCheckIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "WaykiShieldCheck",
        defaultWidth = 22.dp,
        defaultHeight = 22.dp,
        viewportWidth = 22f,
        viewportHeight = 22f
    ).addPath(
        pathData = PathData {
            // Check central.
            moveTo(8.24987f, 11f)
            lineTo(10.083f, 12.8334f)
            lineTo(13.7493f, 9.16658f)
            // Contorno del escudo.
            moveTo(18.3322f, 11.9171f)
            curveTo(18.3322f, 16.5006f, 15.1242f, 18.7923f, 11.3112f, 20.1215f)
            curveTo(11.1116f, 20.1892f, 10.8947f, 20.1859f, 10.6971f, 20.1123f)
            curveTo(6.875f, 18.7923f, 3.66699f, 16.5006f, 3.66699f, 11.9171f)
            verticalLineTo(5.50021f)
            curveTo(3.66699f, 5.25708f, 3.76356f, 5.02392f, 3.93545f, 4.852f)
            curveTo(4.10734f, 4.68009f, 4.34048f, 4.58351f, 4.58357f, 4.58351f)
            curveTo(6.41672f, 4.58351f, 8.70815f, 3.48347f, 10.303f, 2.09009f)
            curveTo(10.4972f, 1.92417f, 10.7442f, 1.83301f, 10.9996f, 1.83301f)
            curveTo(11.255f, 1.83301f, 11.502f, 1.92417f, 11.6962f, 2.09009f)
            curveTo(13.3002f, 3.49264f, 15.5825f, 4.58351f, 17.4156f, 4.58351f)
            curveTo(17.6587f, 4.58351f, 17.8918f, 4.68009f, 18.0637f, 4.852f)
            curveTo(18.2356f, 5.02392f, 18.3322f, 5.25708f, 18.3322f, 5.50021f)
            verticalLineTo(11.9171f)
            close()
        },
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()
}
