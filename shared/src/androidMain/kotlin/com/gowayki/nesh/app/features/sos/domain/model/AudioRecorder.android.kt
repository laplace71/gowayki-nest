// Implementación Android real: MediaRecorder para audio + ToneGenerator para beeps + MediaPlayer para reproducción.
// Requiere permiso RECORD_AUDIO (ya en Manifest).
@file:Suppress("SpellCheckingInspection", "MissingPermission", "DEPRECATION")
package com.gowayki.nesh.app.features.sos.domain.model

import android.Manifest
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.Build
import android.os.Environment
import com.gowayki.nesh.app.di.AppDi
import kotlinx.coroutines.CancellableContinuation
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
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

object AndroidAudioContext {
    var appContext: Context? = null
    /** Activity actual: necesaria para pedir el permiso de micrófono en runtime. */
    @Volatile
    var activity: WeakReference<android.app.Activity>? = null

    internal var pendingPermission: CancellableContinuation<Boolean>? = null
}

/** Código de request del permiso RECORD_AUDIO (lo usa MainActivity). */
const val REQUEST_CODE_RECORD_AUDIO = 9102

/** Reenvía el resultado de onRequestPermissionsResult a la corrutina que pidió el permiso. */
fun handleRecordPermissionResult(requestCode: Int, grantResults: IntArray) {
    if (requestCode != REQUEST_CODE_RECORD_AUDIO) return
    val cont = AndroidAudioContext.pendingPermission
    AndroidAudioContext.pendingPermission = null
    val granted = grantResults.isNotEmpty() &&
        grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
    cont?.resume(granted)
}

private class AndroidAudioRecorder(
    private val appContext: Context,
) : AudioRecorder {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(RecordingState.IDLE)
    override val state: StateFlow<RecordingState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0L)
    override val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    override val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _playbackDuration = MutableStateFlow(0L)
    override val playbackDuration: StateFlow<Long> = _playbackDuration.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null
    private var recordingStartTime: Long = 0
    private var durationTimer: Job? = null
    private var playbackTimer: Job? = null
    private val currentFilePath = AtomicReference<String?>(null)

    init {
        try {
            toneGenerator = ToneGenerator(android.media.AudioManager.STREAM_ALARM, 80)
        } catch (_: Exception) {
            // ToneGenerator puede fallar en algunos dispositivos
        }
    }

    override suspend fun startRecording(): Result<Unit> = runCatching {
        // Verificar permiso; si falta, se pide en runtime (Activity registrada).
        if (!hasRecordPermission() && !requestRecordPermission()) {
            throw SecurityException("Permiso RECORD_AUDIO no concedido")
        }

        // Preparar archivo de salida
        val outputFile = createAudioFile()
        currentFilePath.set(outputFile.absolutePath)

        // Configurar MediaRecorder
        // Intenta con VOICE_RECOGNITION (mejor calidad de voz y supresión de ruido);
        // si el dispositivo no lo soporta, cae a MIC.
        mediaRecorder = try {
            MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setAudioChannels(1)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
        } catch (_: Exception) {
            MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setAudioChannels(1)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
        }

        recordingStartTime = System.currentTimeMillis()
        _state.value = RecordingState.RECORDING
        _error.value = null

        // Timer para duración
        durationTimer?.cancel()
        durationTimer = scope.launch {
            while (_state.value == RecordingState.RECORDING) {
                delay(100)
                _recordingDuration.value = System.currentTimeMillis() - recordingStartTime
            }
        }

        // Reproducir beep de inicio
        playBeepInternal(1)
    }

    override suspend fun stopRecording(): Result<String> = runCatching {
        val filePath = currentFilePath.getAndSet(null)
        if (filePath == null) {
            throw IllegalStateException("No hay grabación en curso")
        }

        durationTimer?.cancel()
        durationTimer = null

        mediaRecorder?.apply {
            try {
                stop()
            } catch (_: Exception) {
                // Ignorar error al parar
            }
            release()
        }
        mediaRecorder = null

        _state.value = RecordingState.PROCESSING
        _recordingDuration.value = 0

        // Pequeña pausa para asegurar que el archivo se escriba
        delay(100)

        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            throw IOException("Archivo de audio vacío o no creado")
        }

        _state.value = RecordingState.IDLE
        return@runCatching filePath
    }

    override suspend fun cancelRecording(): Result<Unit> = runCatching {
        durationTimer?.cancel()
        durationTimer = null

        mediaRecorder?.apply {
            try { stop() } catch (_: Exception) {}
            release()
        }
        mediaRecorder = null

        val filePath = currentFilePath.getAndSet(null)
        filePath?.let { File(it).delete() }

        _state.value = RecordingState.IDLE
        _recordingDuration.value = 0
    }

    override suspend fun playSingleBeep(): Result<Unit> = runCatching {
        playBeepInternal(1)
    }

    override suspend fun playTripleBeep(): Result<Unit> = runCatching {
        playBeepInternal(3)
    }

    override suspend fun playAudio(filePath: String): Result<Unit> = runCatching {
        val isRemote = filePath.startsWith("http://") || filePath.startsWith("https://")
        if (!isRemote) {
            val file = File(filePath)
            if (!file.exists()) {
                throw IOException("Archivo no encontrado: $filePath")
            }
        }

        playbackTimer?.cancel()
        playbackTimer = null
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }

        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            _playbackDuration.value = duration.toLong()
            _playbackPosition.value = 0L
            start()
            _isPlaying.value = true

            setOnCompletionListener {
                _playbackPosition.value = 0L
                _isPlaying.value = false
                it.release()
                mediaPlayer = null
            }
            setOnErrorListener { _, _, _ ->
                _isPlaying.value = false
                true // Manejado
            }
        }

        // Sondeo de posición para la barra de progreso de la nota de voz.
        playbackTimer?.cancel()
        playbackTimer = scope.launch {
            while (_isPlaying.value) {
                runCatching {
                    mediaPlayer?.let { p ->
                        _playbackPosition.value = p.currentPosition.toLong()
                        if (p.duration > 0) _playbackDuration.value = p.duration.toLong()
                    }
                }
                delay(120)
            }
        }
    }

    override suspend fun stopPlayback(): Result<Unit> = runCatching {
        playbackTimer?.cancel()
        playbackTimer = null
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _playbackPosition.value = 0L
        _playbackDuration.value = 0L
    }

    private fun playBeepInternal(count: Int) {
        toneGenerator?.let { tg ->
            scope.launch {
                repeat(count) { i ->
                    if (i > 0) delay(150) // Pausa entre beeps
                    try {
                        tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                        delay(200)
                    } catch (_: Exception) {
                        // Ignorar errores de tono
                    }
                }
            }
        }
    }

    override suspend fun hasRecordPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // En versiones antiguas se concede en instalación
        }
    }

    override suspend fun requestRecordPermission(): Boolean {
        // Pide el permiso desde la Activity registrada y espera la respuesta.
        if (hasRecordPermission()) return true
        val activity = AndroidAudioContext.activity?.get() ?: return false
        if (AndroidAudioContext.pendingPermission != null) return false

        return suspendCancellableCoroutine { cont ->
            AndroidAudioContext.pendingPermission = cont
            cont.invokeOnCancellation { AndroidAudioContext.pendingPermission = null }
            try {
                activity.requestPermissions(
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_CODE_RECORD_AUDIO,
                )
            } catch (_: Exception) {
                AndroidAudioContext.pendingPermission = null
                cont.resume(false)
            }
        }
    }

    private fun createAudioFile(): File {
        val dir = appContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: appContext.filesDir
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(dir, "WAYKI_AUDIO_${timeStamp}.m4a")
    }

    suspend fun destroy() {
        scope.cancel()
        cancelRecording()
        stopPlayback()
        toneGenerator?.release()
        toneGenerator = null
    }
}

actual fun createAudioRecorder(): AudioRecorder {
    val ctx = AndroidAudioContext.appContext
        ?: return NoopAudioRecorder()
    return AndroidAudioRecorder(ctx)
}

// Fallback para Preview/tests
private class NoopAudioRecorder : AudioRecorder {
    private val _state = MutableStateFlow(RecordingState.IDLE)
    override val state: StateFlow<RecordingState> = _state.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()
    private val _duracion = MutableStateFlow(0L)
    override val recordingDuration: StateFlow<Long> = _duracion.asStateFlow()
    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _playbackPosition = MutableStateFlow(0L)
    override val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()
    private val _playbackDuration = MutableStateFlow(0L)
    override val playbackDuration: StateFlow<Long> = _playbackDuration.asStateFlow()

    override suspend fun startRecording(): Result<Unit> = Result.failure(UnsupportedOperationException("Noop"))
    override suspend fun stopRecording(): Result<String> = Result.failure(UnsupportedOperationException("Noop"))
    override suspend fun cancelRecording(): Result<Unit> = Result.failure(UnsupportedOperationException("Noop"))
    override suspend fun playSingleBeep(): Result<Unit> = Result.success(Unit)
    override suspend fun playTripleBeep(): Result<Unit> = Result.success(Unit)
    override suspend fun playAudio(filePath: String): Result<Unit> = Result.success(Unit)
    override suspend fun stopPlayback(): Result<Unit> = Result.success(Unit)
    override suspend fun hasRecordPermission(): Boolean = false
    override suspend fun requestRecordPermission(): Boolean = false
}