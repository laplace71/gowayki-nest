// Ventana emergente global (Material 3) para las grabaciones de Comunicación.
// Se muestra encima de la pantalla actual; el usuario puede reproducir,
// descartar y volver a grabar antes de enviar.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.communication.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gowayki.nesh.app.common.ui.shared.BackHandler
import com.gowayki.nesh.app.features.communication.presentation.components.VoiceNoteMode
import com.gowayki.nesh.app.features.communication.presentation.components.VoiceNotePanel

@Composable
fun CommunicationVoicePopup(vm: CommunicationVoiceViewModel) {
    val ui = vm.ui
    val hasRecording = ui.lastRecordedFilePath != null
    val isRecording = ui.isRecording
    val isPlaying = ui.isPlaying
    val visible = (isRecording && ui.isLocked) || isPlaying || hasRecording || ui.audioError != null

    if (!visible) return

    val mode = when {
        isRecording -> VoiceNoteMode.RECORDING
        isPlaying -> VoiceNoteMode.PLAYING
        hasRecording -> VoiceNoteMode.PREVIEW
        else -> VoiceNoteMode.ERROR
    }
    val totalMillis = if (hasRecording) {
        maxOf(ui.recordedDurationMillis, ui.playbackDuration)
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
                isRecording -> "Grabando desde Comunicación"
                isPlaying -> "Reproduciendo tu grabación"
                hasRecording -> "Grabación lista para enviar"
                else -> "No se pudo grabar el audio"
            },
            elapsedMillis = ui.duration,
            totalMillis = totalMillis,
            positionMillis = ui.playbackPosition,
            error = ui.audioError,
            kicker = "NOTA DE VOZ",
            onCancel = { vm.cancel() },
            onStop = { vm.stopAndSave() },
            onPlayPause = {
                if (isPlaying) vm.stopPlayback() else vm.playLastRecording()
            },
            onSend = { vm.sendRecorded() },
            onDiscard = { vm.discardRecorded() },
            onDismissError = { vm.clearError() },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
        )
    }
}
