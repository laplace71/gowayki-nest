package com.gowayki.nesh.app.common.ui.shared

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppRadius
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.FeatherIcons
import compose.icons.feathericons.AlertTriangle
import compose.icons.feathericons.ChevronRight
import compose.icons.feathericons.GitCommit
import compose.icons.feathericons.HelpCircle
import compose.icons.feathericons.Home
import compose.icons.feathericons.LogOut
import compose.icons.feathericons.Map
import compose.icons.feathericons.Radio
import compose.icons.feathericons.Sliders
import compose.icons.feathericons.X

@Composable
fun WaykiSidebarContent(
    onClose: () -> Unit,
    onSosClick: () -> Unit = {},
    onInicioClick: () -> Unit = {},
    onCommunicationClick: () -> Unit = {},
    onRecorridoClick: () -> Unit = {},
    onMapaClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.cream)
            .padding(24.dp)
    ) {
        // 1. Marca y cierre
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Wayki Nest",
                style = AppTextStyles.waykiHeader,
                color = AppColors.ink
            )
            val closeInteractionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AppColors.mauve, CircleShape)
                    .clickable(
                        interactionSource = closeInteractionSource,
                        indication = ripple(color = AppColors.ink.copy(alpha = 0.2f)),
                        onClick = onClose,
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FeatherIcons.X,
                    contentDescription = "Cerrar",
                    tint = AppColors.ink,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 2. Conductor
        Text(
            text = "Carlos Mendoza",
            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
            color = AppColors.ink
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Línea 7 • Unidad 08 • Arequipa",
            style = AppTextStyles.waykiMini,
            color = AppColors.slate
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 3. Menu Principal label
        Text(
            text = "Menu Principal",
            style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
            color = AppColors.slate
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 4. Navegacion Principal
        DrawerMenuItem(
            icon = FeatherIcons.Home,
            title = "Inicio",
            subtitle = "Tu ritmo y próximo control",
            isSelected = false,
            onClick = onInicioClick
        )
        DrawerMenuItem(
            icon = FeatherIcons.Radio,
            title = "Comunicación",
            subtitle = "Canal de la Línea 7",
            isSelected = false,
            onClick = onCommunicationClick
        )
        DrawerMenuItem(
            icon = FeatherIcons.GitCommit,
            title = "Recorrido",
            subtitle = "Tus recorridos y horarios",
            isSelected = false,
            onClick = onRecorridoClick
        )
        DrawerMenuItem(
            icon = FeatherIcons.Map,
            title = "Mapa",
            subtitle = "Informacion de tus rutas",
            isSelected = true,
            onClick = onMapaClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 5. Separador
        Box(
            modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.slate.copy(alpha = 0.2f))
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 6. Preferencias y ayuda
        DrawerSimpleItem(icon = FeatherIcons.Sliders, title = "Configuración", onClick = onSettingsClick)
        DrawerSimpleItem(icon = FeatherIcons.HelpCircle, title = "Soporte técnico", onClick = onSupportClick)
        DrawerSimpleItem(icon = FeatherIcons.LogOut, title = "Cerrar sesión", onClick = onLogoutClick)

        Spacer(modifier = Modifier.height(32.dp))

        // 7. SOS
        val sosInteractionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.ink, RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = sosInteractionSource,
                    indication = ripple(color = AppColors.cream.copy(alpha = 0.2f)),
                    onClick = onSosClick
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = FeatherIcons.AlertTriangle,
                    contentDescription = "SOS",
                    tint = AppColors.cream,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "S.O.S",
                        style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = AppColors.cream
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Informacion de emergencias.",
                        style = AppTextStyles.waykiMini,
                        color = AppColors.cream
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Recorrido en curso • GPS activo",
            style = AppTextStyles.waykiMini,
            color = AppColors.slate,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DrawerMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> AppColors.ink
            isPressed -> AppColors.mauve.copy(alpha = 0.4f)
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "drawerItemBg"
    )
    val contentColor = if (isSelected) AppColors.cream else AppColors.ink
    val subtitleColor = if (isSelected) AppColors.cream.copy(alpha = 0.7f) else AppColors.slate

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AppColors.primary.copy(alpha = 0.15f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = AppTextStyles.waykiMini,
                color = subtitleColor
            )
        }
    }
}

@Composable
private fun DrawerSimpleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AppColors.primary.copy(alpha = 0.15f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.ink,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
            color = AppColors.ink,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = FeatherIcons.ChevronRight,
            contentDescription = null,
            tint = AppColors.ink,
            modifier = Modifier.size(20.dp)
        )
    }
}
