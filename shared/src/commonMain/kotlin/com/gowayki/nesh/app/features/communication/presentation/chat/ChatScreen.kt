// Pantalla de chat de una conversación: burbujas, lista de mensajes y
// campo de escritura. Estilo Wayki (ink, mauve, cream, slate).
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.communication.presentation.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.app.common.ui.shared.BackHandler
import com.gowayki.nesh.app.common.ui.shared.WaykiAvatar
import com.gowayki.nesh.app.features.communication.domain.model.Message
import com.gowayki.nesh.app.features.communication.domain.model.MessageKind
import com.gowayki.nesh.app.features.communication.domain.model.MessageStatus
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.feathericons.Check
import compose.icons.feathericons.CheckSquare
import compose.icons.feathericons.Mic
import compose.icons.feathericons.Send
import compose.icons.feathericons.Users

@Composable
fun ChatScreen(
    title: String,
    isGroup: Boolean,
    memberCount: Int,
    messages: List<Message>,
    draft: String,
    sending: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceNote: () -> Unit,
    onBack: () -> Unit,
    onGroupInfo: () -> Unit = {},
) {
    BackHandler(true) { onBack() }
    val listState = rememberLazyListState()

    // Baja al último mensaje cuando llega uno nuevo.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.cream)
    ) {
        // ── Cabecera ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.slate)
                .statusBarsPadding()
                .padding(horizontal = AppSpacing.pageHorizontal)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WaykiBackButton(onClick = onBack)

            Spacer(modifier = Modifier.width(AppSpacing.md))

            if (isGroup) {
                WaykiAvatar(text = title.take(2).uppercase(), size = 40.dp, dark = false)
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTextStyles.waykiLabelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = AppColors.cream,
                    maxLines = 1
                )
                if (isGroup) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = compose.icons.FeatherIcons.Users,
                            contentDescription = null,
                            tint = AppColors.cream.copy(alpha = 0.7f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$memberCount conductores",
                            style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                            color = AppColors.cream.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            if (isGroup) {
                val infoSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(AppColors.cream.copy(alpha = 0.15f), CircleShape)
                        .clickable(
                            interactionSource = infoSource,
                            indication = ripple(color = AppColors.cream.copy(alpha = 0.2f)),
                            onClick = onGroupInfo
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = compose.icons.FeatherIcons.Users,
                        contentDescription = "Info del grupo",
                        tint = AppColors.cream,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ── Mensajes ──────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = AppSpacing.pageHorizontal, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(messages) { index, message ->
                // Agrupa mensajes seguidos del mismo emisor (solo en grupos).
                val previous = messages.getOrNull(index - 1)
                val showSender = isGroup && previous?.senderId != message.senderId

                MessageBubble(
                    message = message,
                    showSender = showSender,
                    showAvatar = !message.isMine && showSender
                )
            }
        }

        // ── Campo de escritura ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.cream)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppColors.mauve.copy(alpha = 0.35f))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = AppColors.ink
                ),
                cursorBrush = SolidColor(AppColors.ink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                maxLines = 4,
                decorationBox = { inner ->
                    if (draft.isEmpty()) {
                        Text(
                            text = "Escribe un mensaje",
                            style = AppTextStyles.waykiMini.copy(fontSize = 14.sp),
                            color = AppColors.slate,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    inner()
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Botón de envío o de nota de voz.
            val hasText = draft.isNotBlank()
            val sendSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (hasText) AppColors.primary else AppColors.mauve,
                        CircleShape
                    )
                    .clickable(
                        interactionSource = sendSource,
                        indication = ripple(color = AppColors.cream.copy(alpha = 0.2f)),
                        enabled = !sending,
                        onClick = { if (hasText) onSend() else onVoiceNote() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasText) compose.icons.FeatherIcons.Send else compose.icons.FeatherIcons.Mic,
                    contentDescription = if (hasText) "Enviar" else "Nota de voz",
                    tint = AppColors.cream,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** Burbuja de un mensaje. */
@Composable
private fun MessageBubble(
    message: Message,
    showSender: Boolean,
    showAvatar: Boolean
) {
    val isMine = message.isMine

    // Cada burbuja "pop"a al aparecer: escala desde un poco más chica.
    val pop = remember(message.id) { Animatable(0.85f) }
    LaunchedEffect(message.id) {
        pop.animateTo(
            targetValue = 1f,
            animationSpec = tween(260, easing = LinearOutSlowInEasing)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                alpha = pop.value
            },
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine && showAvatar) {
            WaykiAvatar(
                text = message.senderName.take(1).uppercase(),
                size = 28.dp,
                dark = false
            )
            Spacer(modifier = Modifier.width(6.dp))
        } else if (!isMine) {
            Spacer(modifier = Modifier.width(34.dp))
        }

        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
            if (showSender) {
                Text(
                    text = message.senderName,
                    style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                    color = AppColors.slate,
                    modifier = Modifier.padding(start = 4.dp, bottom = 3.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (isMine) 20.dp else 6.dp,
                            bottomEnd = if (isMine) 6.dp else 20.dp
                        )
                    )
                    .background(if (isMine) AppColors.primary else AppColors.mauve.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (message.kind == MessageKind.AUDIO) {
                    AudioBubbleContent(message = message, isMine = isMine)
                } else {
                    Text(
                        text = message.text,
                        style = AppTextStyles.waykiLabel.copy(fontSize = 14.sp),
                        color = if (isMine) AppColors.cream else AppColors.ink
                    )
                }
            }

            // Hora + ticks de estado (solo mensajes propios).
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            ) {
                Text(
                    text = formatTime(message.sentAtMillis),
                    style = AppTextStyles.waykiMini.copy(fontSize = 10.sp),
                    color = AppColors.slate
                )
                if (isMine) {
                    Spacer(modifier = Modifier.width(4.dp))
                    when (message.status) {
                        MessageStatus.SENDING -> Icon(
                            imageVector = compose.icons.FeatherIcons.Check,
                            contentDescription = "Enviando",
                            tint = AppColors.slate,
                            modifier = Modifier.size(12.dp)
                        )
                        MessageStatus.DELIVERED -> Icon(
                            imageVector = compose.icons.FeatherIcons.CheckSquare,
                            contentDescription = "Entregado",
                            tint = AppColors.slate,
                            modifier = Modifier.size(12.dp)
                        )
                        MessageStatus.READ -> Icon(
                            imageVector = compose.icons.FeatherIcons.CheckSquare,
                            contentDescription = "Leído",
                            tint = AppColors.ink,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Contenido de una burbuja de audio: botón play + barra + duración. */
@Composable
private fun AudioBubbleContent(message: Message, isMine: Boolean) {
    val contentColor = if (isMine) AppColors.cream else AppColors.ink
    val muted = if (isMine) AppColors.cream.copy(alpha = 0.4f) else AppColors.slate

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(if (isMine) AppColors.secondary else AppColors.ink, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "▶",
                style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                color = if (isMine) AppColors.ink else AppColors.cream
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        // Barra de audio simulada.
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(6.dp)
                .background(muted, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatDuration(message.audioDurationSeconds),
            style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
            color = contentColor
        )
    }
}

/** "14:32" a partir del timestamp. */
private fun formatTime(millis: Long): String {
    val totalMinutes = (millis / 60000) % (24 * 60)
    val hour = (totalMinutes / 60).toInt()
    val minute = (totalMinutes % 60).toInt()
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

/** "18" segundos -> "0:18". */
private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "$m:${s.toString().padStart(2, '0')}"
}
