// Stub para iOS — ReplayKit requiere integración nativa más profunda.

package com.gowayki.nesh.app.features.sos.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private class NoopScreenRecorder : ScreenRecorder {
    private val _state = MutableStateFlow(ScreenRecordingState.IDLE)
    override val state: StateFlow<ScreenRecordingState> = _state.asStateFlow()
    
    private val _error = MutableStateFlow<String?>("Grabación de pantalla no disponible en iOS desde KMP")
    override val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    override val recordingDuration: StateFlow<Long> = _duration.asStateFlow()
    
    override suspend fun startRecording(): Result<Unit> = 
        Result.failure(UnsupportedOperationException("No soportado en iOS desde KMP"))
    
    override suspend fun stopRecording(): Result<String> = 
        Result.failure(UnsupportedOperationException("No soportado en iOS desde KMP"))
    
    override suspend fun pauseRecording(): Result<Unit> = 
        Result.failure(UnsupportedOperationException("No soportado en iOS desde KMP"))
    
    override suspend fun resumeRecording(): Result<Unit> = 
        Result.failure(UnsupportedOperationException("No soportado en iOS desde KMP"))
    
    override suspend fun cancelRecording(): Result<Unit> = Result.success(Unit)
    
    override suspend fun hasScreenRecordPermission(): Boolean = false
    
    override suspend fun requestScreenRecordPermission(): Boolean = false
}

actual fun createScreenRecorder(): ScreenRecorder = NoopScreenRecorder()
