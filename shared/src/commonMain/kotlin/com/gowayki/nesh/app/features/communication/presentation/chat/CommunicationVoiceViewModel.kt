// ViewModel de notas de voz para Comunicación.
// Vive a nivel raíz del NavHost: la grabación, reproducción y envío sobreviven
// a la navegación entre módulos.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.communication.presentation.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gowayki.nesh.app.features.communication.domain.model.MessageKind
import com.gowayki.nesh.app.features.communication.domain.repository.ChatRepository
import com.gowayki.nesh.app.common.ui.shared.ToastManager
import com.gowayki.nesh.app.features.sos.domain.model.AudioRecorder
import com.gowayki.nesh.app.features.sos.domain.model.RecordingState
import com.gowayki.nesh.app.features.sos.domain.model.createAudioRecorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class CommunicationVoiceUiState(
    val isRecording: Boolean = false,
    val isPlaying: Boolean = false,
    val duration: Long = 0L,
    val recordedDurationMillis: Long = 0L,
    val playbackPosition: Long = 0L,
    val playbackDuration: Long = 0L,
    val isLocked: Boolean = false,
    val lastRecordedFilePath: String? = null,
    val audioError: String? = null,
    val targetConversationId: String? = null,
)

class CommunicationVoiceViewModel(
    private val chatRepository: ChatRepository? = null,
    private val audioRecorder: AudioRecorder? = runCatching { createAudioRecorder() }.getOrNull(),
) : ViewModel() {

    var ui by mutableStateOf(CommunicationVoiceUiState())
        private set

    private var stateJob: Job? = null
    private var durationJob: Job? = null
    private var playingJob: Job? = null
    private var errorJob: Job? = null
    private var startJob: Job? = null

    init {
        observeRecorder()
    }

    private fun observeRecorder() {
        audioRecorder?.let { recorder ->
            stateJob = viewModelScope.launch {
                recorder.state.collectLatest { state ->
                    ui = ui.copy(isRecording = (state == RecordingState.RECORDING))
                }
            }
            durationJob = viewModelScope.launch {
                recorder.recordingDuration.collectLatest { duration ->
                    ui = ui.copy(duration = duration)
                }
            }
            playingJob = viewModelScope.launch {
                recorder.isPlaying.collectLatest { playing ->
                    ui = ui.copy(isPlaying = playing)
                }
            }
            viewModelScope.launch {
                kotlinx.coroutines.flow.combine(
                    recorder.playbackPosition,
                    recorder.playbackDuration,
                ) { position, duration -> position to duration }
                    .collectLatest { (position, duration) ->
                        ui = ui.copy(
                            playbackPosition = position,
                            playbackDuration = duration,
                        )
                    }
            }
            errorJob = viewModelScope.launch {
                recorder.error.collectLatest { error ->
                    ui = ui.copy(audioError = error)
                }
            }
        }
    }

    /**
     * Comportamiento ideal tipo WhatsApp:
     * pulsa el micrófono para iniciar la grabación y pulsa detener para previsualizar.
     */
    fun toggleRecording(conversationId: String) {
        if (ui.isRecording) {
            stopAndSave()
        } else {
            startRecording(conversationId)
        }
    }

    /** Inicia una grabación destinada a [conversationId]. */
    fun startRecording(conversationId: String) {
        val recorder = requireRecorder() ?: return
        if (ui.isRecording) return

        startJob = viewModelScope.launch {
            ui = ui.copy(
                targetConversationId = conversationId,
                audioError = null,
                duration = 0,
                isLocked = false,
                lastRecordedFilePath = null,
            )
            // Beep no bloquea el inicio de la grabación.
            launch { recorder.playSingleBeep() }

            if (!recorder.hasRecordPermission()) {
                val granted = recorder.requestRecordPermission()
                if (!granted) {
                    val message =
                        "Falta permiso de micrófono. Actívalo en Ajustes > Aplicaciones > Wayki Nest > Permisos."
                    ui = ui.copy(audioError = message)
                    ToastManager.show("Falta permiso de micrófono")
                    return@launch
                }
            }
            recorder.startRecording()
                .onFailure { error ->
                    val message = error.message ?: "No se pudo iniciar la grabación"
                    ui = ui.copy(audioError = message)
                    ToastManager.show(message)
                }
                .onSuccess {
                    ui = ui.copy(audioError = null)
                    com.gowayki.nesh.core.mqtt.MqttClient.publish(
                        com.gowayki.nesh.core.mqtt.MqttTopics.RECORDING,
                        com.gowayki.nesh.core.mqtt.MqttTopics.MSG_RECORDING
                    )
                }
        }
    }

    /** Fija la grabación (modo manos libres): muestra controles para terminar/enviar. */
    fun lockRecording() {
        if (ui.isRecording) {
            ui = ui.copy(isLocked = true)
        }
    }

    /** Detiene la grabación y la deja en PREVIEW (no envía todavía). */
    fun stopAndSave() {
        val recorder = requireRecorder() ?: return
        viewModelScope.launch {
            // Si el usuario suelta inmediatamente, espera a que arranque y luego corta.
            startJob?.join()
            val recordedMillis = ui.duration
            recorder.stopRecording()
                .onFailure { error ->
                    ui = ui.copy(audioError = error.message ?: "No se pudo guardar el audio")
                }
                .onSuccess { path ->
                    ui = ui.copy(
                        lastRecordedFilePath = path,
                        recordedDurationMillis = recordedMillis,
                        isLocked = false,
                        audioError = null,
                    )
                }
        }
    }

    /** Detiene y envía directamente (pulsar-mantener y soltar, estilo WhatsApp). */
    fun stopAndSend() {
        val recorder = requireRecorder() ?: return
        viewModelScope.launch {
            // Espera al arranque para no perder una pulsación corta.
            startJob?.join()
            // Si el arranque falló, el motivo ya fue mostrado por toast/UI.
            if (!ui.isRecording && ui.audioError != null) return@launch
            val durationMillis = ui.duration
            recorder.stopRecording()
                .onFailure { error ->
                    ui = ui.copy(audioError = error.message ?: "No se pudo guardar el audio")
                }
                .onSuccess { path ->
                    sendAudioFile(path, durationMillis)
                }
        }
    }

    /** Envía la grabación a la conversación destino previamente elegida. */
    fun sendRecorded() {
        val path = ui.lastRecordedFilePath ?: return
        val targetId = ui.targetConversationId ?: return
        val duration = if (ui.recordedDurationMillis > 0L) ui.recordedDurationMillis else ui.duration
        sendAudioFile(path, duration)
    }

    /** Envía un archivo de audio al destino actual. */
    private fun sendAudioFile(filePath: String, durationMillis: Long) {
        val targetId = ui.targetConversationId ?: return
        viewModelScope.launch {
            val repo = chatRepository
            if (repo == null) {
                ui = ui.copy(audioError = "No hay repositorio de chat disponible.")
                return@launch
            }
            val durationSeconds = (durationMillis / 1000).toInt().coerceAtLeast(1)
            repo.sendMessage(
                conversationId = targetId,
                text = "Nota de voz",
                kind = MessageKind.AUDIO,
                audioDurationSeconds = durationSeconds,
                audioFilePath = filePath,
            )
                .onSuccess {
                    ui = ui.copy(
                        lastRecordedFilePath = null,
                        recordedDurationMillis = 0,
                        playbackPosition = 0,
                        playbackDuration = 0,
                        isLocked = false,
                        duration = 0,
                        audioError = null,
                    )
                }
                .onFailure { error ->
                    ui = ui.copy(audioError = error.message ?: "No se pudo enviar el audio")
                }
        }
    }

    /** Cierra el aviso de error y deja la barra en estado idle. */
    fun clearError() {
        ui = ui.copy(audioError = null)
    }

    /** Cancela una grabación en curso sin guardar. */
    fun cancel() {
        val recorder = audioRecorder ?: return
        viewModelScope.launch {
            recorder.cancelRecording()
            ui = ui.copy(duration = 0, isRecording = false, isLocked = false, audioError = null)
        }
    }

    /** Descarta la grabación guardada sin enviarla. */
    fun discardRecorded() {
        ui = ui.copy(
            lastRecordedFilePath = null,
            recordedDurationMillis = 0,
            playbackPosition = 0,
            playbackDuration = 0,
            isLocked = false,
            duration = 0,
            audioError = null,
        )
    }

    /** Reproduce la última grabación guardada. */
    fun playLastRecording() {
        val path = ui.lastRecordedFilePath ?: return
        audioRecorder?.let { recorder ->
            viewModelScope.launch { recorder.playAudio(path) }
        }
    }

    /** Detiene la reproducción actual. */
    fun stopPlayback() {
        audioRecorder?.let { recorder ->
            viewModelScope.launch { recorder.stopPlayback() }
        }
    }

    override fun onCleared() {
        stateJob?.cancel()
        durationJob?.cancel()
        playingJob?.cancel()
        errorJob?.cancel()
        viewModelScope.launch { audioRecorder?.cancelRecording() }
    }

    private fun requireRecorder(): AudioRecorder? {
        val recorder = audioRecorder
        if (recorder == null) {
            ui = ui.copy(audioError = "Grabadora no disponible en este dispositivo.")
        }
        return recorder
    }
}
