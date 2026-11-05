// Stub iOS/Presentation para Preview y tests.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.sos.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private class NoopAudioRecorder : AudioRecorder {
    private val _state = MutableStateFlow(RecordingState.IDLE)
    override val state: StateFlow<RecordingState> = _state.asStateFlow()
    private val _error = MutableStateFlow<String?>("Audio no disponible en iOS/Presentation")
    override val error: StateFlow<String?> = _error.asStateFlow()
    private val _duration = MutableStateFlow(0L)
    override val recordingDuration: StateFlow<Long> = _duration.asStateFlow()
    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _playbackPosition = MutableStateFlow(0L)
    override val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()
    private val _playbackDuration = MutableStateFlow(0L)
    override val playbackDuration: StateFlow<Long> = _playbackDuration.asStateFlow()

    override suspend fun startRecording(): Result<Unit> = Result.failure(UnsupportedOperationException("Noop iOS"))
    override suspend fun stopRecording(): Result<String> = Result.failure(UnsupportedOperationException("Noop iOS"))
    override suspend fun cancelRecording(): Result<Unit> = Result.failure(UnsupportedOperationException("Noop iOS"))
    override suspend fun playSingleBeep(): Result<Unit> = Result.success(Unit)
    override suspend fun playTripleBeep(): Result<Unit> = Result.success(Unit)
    override suspend fun playAudio(filePath: String): Result<Unit> = Result.success(Unit)
    override suspend fun stopPlayback(): Result<Unit> = Result.success(Unit)
    override suspend fun hasRecordPermission(): Boolean = false
    override suspend fun requestRecordPermission(): Boolean = false
}

actual fun createAudioRecorder(): AudioRecorder = NoopAudioRecorder()