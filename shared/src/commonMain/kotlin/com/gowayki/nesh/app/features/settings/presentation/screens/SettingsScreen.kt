package com.gowayki.nesh.app.features.settings.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bell
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.ChevronRight
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Globe
import compose.icons.feathericons.HelpCircle
import compose.icons.feathericons.Lock
import compose.icons.feathericons.MapPin
import compose.icons.feathericons.Volume2

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onSupportClick: () -> Unit = {}
) {
    var timeAlertsEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var channelAudioEnabled by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.cream)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = AppSpacing.pageHorizontal, vertical = AppSpacing.sm)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Encabezado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WaykiBackButton(onClick = onBack)
                    Text(
                        text = "Configuración",
                        style = AppTextStyles.waykiHeader,
                        color = AppColors.ink
                    )
                }

                Icon(
                    imageVector = FeatherIcons.Bell,
                    contentDescription = "Notificaciones",
                    tint = AppColors.ink,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onNotificationsClick() }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "A tu manera, sin perder el ritmo.",
                style = AppTextStyles.waykiMini.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = AppColors.slate
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Perfil del conductor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.mauve, RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(AppColors.ink, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CM",
                            style = AppTextStyles.waykiButtonSmall.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = AppColors.cream
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Carlos Mendoza",
                            style = AppTextStyles.waykiLabelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = AppColors.ink
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Línea 7 · Unidad 08",
                            style = AppTextStyles.waykiMini,
                            color = AppColors.slate
                        )
                    }

                    Icon(
                        imageVector = FeatherIcons.CheckCircle,
                        contentDescription = "Verificado",
                        tint = AppColors.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 3. AVISOS Y COMUNICACIÓN
            Text(
                text = "AVISOS Y COMUNICACIÓN",
                style = AppTextStyles.waykiMini.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = AppColors.slate
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingSwitchItem(
                    icon = FeatherIcons.Clock,
                    title = "Alertas de tiempo",
                    subtitle = "Al adelantar o atrasar tu recorrido",
                    checked = timeAlertsEnabled,
                    onCheckedChange = { timeAlertsEnabled = it }
                )

                SettingSwitchItem(
                    icon = FeatherIcons.Volume2,
                    title = "Vibración",
                    subtitle = "Avisos discretos de puntos de control",
                    checked = vibrationEnabled,
                    onCheckedChange = { vibrationEnabled = it }
                )

                SettingSwitchItem(
                    icon = FeatherIcons.Volume2,
                    title = "Audio del canal",
                    subtitle = "Escuchar mensajes de la Línea 7",
                    checked = channelAudioEnabled,
                    onCheckedChange = { channelAudioEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 4. CUENTA Y APLICACIÓN
            Text(
                text = "CUENTA Y APLICACIÓN",
                style = AppTextStyles.waykiMini.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = AppColors.slate
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingActionItem(
                    icon = FeatherIcons.Lock,
                    title = "Cambiar PIN",
                    subtitle = "Protege tu código de 6 dígitos",
                    value = null,
                    onClick = {}
                )

                SettingActionItem(
                    icon = FeatherIcons.MapPin,
                    title = "Ubicación",
                    subtitle = "Activa durante tu recorrido",
                    value = "GPS activo",
                    onClick = {}
                )

                SettingActionItem(
                    icon = FeatherIcons.Globe,
                    title = "Idioma",
                    subtitle = "Idioma de la aplicación",
                    value = "Español",
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 5. Botón Soporte Técnico
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(AppColors.mauve)
                    .clickable { onSupportClick() }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = FeatherIcons.HelpCircle,
                        contentDescription = null,
                        tint = AppColors.ink,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Soporte técnico",
                        style = AppTextStyles.waykiLabelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = AppColors.ink
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Versión al pie
            Text(
                text = "GoWayki para Android · Versión 1.0.0",
                style = AppTextStyles.waykiMini.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = AppColors.slate,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF2EDF2))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AppColors.ink.copy(alpha = 0.08f)),
                onClick = { onCheckedChange(!checked) }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.ink,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTextStyles.waykiLabelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = AppColors.ink
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = AppTextStyles.waykiMini.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = AppColors.slate
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                interactionSource = interactionSource,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppColors.cream,
                    checkedTrackColor = AppColors.ink,
                    uncheckedThumbColor = AppColors.cream,
                    uncheckedTrackColor = AppColors.steel.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun SettingActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String?,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF2EDF2))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AppColors.ink.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.ink,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTextStyles.waykiLabelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = AppColors.ink
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = AppTextStyles.waykiMini.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = AppColors.slate
                )
            }

            if (value != null) {
                Text(
                    text = value,
                    style = AppTextStyles.waykiMini.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = AppColors.slate
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Icon(
                imageVector = FeatherIcons.ChevronRight,
                contentDescription = null,
                tint = AppColors.slate,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
