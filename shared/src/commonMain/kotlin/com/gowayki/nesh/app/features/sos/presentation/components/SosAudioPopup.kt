// Ventana emergente global (Material 3) para las grabaciones SOS iniciadas
// desde el botón físico ESP32. Se muestra encima del módulo abierto y bloquea
// la pantalla hasta enviar, reproducir o descartar.
// La lógica del botón ESP32 no cambia: solo se rediseñó la interfaz.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.sos.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.ui.shared.BackHandler
import com.gowayki.nesh.app.common.ui.shared.WaykiAvatar
import com.gowayki.nesh.app.features.communication.presentation.components.VoiceNoteMode
import com.gowayki.nesh.app.features.communication.presentation.components.VoiceNotePanel
import com.gowayki.nesh.app.features.sos.presentation.StateFlow.SosViewModel
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors

@Composable
fun SosAudioPopup(vm: SosViewModel) {
    val ui = vm.ui
    val hasRecording = ui.lastRecordedFilePath != null
    val isRecording = ui.audioRecording
    val isPlaying = ui.isPlayingAudio
    val hasError = ui.audioError != null
    val visible = isRecording || isPlaying || hasRecording || hasError

    if (!visible) return

    val conversations by vm.conversations.collectAsState()
    var showTargetDialog by remember { mutableStateOf(false) }

    val mode = when {
        isRecording -> VoiceNoteMode.RECORDING
        isPlaying -> VoiceNoteMode.PLAYING
        hasRecording -> VoiceNoteMode.PREVIEW
        else -> VoiceNoteMode.ERROR
    }
    val totalMillis = if (hasRecording) {
        maxOf(ui.lastRecordedDurationMillis, ui.audioPlaybackDuration)
    } else {
        0L
    }

    // La ventana emergente modal no deja salir con Back.
    BackHandler(enabled = true) { }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .clickable { }
            .padding(horizontal = 20.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        VoiceNotePanel(
            mode = mode,
            statusText = when {
                isRecording -> "Grabando desde el botón ESP32"
                isPlaying -> "Reproduciendo tu grabación"
                hasRecording -> "Grabación lista para enviar"
                else -> "No se pudo grabar el audio"
            },
            elapsedMillis = ui.audioDuration,
            totalMillis = totalMillis,
            positionMillis = ui.audioPlaybackPosition,
            error = ui.audioError,
            kicker = "NOTA DE VOZ · SOS",
            onCancel = { vm.cancelAudioRecording() },
            onStop = { vm.stopAndSendAudio() },
            onPlayPause = {
                if (isPlaying) vm.stopPlayback() else vm.playLastRecording()
            },
            onSend = { showTargetDialog = true },
            onDiscard = { vm.discardRecordedAudio() },
            onDismissError = { vm.clearAudioError() },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
        )
    }

    if (showTargetDialog) {
        SendTargetDialog(
            conversations = conversations.map {
                TargetConversation(it.id, it.title, it.preview)
            },
            onDismiss = { showTargetDialog = false },
            onPick = { conversationId ->
                showTargetDialog = false
                vm.sendRecordedAudio(conversationId)
            },
        )
    }
}

private data class TargetConversation(
    val id: String,
    val title: String,
    val preview: String,
)

/** Diálogo M3 para elegir a qué conversación se envía la nota de voz del SOS. */
@Composable
private fun SendTargetDialog(
    conversations: List<TargetConversation>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = AppColors.surface,
        title = {
            Text(
                text = "Enviar nota de voz",
                style = AppTextStyles.waykiLabelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                ),
                color = AppColors.ink,
            )
        },
        text = {
            if (conversations.isEmpty()) {
                Text(
                    text = "No hay conversaciones disponibles.",
                    style = AppTextStyles.waykiMini,
                    color = AppColors.slate,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(conversations, key = { it.id }) { item ->
                        val id = item.id
                        val title = item.title
                        val preview = item.preview
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onPick(id) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            WaykiAvatar(
                                text = title.take(2).uppercase(),
                                size = 40.dp,
                                dark = false,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = AppTextStyles.waykiLabelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = AppColors.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (preview.isNotBlank()) {
                                    Text(
                                        text = preview,
                                        style = AppTextStyles.waykiMini,
                                        color = AppColors.slate,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = AppColors.slate.copy(alpha = 0.2f),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancelar",
                    style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.primary,
                )
            }
        },
    )
}
