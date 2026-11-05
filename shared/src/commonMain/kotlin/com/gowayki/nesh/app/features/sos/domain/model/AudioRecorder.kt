// Contrato para grabación de audio y feedback sonoro (expect/actual).
// Android: implementación real con MediaRecorder + ToneGenerator + MediaPlayer.
// iOS/Presentation: stub para Preview/tests.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.sos.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class RecordingState {
    IDLE,
    RECORDING,
    PROCESSING,
    ERROR,
}

interface AudioRecorder {
    val state: StateFlow<RecordingState>
    val error: StateFlow<String?>
    val recordingDuration: StateFlow<Long> // en milisegundos
    val isPlaying: StateFlow<Boolean>
    val playbackPosition: StateFlow<Long> // posición de reproducción en milisegundos
    val playbackDuration: StateFlow<Long> // duración total del audio en reproducción

    // Inicia la grabación de audio
    suspend fun startRecording(): Result<Unit>

    // Detiene la grabación y devuelve la ruta del archivo de audio
    suspend fun stopRecording(): Result<String>

    // Cancela la grabación actual sin guardar
    suspend fun cancelRecording(): Result<Unit>

    // Reproduce un beep simple (para presión simple)
    suspend fun playSingleBeep(): Result<Unit>

    // Reproduce tres beeps (para triple presión/SOS)
    suspend fun playTripleBeep(): Result<Unit>

    // Reproduce un archivo de audio grabado
    suspend fun playAudio(filePath: String): Result<Unit>

    // Detiene la reproducción
    suspend fun stopPlayback(): Result<Unit>

    // Verifica si tiene permisos de grabación
    suspend fun hasRecordPermission(): Boolean

    // Solicita permisos de grabación
    suspend fun requestRecordPermission(): Boolean
}

// Factory platform
expect fun createAudioRecorder(): AudioRecorder