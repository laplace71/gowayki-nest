// Pantalla de chat de una conversación: burbujas, lista de mensajes y
// campo de escritura. Estilo Wayki (ink, mauve, cream, slate).
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.communication.presentation.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.gowayki.nesh.app.common.ui.shared.ToastManager
import com.gowayki.nesh.app.features.sos.domain.model.createAudioRecorder
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import com.gowayki.nesh.app.features.communication.domain.model.Message
import com.gowayki.nesh.app.features.communication.domain.model.MessageKind
import com.gowayki.nesh.app.features.communication.domain.model.MessageStatus
import com.gowayki.nesh.app.features.communication.presentation.components.VoiceNoteMode
import com.gowayki.nesh.app.features.communication.presentation.components.VoiceNotePanel
import com.gowayki.nesh.app.features.communication.presentation.components.formatVoiceTime
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.feathericons.Check
import compose.icons.feathericons.CheckSquare
import compose.icons.feathericons.Mic
import compose.icons.feathericons.Archive
import compose.icons.feathericons.Inbox
import compose.icons.feathericons.MessageCircle
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.Send
import compose.icons.feathericons.Trash2
import compose.icons.feathericons.ArrowUp
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
    conversationId: String,
    voiceVm: CommunicationVoiceViewModel,
    onBack: () -> Unit,
    photoPath: String? = null,
    onChangePhoto: (String) -> Unit = {},
    onDeleteMessage: (String) -> Unit = {},
    onDeleteMessageForAll: (String) -> Unit = {},
    archived: Boolean = false,
    onArchive: () -> Unit = {},
    onUnarchive: () -> Unit = {},
    onClearChat: () -> Unit = {},
    onDeleteChat: () -> Unit = {},
) {
    var showInfo by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var longPressedMessage by remember { mutableStateOf<Message?>(null) }
    var confirmDeleteChat by remember { mutableStateOf(false) }
    var confirmClearChat by remember { mutableStateOf(false) }
    BackHandler(true) { if (showInfo) showInfo = false else onBack() }
    val listState = rememberLazyListState()

    // Baja al último mensaje cuando llega uno nuevo.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    if (showInfo) {
        ChatInfoScreen(
            title = title,
            isGroup = isGroup,
            memberCount = memberCount,
            photoPath = photoPath,
            onBack = { showInfo = false },
            onChangePhoto = onChangePhoto,
        )
        return
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
                .background(AppColors.primary)
                .statusBarsPadding()
                .padding(horizontal = AppSpacing.pageHorizontal)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WaykiBackButton(onClick = onBack)

            Spacer(modifier = Modifier.width(AppSpacing.md))

            if (isGroup) {
                WaykiAvatar(
                    text = title.take(2).uppercase(),
                    size = 40.dp,
                    dark = false,
                    photoPath = photoPath,
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f).clickable { showInfo = true }) {
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
                    Text(
                        text = "$memberCount conductores",
                        style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                        color = AppColors.cream.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))
            val menuSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(AppColors.cream.copy(alpha = 0.15f), CircleShape)
                    .clickable(
                        interactionSource = menuSource,
                        indication = ripple(color = AppColors.cream.copy(alpha = 0.2f)),
                        onClick = { showMenu = true }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = compose.icons.FeatherIcons.MoreVertical,
                    contentDescription = "Más opciones",
                    tint = AppColors.cream,
                    modifier = Modifier.size(18.dp)
                )
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
                    showAvatar = !message.isMine && showSender,
                    onLongPress = { longPressedMessage = message }
                )
            }
        }

        // ── Campo de escritura (o barra de voz como WhatsApp) ────────────
        val voiceUi = voiceVm.ui
        val hasVoiceState = voiceUi.isRecording ||
            voiceUi.isPlaying ||
            voiceUi.lastRecordedFilePath != null ||
            voiceUi.audioError != null
        if (hasVoiceState) {
            RecordingComposer(
                voiceVm = voiceVm,
                voiceUi = voiceUi,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.cream)
                    .navigationBarsPadding()
                    .imePadding(),
            )
        } else {
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
            if (hasText) {
                val sendSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(AppColors.primary, CircleShape)
                        .clickable(
                            interactionSource = sendSource,
                            indication = ripple(color = AppColors.cream.copy(alpha = 0.2f)),
                            enabled = !sending,
                            onClick = onSend,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = compose.icons.FeatherIcons.Send,
                        contentDescription = "Enviar",
                        tint = AppColors.cream,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                VoiceRecordingButton(
                    voiceVm = voiceVm,
                    conversationId = conversationId,
                )
            }
        }
        }
    }

    // ── Menú de la conversación ──────────────────────────────────────────
    if (showMenu) {
        val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            sheetState = sheetState,
            containerColor = AppColors.cream,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = title,
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    color = AppColors.ink,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                )
                ChatMenuRow(
                    icon = if (archived) compose.icons.FeatherIcons.Inbox else compose.icons.FeatherIcons.Archive,
                    label = if (archived) "Desarchivar" else "Archivar",
                    onClick = {
                        showMenu = false
                        if (archived) onUnarchive() else onArchive()
                    }
                )
                ChatMenuRow(
                    icon = compose.icons.FeatherIcons.MessageCircle,
                    label = "Vaciar chat",
                    onClick = { showMenu = false; confirmClearChat = true }
                )
                ChatMenuRow(
                    icon = compose.icons.FeatherIcons.Trash2,
                    label = "Eliminar chat",
                    destructive = true,
                    onClick = { showMenu = false; confirmDeleteChat = true }
                )
            }
        }
    }

    // ── Acciones de un mensaje (mantener presionado) ─────────────────────
    longPressedMessage?.let { msg ->
        val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { longPressedMessage = null },
            sheetState = sheetState,
            containerColor = AppColors.cream,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Mensaje",
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    color = AppColors.ink,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                )
                ChatMenuRow(
                    icon = compose.icons.FeatherIcons.Trash2,
                    label = "Eliminar para mí",
                    onClick = {
                        longPressedMessage = null
                        onDeleteMessage(msg.id)
                    }
                )
                ChatMenuRow(
                    icon = compose.icons.FeatherIcons.Trash2,
                    label = "Eliminar para todos",
                    destructive = true,
                    onClick = {
                        longPressedMessage = null
                        onDeleteMessageForAll(msg.id)
                    }
                )
            }
        }
    }

    if (confirmDeleteChat) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDeleteChat = false },
            containerColor = AppColors.cream,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "¿Eliminar este chat?",
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = AppColors.ink
                )
            },
            text = {
                Text(
                    text = if (isGroup) "Se eliminará esta conversación de tu lista de chats. El grupo seguirá disponible en la sección Grupos y podrás volver a abrirlo."
                    else "Se borrará \"$title\" con sus mensajes y audios.",
                    style = AppTextStyles.waykiMini,
                    color = AppColors.slate
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirmDeleteChat = false
                    onDeleteChat()
                }) {
                    Text("Eliminar chat", color = AppColors.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmDeleteChat = false }) {
                    Text("Cancelar", color = AppColors.primary)
                }
            }
        )
    }

    if (confirmClearChat) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmClearChat = false },
            containerColor = AppColors.cream,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "¿Vaciar este chat?",
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = AppColors.ink
                )
            },
            text = {
                Text(
                    text = "Se eliminarán todos los mensajes de esta conversación. El chat seguirá disponible en tu lista y podrás seguir enviando y recibiendo mensajes.",
                    style = AppTextStyles.waykiMini,
                    color = AppColors.slate
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirmClearChat = false
                    onClearChat()
                }) {
                    Text("Vaciar chat", color = AppColors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmClearChat = false }) {
                    Text("Cancelar", color = AppColors.primary)
                }
            }
        )
    }
}

@Composable
private fun ChatMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (destructive) AppColors.error else AppColors.ink
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(if (destructive) AppColors.error.copy(alpha = 0.12f) else AppColors.mauve.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
            color = tint
        )
    }
}

/** Burbuja de un mensaje. */
@Composable
private fun MessageBubble(
    message: Message,
    showSender: Boolean,
    showAvatar: Boolean,
    onLongPress: () -> Unit = {}
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
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            )
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

/** Contenido de una burbuja de audio: botón play real + barra + duración. */
@Composable
private fun AudioBubbleContent(message: Message, isMine: Boolean) {
    val contentColor = if (isMine) AppColors.cream else AppColors.ink
    val muted = if (isMine) AppColors.cream.copy(alpha = 0.4f) else AppColors.slate
    val audioRecorder = remember { runCatching { createAudioRecorder() }.getOrNull() }
    val isPlaying by (audioRecorder?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) })
    val position by (audioRecorder?.playbackPosition?.collectAsState() ?: remember { mutableStateOf(0L) })
    val playbackTotal by (audioRecorder?.playbackDuration?.collectAsState() ?: remember { mutableStateOf(0L) })
    val audioPath = message.audioFilePath.orEmpty()
    val scope = rememberCoroutineScope()

    val totalMillis = if (isPlaying && playbackTotal > 0L) {
        playbackTotal
    } else {
        message.audioDurationSeconds * 1000L
    }
    val progress = if (totalMillis > 0L) {
        (position.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Row(
        modifier = Modifier.widthIn(min = 180.dp, max = 260.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isMine) AppColors.secondary else AppColors.ink)
                .clickable {
                    if (audioPath.isBlank()) {
                        ToastManager.show("Audio de muestra: sin archivo grabado")
                    } else {
                        scope.launch {
                            if (isPlaying) {
                                audioRecorder?.stopPlayback()
                            } else {
                                audioRecorder?.playAudio(audioPath)
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isPlaying) "❚❚" else "▶",
                style = AppTextStyles.waykiLabelMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                color = if (isMine) AppColors.ink else AppColors.cream
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape),
                color = if (isMine) AppColors.cream else AppColors.ink,
                trackColor = muted,
                drawStopIndicator = {}
            )
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isPlaying) formatVoiceTime(position) else "0:00",
                    style = AppTextStyles.waykiMini.copy(fontSize = 10.sp),
                    color = contentColor.copy(alpha = 0.8f)
                )
                Text(
                    text = formatVoiceTime(totalMillis),
                    style = AppTextStyles.waykiMini.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = compose.icons.FeatherIcons.Mic,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
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

// ── Grabación tipo WhatsApp para Comunicación ────────────────────────────────
// Pulsa el micrófono para iniciar la grabación y pulsa detener para
// previsualizarla. Desde la barra puedes reproducir, descartar y enviar.

@Composable
private fun VoiceRecordingButton(
    voiceVm: CommunicationVoiceViewModel,
    conversationId: String,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(AppColors.mauve, CircleShape)
            .clickable { voiceVm.toggleRecording(conversationId) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = compose.icons.FeatherIcons.Mic,
            contentDescription = "Grabar nota de voz",
            tint = AppColors.cream,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun RecordingComposer(
    voiceVm: CommunicationVoiceViewModel,
    voiceUi: CommunicationVoiceUiState,
    modifier: Modifier = Modifier,
) {
    val hasRecording = voiceUi.lastRecordedFilePath != null
    val mode = when {
        voiceUi.isRecording -> VoiceNoteMode.RECORDING
        voiceUi.isPlaying -> VoiceNoteMode.PLAYING
        hasRecording -> VoiceNoteMode.PREVIEW
        else -> VoiceNoteMode.ERROR
    }
    val totalMillis = if (hasRecording) {
        maxOf(voiceUi.recordedDurationMillis, voiceUi.playbackDuration)
    } else {
        0L
    }

    VoiceNotePanel(
        mode = mode,
        statusText = when {
            voiceUi.isRecording -> "Grabando nota de voz"
            voiceUi.isPlaying -> "Reproduciendo nota de voz"
            hasRecording -> "Nota de voz lista"
            else -> "No se pudo grabar"
        },
        elapsedMillis = voiceUi.duration,
        totalMillis = totalMillis,
        positionMillis = voiceUi.playbackPosition,
        error = voiceUi.audioError,
        onCancel = { voiceVm.cancel() },
        onStop = { voiceVm.stopAndSave() },
        onPlayPause = {
            if (voiceUi.isPlaying) voiceVm.stopPlayback() else voiceVm.playLastRecording()
        },
        onSend = { voiceVm.sendRecorded() },
        onDiscard = { voiceVm.discardRecorded() },
        onDismissError = { voiceVm.clearError() },
        compact = true,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

