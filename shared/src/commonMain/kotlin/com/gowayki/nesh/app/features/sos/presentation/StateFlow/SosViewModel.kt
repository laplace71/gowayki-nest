// SosViewModel — puente entre botón físico ESP32 (BLE) y punto GPS en el mapa.
// Vive a nivel raíz del NavHost (no lo recrea la navegación): BLE/GPS persisten.
// Maneja dos tipos de presión: SINGLE (grabar audio) y TRIPLE (SOS/heatmap).
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.sos.presentation.StateFlow

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gowayki.nesh.app.features.communication.domain.model.MessageKind
import com.gowayki.nesh.app.features.communication.domain.model.Conversation
import com.gowayki.nesh.app.features.communication.domain.repository.ChatRepository
import com.gowayki.nesh.app.features.sos.domain.model.AudioRecorder
import com.gowayki.nesh.app.features.sos.domain.model.BleDevice
import com.gowayki.nesh.app.features.sos.domain.model.BleSosClient
import com.gowayki.nesh.app.features.sos.domain.model.GpsFix
import com.gowayki.nesh.app.features.sos.domain.model.GpsProvider
import com.gowayki.nesh.app.features.reports.domain.model.ReportItem
import com.gowayki.nesh.app.features.sos.domain.model.SosMemory
import com.gowayki.nesh.app.features.sos.domain.model.PressType
import com.gowayki.nesh.app.features.sos.domain.model.RecordingState
import com.gowayki.nesh.app.features.sos.domain.model.SosBleState
import com.gowayki.nesh.app.features.sos.domain.model.createAudioRecorder
import com.gowayki.nesh.app.features.sos.domain.model.createBleSosClient
import com.gowayki.nesh.app.features.sos.domain.model.createGpsProvider
import com.gowayki.nesh.app.common.ui.shared.ToastManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class SosUiState(
    val ble: SosBleState = SosBleState(),
    val gps: GpsFix? = null,
    val locating: Boolean = false,
    val lastSosLat: Double? = null,
    val lastSosLon: Double? = null,
    val sosTripRequestSeq: Int = 0,
    val info: String = "Conecta tu ESP32 y presiona el botón (GPIO14).",
    // Nuevo: estado de grabación de audio
    val audioRecording: Boolean = false,
    val audioDuration: Long = 0,
    val audioError: String? = null,
    val lastRecordedFilePath: String? = null,
    val lastRecordedDurationMillis: Long = 0,
    val isPlayingAudio: Boolean = false,
    val audioPlaybackPosition: Long = 0,
    val audioPlaybackDuration: Long = 0,
)

class SosViewModel(
    private val ble: BleSosClient = runCatching { createBleSosClient() }.getOrNull() ?: NoopFallbackBle(),
    private val gps: GpsProvider = runCatching { createGpsProvider() }.getOrNull() ?: NoopFallbackGps(),
    private val audioRecorder: AudioRecorder = runCatching { createAudioRecorder() }.getOrNull() ?: NoopAudioRecorder(),
    private val chatRepository: ChatRepository? = null, // Inyectado desde DI si está disponible
    // Callback para notificar que se envió audio a comunicación
    private val onAudioSent: (conversationId: String) -> Unit = { _ -> },
) : ViewModel() {

    // Handlers registrados por el mapa activo. Sobreviven al cambio de pantalla
    // porque este ViewModel vive a nivel raíz del NavHost.
    var onSosPoint: ((ReportItem) -> Unit)? = null
    var onLocate: ((Double, Double) -> Unit)? = null

    var ui by mutableStateOf(SosUiState())
        private set

    // Dispositivos cercanos estilo WiFi (WAYKI_SOS primero).
    val devices: kotlinx.coroutines.flow.StateFlow<List<BleDevice>> = ble.devices

    // Conversaciones disponibles para enviar la nota de voz (para el chooser).
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    // Auto-conexión para ESP32 portátil: conecta solo al prenderlo.
    var autoConnect by mutableStateOf(true)
        private set

    private var collectJob: Job? = null
    private var autoJob: Job? = null
    private var audioStateJob: Job? = null
    private var audioDurationJob: Job? = null
    private var audioErrorJob: Job? = null
    private var audioPlayingJob: Job? = null
    private var audioPlaybackJob: Job? = null

    init {
        collectJob = viewModelScope.launch {
            ble.state.collectLatest { s -> ui = ui.copy(ble = s) }
        }
        ble.setOnPress { pressType -> handlePress(pressType) }

        // Conversaciones para el chooser de envío de la nota de voz (DI si está disponible).
        viewModelScope.launch {
            chatRepository?.conversations()?.collect { list ->
                _conversations.value = list
            }
        }

        // Observar estado de audio: un job por flow (collectLatest nunca termina, así que
        // deben ser coroutines independientes, no uno solo encadenado).
        audioStateJob = viewModelScope.launch {
            audioRecorder.state.collectLatest { state ->
                ui = ui.copy(audioRecording = (state == com.gowayki.nesh.app.features.sos.domain.model.RecordingState.RECORDING))
            }
        }
        audioDurationJob = viewModelScope.launch {
            audioRecorder.recordingDuration.collectLatest { duration ->
                ui = ui.copy(audioDuration = duration)
            }
        }
        audioErrorJob = viewModelScope.launch {
            audioRecorder.error.collectLatest { error ->
                ui = ui.copy(audioError = error)
            }
        }
        audioPlayingJob = viewModelScope.launch {
            audioRecorder.isPlaying.collectLatest { playing ->
                ui = ui.copy(isPlayingAudio = playing)
            }
        }
        audioPlaybackJob = viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                audioRecorder.playbackPosition,
                audioRecorder.playbackDuration,
            ) { position, duration -> position to duration }
                .collectLatest { (position, duration) ->
                    ui = ui.copy(
                        audioPlaybackPosition = position,
                        audioPlaybackDuration = duration,
                    )
                }
        }

        // GPS inicial (último conocido, sin bloquear).
        viewModelScope.launch {
            runCatching { gps.lastFix() }.getOrNull()?.let { ui = ui.copy(gps = it) }
        }
        // Al entrar busca el ESP32 solo; si se apaga/pierde, reintenta cada 8 s.
        autoJob = viewModelScope.launch {
            delay(500)
            while (true) {
                ble.autoJoin = autoConnect
                val s = ble.state.value
                if (autoConnect && !s.connected && !s.scanning) {
                    ui = ui.copy(info = "Buscando WAYKI_SOS por Bluetooth…")
                    runCatching { ble.startScan() }
                }
                delay(8_000)
            }
        }
    }

    // Conexión manual desde la lista estilo WiFi.
    fun connectDevice(d: BleDevice) {
        autoConnect = true
        ble.autoJoin = true
        ui = ui.copy(info = "Conectando a ${d.name}…")
        runCatching { ble.connectDevice(d.address) }
    }

    fun connect() {
        autoConnect = true
        ui = ui.copy(info = "Buscando WAYKI_SOS por Bluetooth…")
        ble.startScan()
    }

    fun disconnect() {
        autoConnect = false
        ble.stop()
        ui = ui.copy(info = "Desconectado.")
    }

    fun refreshGps() {
        viewModelScope.launch {
            ui = ui.copy(locating = true, info = "Obteniendo GPS real…")
            val fix = runCatching { gps.currentFix() }.getOrNull()
                ?: runCatching { gps.lastFix() }.getOrNull()
            ui = if (fix != null) {
                ui.copy(gps = fix, locating = false, info = "GPS listo: ${fix.lat}, ${fix.lon}")
            } else {
                ui.copy(locating = false, info = "Sin GPS: activa ubicación y acepta permiso.")
            }
        }
    }

    // Punto manual (prueba sin ESP32): usa GPS real igual que el botón físico.
    fun simulatePress() = handlePress(PressType.TRIPLE)

    // Punto manual para prueba de audio (presión simple).
    fun simulateAudioPress() = handlePress(PressType.SINGLE)

    // Calibrar mi posición: GPS fresco y vuela el mapa hasta ahí (sin crear punto).
    fun locateMe() {
        viewModelScope.launch {
            ui = ui.copy(locating = true, info = "Ubicando tu posición…")
            val fix = runCatching { gps.currentFix() }.getOrNull()
                ?: runCatching { gps.lastFix() }.getOrNull()
                ?: ui.gps
            if (fix == null) {
                ui = ui.copy(locating = false, info = "Sin GPS: activa ubicación.")
                return@launch
            }
            ui = ui.copy(
                gps = fix,
                locating = false,
                info = "Mi posición: ${fix.lat}, ${fix.lon}",
            )
            try {
                onLocate?.invoke(fix.lat, fix.lon)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handlePress(pressType: PressType) {
        when (pressType) {
            PressType.SINGLE -> handleSinglePress()
            PressType.TRIPLE -> handleTriplePress()
        }
    }

    private fun handleSinglePress() {
        viewModelScope.launch {
            ui = ui.copy(info = "Iniciando grabación de audio…", audioError = null)
            // Reproducir beep de confirmación
            runCatching { audioRecorder.playSingleBeep() }

            // Verificar permiso (si falta, se solicita en runtime).
            val hasPermission = runCatching { audioRecorder.hasRecordPermission() }.getOrDefault(false)
            if (!hasPermission) {
                val granted = runCatching { audioRecorder.requestRecordPermission() }.getOrDefault(false)
                if (!granted) {
                    ui = ui.copy(
                        info = "Permiso de micrófono requerido",
                        audioError = "Concede permiso de micrófono en ajustes",
                    )
                    return@launch
                }
            }

            // Iniciar grabación
            val result = runCatching { audioRecorder.startRecording() }.getOrDefault(Result.failure(Exception("Error al iniciar grabación")))
            result.onFailure { e ->
                ui = ui.copy(info = "Error al grabar", audioError = e.message ?: "Error desconocido")
            }
            result.onSuccess { _ ->
                ui = ui.copy(info = "Grabando… mantén el botón o presiona de nuevo para enviar")
                com.gowayki.nesh.core.mqtt.MqttClient.publish(
                    com.gowayki.nesh.core.mqtt.MqttTopics.RECORDING,
                    com.gowayki.nesh.core.mqtt.MqttTopics.MSG_RECORDING
                )
            }
        }
    }

    private fun handleTriplePress() {
        viewModelScope.launch {
            // Reproducir triple beep para SOS
            runCatching { audioRecorder.playTripleBeep() }

            ToastManager.show("¡SOS activado! Marcando punto GPS…")
            ui = ui.copy(locating = true, info = "¡SOS recibido! Marcando punto GPS…")
            val fix = runCatching { gps.currentFix() }.getOrNull()
                ?: runCatching { gps.lastFix() }.getOrNull()
                ?: ui.gps
            if (fix == null) {
                ui = ui.copy(locating = false, info = "SOS sin GPS: activa ubicación.")
                return@launch
            }
            ui = ui.copy(
                gps = fix,
                locating = false,
                lastSosLat = fix.lat,
                lastSosLon = fix.lon,
                sosTripRequestSeq = ui.sosTripRequestSeq + 1,
                info = "SOS marcado en ${fix.lat}, ${fix.lon}",
            )
            // Crea el punto en memoria (fuente única y observable) y lo persiste.
            val item = runCatching { SosMemory.addEmergency(fix.lat, fix.lon) }.getOrNull()
            if (item != null) {
                viewModelScope.launch { runCatching { SosMemory.persist() } }
                // Notifica al mapa activo (si está registrado) para volar al punto.
                try {
                    onSosPoint?.invoke(item)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                ui = ui.copy(info = "Error al crear punto SOS", locating = false)
            }
        }
    }

    /**
     * Detiene la grabación y guarda el audio para previsualizar.
     * Si se indica [conversationId], además envía la nota de voz a esa conversación.
     * Al finalizar, el audio queda en PREVIEW en la barra (no se envía ni se descarta solo).
     */
    fun stopAndSendAudio(conversationId: String? = null) {
        viewModelScope.launch {
            ui = ui.copy(info = "Procesando audio…")
            val recordedMillis = ui.audioDuration
            val result = runCatching { audioRecorder.stopRecording() }.getOrDefault(Result.failure(Exception("Error al detener grabación")))
            result.onFailure { e ->
                ui = ui.copy(info = "Error al guardar audio", audioError = e.message ?: "Error desconocido")
                return@launch
            }
            result.onSuccess { filePath ->
                ui = ui.copy(
                    info = if (conversationId == null)
                        "Audio guardado. Previsualiza antes de enviar."
                    else
                        "Audio guardado, enviando…",
                    lastRecordedFilePath = filePath,
                    lastRecordedDurationMillis = recordedMillis,
                )
                if (conversationId != null) {
                    sendAudioToCommunication(filePath, conversationId)
                }
            }
        }
    }

    /** Envía la grabación guardada (preview) a la conversación elegida. */
    fun sendRecordedAudio(conversationId: String) {
        val filePath = ui.lastRecordedFilePath ?: return
        sendAudioToCommunication(filePath, conversationId)
    }

    /** Reproduce la última grabación */
    fun playLastRecording() {
        val filePath = ui.lastRecordedFilePath ?: return
        viewModelScope.launch {
            runCatching { audioRecorder.playAudio(filePath) }
        }
    }

    /** Detiene la reproducción */
    fun stopPlayback() {
        viewModelScope.launch {
            runCatching { audioRecorder.stopPlayback() }
        }
    }

    /** Cancela la grabación actual sin enviar */
    fun cancelAudioRecording() {
        viewModelScope.launch {
            runCatching { audioRecorder.cancelRecording() }
            ui = ui.copy(info = "Grabación cancelada", audioError = null)
        }
    }

    /** Descarta la grabación guardada sin enviar */
    fun discardRecordedAudio() {
        ui = ui.copy(lastRecordedFilePath = null, lastRecordedDurationMillis = 0, info = "Grabación descartada")
    }

    /** Cierra el aviso de error de audio. */
    fun clearAudioError() {
        ui = ui.copy(audioError = null)
    }

    private fun sendAudioToCommunication(filePath: String, conversationId: String?) {
        viewModelScope.launch {
            val repo = chatRepository ?: return@launch
            val targetConversationId = conversationId ?: getDefaultConversationId()
                ?: run {
                    ui = ui.copy(info = "Selecciona un grupo/contacto en Comunicación")
                    return@launch
                }

            // Calcular duración aproximada del archivo (o usar la grabada)
            val durationSeconds = (ui.audioDuration / 1000).toInt().coerceAtLeast(1)

            val result = repo.sendMessage(
                conversationId = targetConversationId,
                text = "Nota de voz",
                kind = MessageKind.AUDIO,
                audioDurationSeconds = durationSeconds,
                audioFilePath = filePath
            )

            result.onSuccess { message ->
                ui = ui.copy(
                    info = "✅ Audio enviado a ${getConversationTitle(targetConversationId)}\n📁 Guardado en: $filePath",
                    lastRecordedFilePath = null,
                )
                onAudioSent(targetConversationId)
            }
            result.onFailure { e ->
                ui = ui.copy(info = "Error al enviar", audioError = e.message ?: "Error desconocido")
            }
        }
    }

    private fun getDefaultConversationId(): String? {
        // Usar el primer grupo del fake data para pruebas
        return "g-linea7"
    }

    private fun getConversationTitle(conversationId: String): String {
        return when (conversationId) {
            "g-linea7" -> "GRUPO · Línea 7 · Central"
            "p-marco" -> "Privado · Marco Rojas"
            "g-operativos" -> "GRUPO · Operativos · Línea 7"
            "p-jefe" -> "Privado · Jefe de línea"
            else -> "Conversación"
        }
    }

    override fun onCleared() {
        runCatching { ble.stop() }
        viewModelScope.launch { runCatching { audioRecorder.cancelRecording() } }
        collectJob?.cancel()
        autoJob?.cancel()
        audioStateJob?.cancel()
        audioDurationJob?.cancel()
        audioErrorJob?.cancel()
        audioPlayingJob?.cancel()
        audioPlaybackJob?.cancel()
    }
}

// Fallbacks internos por si el factory platform falla (nunca crashea la app).
private class NoopFallbackBle : BleSosClient {
    private val s = kotlinx.coroutines.flow.MutableStateFlow(SosBleState())
    override val state: kotlinx.coroutines.flow.StateFlow<SosBleState> = s
    private val d = kotlinx.coroutines.flow.MutableStateFlow(emptyList<BleDevice>())
    override val devices: kotlinx.coroutines.flow.StateFlow<List<BleDevice>> = d
    override var autoJoin: Boolean = false
    override fun startScan() {}
    override fun stop() {}
    override fun connectDevice(address: String) {}
    override fun setOnPress(listener: (PressType) -> Unit) {}
}

private class NoopFallbackGps : GpsProvider {
    override suspend fun currentFix(): GpsFix? = null
    override suspend fun lastFix(): GpsFix? = null
}

private class NoopAudioRecorder : AudioRecorder {
    private val _state = kotlinx.coroutines.flow.MutableStateFlow(RecordingState.IDLE)
    override val state: kotlinx.coroutines.flow.StateFlow<RecordingState> = _state.asStateFlow()
    private val _error = kotlinx.coroutines.flow.MutableStateFlow<String?>("Audio no disponible")
    override val error: kotlinx.coroutines.flow.StateFlow<String?> = _error.asStateFlow()
    private val _duration = kotlinx.coroutines.flow.MutableStateFlow(0L)
    override val recordingDuration: kotlinx.coroutines.flow.StateFlow<Long> = _duration.asStateFlow()
    private val _isPlaying = kotlinx.coroutines.flow.MutableStateFlow(false)
    override val isPlaying: kotlinx.coroutines.flow.StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _playbackPosition = kotlinx.coroutines.flow.MutableStateFlow(0L)
    override val playbackPosition: kotlinx.coroutines.flow.StateFlow<Long> = _playbackPosition.asStateFlow()
    private val _playbackDuration = kotlinx.coroutines.flow.MutableStateFlow(0L)
    override val playbackDuration: kotlinx.coroutines.flow.StateFlow<Long> = _playbackDuration.asStateFlow()

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
