// Contrato para grabación de pantalla + audio (expect/actual).
// Android: implementación real con MediaProjection + MediaRecorder.
// iOS: stub (grabación de pantalla requiere API nativa de ReplayKit).

package com.gowayki.nesh.app.features.sos.domain.model

import kotlinx.coroutines.flow.StateFlow

enum class ScreenRecordingState {
    IDLE,
    RECORDING,
    PROCESSING,
    PAUSED
}

/**
 * Interfaz para grabación de pantalla con audio del dispositivo y micrófono.
 * Implementación nativa por plataforma.
 */
interface ScreenRecorder {
    /** Estado actual de la grabación */
    val state: StateFlow<ScreenRecordingState>
    
    /** Errores durante la grabación */
    val error: StateFlow<String?>
    
    /** Duración de la grabación en milisegundos */
    val recordingDuration: StateFlow<Long>
    
    /**
     * Inicia la grabación de pantalla.
     * Requiere permiso de MediaProjection en Android (se debe solicitar desde Activity).
     * @return Result con Unit si fue exitoso, Exception si falló
     */
    suspend fun startRecording(): Result<Unit>
    
    /**
     * Detiene la grabación y devuelve la ruta del archivo.
     * @return Result con la ruta del archivo de video si fue exitoso
     */
    suspend fun stopRecording(): Result<String>
    
    /**
     * Pausa la grabación (Android 7.0+).
     * En versiones anteriores no tiene efecto.
     */
    suspend fun pauseRecording(): Result<Unit>
    
    /**
     * Reanuda la grabación pausada.
     */
    suspend fun resumeRecording(): Result<Unit>
    
    /**
     * Cancela la grabación actual sin guardar.
     */
    suspend fun cancelRecording(): Result<Unit>
    
    /**
     * Verifica si los permisos necesarios están concedidos.
     */
    suspend fun hasScreenRecordPermission(): Boolean
    
    /**
     * Solicita permisos de grabación de pantalla.
     * En Android, esto debe iniciarse desde una Activity.
     */
    suspend fun requestScreenRecordPermission(): Boolean
}

/**
 * Factory para crear una instancia de ScreenRecorder según la plataforma.
 */
expect fun createScreenRecorder(): ScreenRecorder
