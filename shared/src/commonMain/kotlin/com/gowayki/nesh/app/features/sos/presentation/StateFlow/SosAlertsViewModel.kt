package com.gowayki.nesh.app.features.sos.presentation.StateFlow

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gowayki.nesh.app.features.reports.domain.model.OsmLatLng
import com.gowayki.nesh.app.features.sos.data.SosAlertsRepository
import com.gowayki.nesh.app.features.sos.domain.model.SosAlert
import com.gowayki.nesh.core.config.AppConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SosAlertsUiState(
    val alerts: List<SosAlert> = emptyList(),
    val isConnected: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val locationShared: Boolean = false,
    val locationError: String? = null,
)

class SosAlertsViewModel : ViewModel() {
    private val apiBaseUrl = AppConfig.SOS_API_BASE_URL.trimEnd('/')
    private val repository = apiBaseUrl.takeIf(String::isNotBlank)?.let(::SosAlertsRepository)

    var ui by mutableStateOf(SosAlertsUiState())
        private set

    init {
        if (repository != null) {
            viewModelScope.launch {
                while (true) {
                    try {
                        ui = ui.copy(
                            alerts = repository.activeAlerts(),
                            isConnected = true,
                            isLoading = false,
                            error = null,
                        )
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        ui = ui.copy(
                            isConnected = false,
                            isLoading = false,
                            error = error.message ?: "No se pudo actualizar las alertas.",
                        )
                    }
                    delay(POLL_INTERVAL_MS)
                }
            }
        } else {
            ui = ui.copy(isLoading = false)
        }
    }

    fun closeAlert(alert: SosAlert) {
        if (repository == null) {
            ui = ui.copy(alerts = ui.alerts.filterNot { it.id == alert.id })
            return
        }
        viewModelScope.launch {
            try {
                repository.closeAlert(alert.id)
                ui = ui.copy(alerts = ui.alerts.filterNot { it.id == alert.id }, error = null)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                ui = ui.copy(error = error.message ?: "No se pudo cerrar la alerta.")
            }
        }
    }

    suspend fun publishPhoneLocation(
        deviceId: String,
        location: OsmLatLng,
        capturedAtEpochMs: Long,
        accuracyMeters: Float,
    ) {
        val service = repository ?: return
        try {
            service.publishPhoneLocation(deviceId, location, capturedAtEpochMs, accuracyMeters)
            ui = ui.copy(locationShared = true, locationError = null)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            ui = ui.copy(
                locationShared = false,
                locationError = error.message ?: "No se pudo compartir la ubicación del teléfono.",
            )
        }
    }

    override fun onCleared() {
        repository?.close()
    }

    private companion object {
        const val POLL_INTERVAL_MS = 3_000L
    }
}
