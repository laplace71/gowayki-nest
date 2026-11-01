// Implementación Android para grabación de pantalla usando MediaProjection.
// Requiere permisos RECORD_AUDIO + solicitud de MediaProjection (ya configurados).

@file:Suppress("SpellCheckingInspection", "MissingPermission", "DEPRECATION")
package com.gowayki.nesh.app.features.sos.domain.model

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

object AndroidScreenRecordContext {
    var appContext: Context? = null
    var currentActivity: Activity? = null
}

// Código de solicitud del permiso MediaProjection (9004). Debe coincidir con el
// usado en MainActivity.onActivityResult para resolver la continuación del permiso.
const val REQUEST_CODE_SCREEN_CAPTURE = 9004

private class AndroidScreenRecorder(
    private val appContext: Context,
) : ScreenRecorder {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    private val _state = MutableStateFlow(ScreenRecordingState.IDLE)
    override val state: StateFlow<ScreenRecordingState> = _state.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _recordingDuration = MutableStateFlow(0L)
    override val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()
    
    private var recordingStartTime: Long = 0
    private var durationTimer: Job? = null
    private val currentFilePath = AtomicReference<String?>(null)
    private val currentResultData = AtomicReference<Pair<Int, Intent?>?>(null)

    override suspend fun startRecording(): Result<Unit> = runCatching {
        // Verificar permiso de audio (necesario para grabar con micrófono)
        if (!hasAudioPermission()) {
            throw SecurityException("Permiso RECORD_AUDIO no concedido")
        }
        
        // Solicitar permiso de MediaProjection
        val activity = AndroidScreenRecordContext.currentActivity
            ?: throw IllegalStateException("No hay Activity disponible para solicitar MediaProjection")
        
        val projectionManager = appContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = projectionManager.createScreenCaptureIntent()
        
        // Solicitar permiso (bloqueante)
        val (resultCode, data) = suspendCancellableCoroutine { continuation ->
            // Guardar continuación para reanudar desde onActivityResult
            pendingContinuation = continuation
            activity.startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE)
        }
        if (resultCode != Activity.RESULT_OK) {
            throw SecurityException("Permiso de grabación de pantalla denegado")
        }
        
        currentResultData.set(Pair(resultCode, data))
        
        // Preparar archivo de salida
        val outputFile = createVideoFile()
        currentFilePath.set(outputFile.absolutePath)
        
        // Obtener dimensiones de pantalla
        val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = appContext.display
            display?.getRealMetrics(metrics)
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
        }
        
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val dpi = metrics.densityDpi
        
        // Iniciar servicio foreground
        val serviceIntent = Intent(appContext, com.gowayki.nesh.ScreenRecordService::class.java).apply {
            action = com.gowayki.nesh.ScreenRecordService.ACTION_START
            putExtra(com.gowayki.nesh.ScreenRecordService.EXTRA_RESULT_CODE, resultCode)
            putExtra(com.gowayki.nesh.ScreenRecordService.EXTRA_DATA, data)
            putExtra(com.gowayki.nesh.ScreenRecordService.EXTRA_OUTPUT_FILE, outputFile.absolutePath)
            putExtra(com.gowayki.nesh.ScreenRecordService.EXTRA_WIDTH, width)
            putExtra(com.gowayki.nesh.ScreenRecordService.EXTRA_HEIGHT, height)
            putExtra(com.gowayki.nesh.ScreenRecordService.EXTRA_DPI, dpi)
        }
        
        ContextCompat.startForegroundService(appContext, serviceIntent)
        
        recordingStartTime = System.currentTimeMillis()
        _state.value = ScreenRecordingState.RECORDING
        _error.value = null
        
        // Timer para duración
        durationTimer?.cancel()
        durationTimer = scope.launch {
            while (_state.value == ScreenRecordingState.RECORDING) {
                delay(100)
                _recordingDuration.value = System.currentTimeMillis() - recordingStartTime
            }
        }
    }
    
    override suspend fun stopRecording(): Result<String> = runCatching {
        val filePath = currentFilePath.getAndSet(null)
        if (filePath == null) {
            throw IllegalStateException("No hay grabación en curso")
        }
        
        durationTimer?.cancel()
        durationTimer = null
        
        // Detener servicio
        val serviceIntent = Intent(appContext, com.gowayki.nesh.ScreenRecordService::class.java).apply {
            action = com.gowayki.nesh.ScreenRecordService.ACTION_STOP
        }
        appContext.startService(serviceIntent)
        
        _state.value = ScreenRecordingState.PROCESSING
        _recordingDuration.value = 0
        
        // Esperar a que el archivo se escriba completamente
        delay(500)
        
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            throw java.io.IOException("Archivo de video vacío o no creado")
        }
        
        _state.value = ScreenRecordingState.IDLE
        currentResultData.set(null)
        return@runCatching filePath
    }
    
    override suspend fun pauseRecording(): Result<Unit> = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val serviceIntent = Intent(appContext, com.gowayki.nesh.ScreenRecordService::class.java).apply {
                action = com.gowayki.nesh.ScreenRecordService.ACTION_PAUSE
            }
            appContext.startService(serviceIntent)
            _state.value = ScreenRecordingState.PAUSED
        } else {
            throw UnsupportedOperationException("Pausar grabación requiere Android 7.0+")
        }
    }
    
    override suspend fun resumeRecording(): Result<Unit> = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val serviceIntent = Intent(appContext, com.gowayki.nesh.ScreenRecordService::class.java).apply {
                action = com.gowayki.nesh.ScreenRecordService.ACTION_RESUME
            }
            appContext.startService(serviceIntent)
            _state.value = ScreenRecordingState.RECORDING
        } else {
            throw UnsupportedOperationException("Reanudar grabación requiere Android 7.0+")
        }
    }
    
    override suspend fun cancelRecording(): Result<Unit> = runCatching {
        durationTimer?.cancel()
        durationTimer = null
        
        // Detener servicio
        val serviceIntent = Intent(appContext, com.gowayki.nesh.ScreenRecordService::class.java).apply {
            action = com.gowayki.nesh.ScreenRecordService.ACTION_STOP
        }
        appContext.startService(serviceIntent)
        
        val filePath = currentFilePath.getAndSet(null)
        filePath?.let { File(it).delete() }
        
        _state.value = ScreenRecordingState.IDLE
        _recordingDuration.value = 0
        currentResultData.set(null)
    }
    
    override suspend fun hasScreenRecordPermission(): Boolean {
        // MediaProjection no se puede verificar sin solicitar primero
        // Verificamos solo el permiso de audio
        return hasAudioPermission()
    }
    
    override suspend fun requestScreenRecordPermission(): Boolean {
        // En Android, esto se maneja al iniciar grabación
        // Aquí solo verificamos audio
        return hasAudioPermission()
    }
    
    private fun hasAudioPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // En versiones antiguas se concede en instalación
        }
    }
    
    private fun createVideoFile(): File {
        val dir = appContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: appContext.filesDir
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(dir, "WAYKI_SCREEN_${timeStamp}.mp4")
    }
    
    suspend fun destroy() {
        scope.cancel()
        cancelRecording()
    }
}

// Continuación pendiente para reanudar desde onActivityResult
private var pendingContinuation: kotlin.coroutines.Continuation<Pair<Int, Intent?>>? = null

/**
 * Debe ser llamado desde MainActivity.onActivityResult para completar la solicitud de permiso.
 */
fun handleScreenCaptureResult(resultCode: Int, data: Intent?) {
    val continuation = pendingContinuation ?: return
    pendingContinuation = null
    // Resumir la continuación aunque data sea null (p.ej. permiso denegado/cancelado).
    continuation.resume(Pair(resultCode, data))
}

actual fun createScreenRecorder(): ScreenRecorder {
    val ctx = AndroidScreenRecordContext.appContext
        ?: return NoopScreenRecorder()
    return AndroidScreenRecorder(ctx)
}

// Fallback para Preview/tests
private class NoopScreenRecorder : ScreenRecorder {
    private val _state = MutableStateFlow(ScreenRecordingState.IDLE)
    override val state: StateFlow<ScreenRecordingState> = _state.asStateFlow()
    
    private val _error = MutableStateFlow<String?>("Grabación de pantalla no disponible en Preview")
    override val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    override val recordingDuration: StateFlow<Long> = _duration.asStateFlow()
    
    override suspend fun startRecording(): Result<Unit> = Result.failure(UnsupportedOperationException("Noop"))
    override suspend fun stopRecording(): Result<String> = Result.failure(UnsupportedOperationException("Noop"))
    override suspend fun pauseRecording(): Result<Unit> = Result.failure(UnsupportedOperationException("Noop"))
    override suspend fun resumeRecording(): Result<Unit> = Result.failure(UnsupportedOperationException("Noop"))
    override suspend fun cancelRecording(): Result<Unit> = Result.success(Unit)
    override suspend fun hasScreenRecordPermission(): Boolean = false
    override suspend fun requestScreenRecordPermission(): Boolean = false
}
