// Menú contextual al mantener presionada una conversación.
// Acciones: vaciar chat, eliminar grupo, eliminar chat.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.communication.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.features.communication.domain.model.Conversation
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.FeatherIcons
import compose.icons.feathericons.MessageCircle
import compose.icons.feathericons.Trash2
import compose.icons.feathericons.Users

/** Qué acción eligió el usuario. */
enum class ConversationAction { CLEAR, DELETE_GROUP, DELETE_CHAT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationMenuSheet(
    conversation: Conversation,
    onDismiss: () -> Unit,
    onAction: (ConversationAction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.cream,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(AppColors.slate.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = AppSpacing.pageHorizontal)
                .padding(bottom = AppSpacing.lg)
        ) {
            // Cabecera: sobre qué conversación acts
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(if (conversation.avatarDark) AppColors.ink else AppColors.mauve, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = conversation.avatarText,
                        style = AppTextStyles.waykiButtonSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (conversation.avatarDark) AppColors.cream else AppColors.ink
                    )
                }
                Spacer(modifier = Modifier.size(14.dp))
                Column {
                    Text(
                        text = conversation.title,
                        style = AppTextStyles.waykiLabelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = AppColors.ink,
                        maxLines = 1
                    )
                    Text(
                        text = if (conversation.isGroup) {
                            "Grupo · ${conversation.members.size} conductores"
                        } else "Conversación privada",
                        style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                        color = AppColors.slate
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Separador
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.slate.copy(alpha = 0.15f))
            )

            Spacer(modifier = Modifier.height(8.dp))

            MenuAction(
                icon = compose.icons.FeatherIcons.MessageCircle,
                title = "Vaciar chat",
                subtitle = "Borra los mensajes, el grupo se conserva",
                onClick = { onAction(ConversationAction.CLEAR) }
            )

            if (conversation.isGroup) {
                MenuAction(
                    icon = compose.icons.FeatherIcons.Users,
                    title = "Eliminar grupo",
                    subtitle = "Se va la conversación y todos sus miembros",
                    onClick = { onAction(ConversationAction.DELETE_GROUP) }
                )
            }

            MenuAction(
                icon = compose.icons.FeatherIcons.Trash2,
                title = "Eliminar chat",
                subtitle = "Quita esta conversación de tu lista",
                destructive = true,
                onClick = { onAction(ConversationAction.DELETE_CHAT) }
            )
        }
    }
}

@Composable
private fun MenuAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val tint = if (destructive) AppColors.ink else AppColors.ink

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AppColors.primary.copy(alpha = 0.12f)),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    if (destructive) AppColors.ink else AppColors.mauve,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (destructive) AppColors.cream else AppColors.ink,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.size(14.dp))
        Column {
            Text(
                text = title,
                style = AppTextStyles.waykiLabelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = tint
            )
            Text(
                text = subtitle,
                style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                color = AppColors.slate
            )
        }
    }
}