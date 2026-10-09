package com.gowayki.nesh.app.features.sos.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.app.features.reports.domain.model.LimaPlaza
import com.gowayki.nesh.app.features.reports.domain.model.OsmMarkerType
import com.gowayki.nesh.app.features.reports.presentation.StateFlow.ReportsViewModel
import com.gowayki.nesh.app.features.reports.presentation.components.OsmGeoCircle
import com.gowayki.nesh.app.features.reports.presentation.components.OsmMap
import com.gowayki.nesh.app.features.reports.presentation.components.OsmMapState
import com.gowayki.nesh.app.features.reports.presentation.components.OsmMarker
import com.gowayki.nesh.app.features.sos.domain.model.SosAlert
import com.gowayki.nesh.app.features.sos.data.rememberPhoneLocation
import com.gowayki.nesh.app.features.sos.presentation.StateFlow.SosAlertsViewModel
import com.gowayki.nesh.core.config.AppConfig
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.MapPin
import compose.icons.feathericons.RefreshCw

private val SosRed = Color(0xFFC64049)

@Composable
fun SosScreen(
    onBack: () -> Unit = {},
    vm: SosAlertsViewModel = viewModel { SosAlertsViewModel() },
    mapVm: ReportsViewModel = viewModel { ReportsViewModel() },
) {
    val ui = vm.ui
    var shareMobileLocation by remember { mutableStateOf(false) }
    var mobileLocationToken by remember { mutableStateOf("") }
    val phoneLocation = rememberPhoneLocation(
        enabled = shareMobileLocation && AppConfig.SOS_API_BASE_URL.isNotBlank(),
    )
    val map = remember { OsmMapState(center = LimaPlaza, zoom = 15) }
    val markers = remember(ui.alerts) {
        ui.alerts.filter { it.status == "ACTIVE" }
            .map { OsmMarker(it.id, it.position, OsmMarkerType.EMERGENCIA) }
    }
    val circles = remember(ui.alerts) {
        ui.alerts.filter { it.status == "ACTIVE" }
            .map { OsmGeoCircle(it.id, it.position, ALERT_RADIUS_METERS, showCenterMarker = true) }
    }
    val activeAlertIds = remember(ui.alerts) {
        ui.alerts.filter { it.status == "ACTIVE" }.map { it.id }
    }

    LaunchedEffect(activeAlertIds) {
        ui.alerts.firstOrNull { it.status == "ACTIVE" }?.let { map.flyTo(it.position) }
    }
    LaunchedEffect(
        shareMobileLocation,
        mobileLocationToken,
        phoneLocation.location,
        phoneLocation.capturedAtEpochMs,
        phoneLocation.accuracyMeters,
    ) {
        val location = phoneLocation.location ?: return@LaunchedEffect
        val capturedAt = phoneLocation.capturedAtEpochMs ?: return@LaunchedEffect
        val accuracy = phoneLocation.accuracyMeters ?: return@LaunchedEffect
        if (!shareMobileLocation || mobileLocationToken.length < 32 || accuracy > MAX_PHONE_ACCURACY_METERS) {
            return@LaunchedEffect
        }
        vm.publishPhoneLocation(
            token = mobileLocationToken,
            deviceId = SOS_DEVICE_ID,
            location = location,
            capturedAtEpochMs = capturedAt,
            accuracyMeters = accuracy,
        )
    }

    Box(Modifier.fillMaxSize().background(AppColors.cream)) {
        OsmMap(
            state = map,
            tiles = mapVm.tiles,
            markers = markers,
            geoCircles = circles,
            selectedId = ui.alerts.firstOrNull { it.status == "ACTIVE" }?.id,
            grayScale = false,
            onNeedTiles = mapVm::ensureTiles,
            onMarkerClick = { map.flyTo(it.pos) },
            markerContent = { _, _ -> },
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WaykiBackButton(onClick = onBack)
                    Spacer(Modifier.size(16.dp))
                    Column {
                        Text("S.O.S.", style = AppTextStyles.waykiHeader, color = AppColors.ink)
                        Text(
                            "Emergencias cercanas",
                            style = AppTextStyles.waykiLabelMedium,
                            color = AppColors.ink,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${ui.alerts.count { it.status == "ACTIVE" }} activas",
                        style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                        color = SosRed,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .padding(end = 5.dp)
                                .size(7.dp)
                                .background(
                                    if (ui.isConnected) Color(0xFF3A8F64) else AppColors.slate,
                                    CircleShape,
                                ),
                        )
                        Text(
                            text = if (AppConfig.SOS_API_BASE_URL.isBlank()) {
                                "Modo de prueba"
                            } else if (ui.isConnected) {
                                "En línea"
                            } else {
                                "Sin conexión"
                            },
                            style = AppTextStyles.waykiMini,
                            color = AppColors.ink,
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (AppConfig.SOS_API_BASE_URL.isNotBlank()) {
                    PhoneLocationCard(
                        enabled = shareMobileLocation,
                        token = mobileLocationToken,
                        permissionGranted = phoneLocation.permissionGranted,
                        location = phoneLocation.location,
                        error = phoneLocation.error,
                        onTokenChange = { mobileLocationToken = it },
                        onToggle = {
                            val wasSharing = shareMobileLocation
                            shareMobileLocation = !wasSharing
                            if (wasSharing) mobileLocationToken = ""
                        },
                    )
                }
                ui.error?.let { error ->
                    InfoCard(text = "No se actualizan las alertas: $error")
                }
                if (mapVm.tileLoadError) {
                    InfoCard(text = "No se pudo cargar el mapa de OpenStreetMap. Comprueba la conexión e inténtalo de nuevo.")
                }

                if (ui.isLoading) {
                    InfoCard(text = "Conectando con el servicio SOS…", loading = true)
                }

                ui.alerts.filter { it.status == "ACTIVE" }.forEach { alert ->
                    SosAlertCard(alert = alert, onClose = { vm.closeAlert(alert) })
                }

                if (AppConfig.SOS_API_BASE_URL.isBlank()) {
                    InfoCard(
                        text = "Prueba local: la alerta usa coordenadas de muestra de Lima y no representa una emergencia real.",
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppColors.cream)
                            .border(1.dp, AppColors.primary, RoundedCornerShape(16.dp))
                            .clickable { vm.simulateAlert() }
                            .padding(15.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Simular alerta SOS",
                            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.ink,
                        )
                    }
                } else if (ui.alerts.isEmpty() && !ui.isLoading && ui.error == null) {
                    InfoCard(text = "No hay alertas SOS activas.")
                }

                Text(
                    text = "© OpenStreetMap",
                    style = AppTextStyles.waykiMini.copy(fontSize = 10.sp),
                    color = AppColors.ink,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun SosAlertCard(alert: SosAlert, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = AppColors.ink.copy(alpha = 0.18f))
            .background(AppColors.cream, RoundedCornerShape(22.dp))
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(SosRed, CircleShape))
                Spacer(Modifier.size(7.dp))
                Text(
                    if (alert.simulated) "Alerta simulada" else "Emergencia activa",
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                    color = SosRed,
                )
            }
            Text(
                alert.deviceId,
                style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                color = AppColors.ink,
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                FeatherIcons.MapPin,
                contentDescription = null,
                tint = AppColors.slate,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${alert.latitude}, ${alert.longitude}",
                    style = AppTextStyles.waykiLabelMedium,
                    color = AppColors.ink,
                )
                Text(
                    if (alert.simulated) "Ubicación ficticia de prueba" else "Coordenadas recibidas del dispositivo",
                    style = AppTextStyles.waykiMini,
                    color = AppColors.slate,
                )
            }
            Text(
                "Cerrar",
                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                color = SosRed,
                modifier = Modifier.clickable(onClick = onClose).padding(8.dp),
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            if (alert.simulated) "Hora: prueba local (no real)" else "Activada: ${alert.occurredAt}",
            style = AppTextStyles.waykiMini,
            color = AppColors.slate,
        )

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Radio geográfico",
                style = AppTextStyles.waykiMini,
                color = AppColors.slate,
            )
            Text(
                "250 m",
                style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                color = AppColors.ink,
            )
        }
    }
}

@Composable
private fun InfoCard(text: String, loading: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.cream.copy(alpha = 0.96f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = SosRed,
            )
        } else {
            Icon(
                FeatherIcons.RefreshCw,
                contentDescription = null,
                tint = AppColors.slate,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(text, style = AppTextStyles.waykiMini, color = AppColors.ink)
    }
}

@Composable
private fun PhoneLocationCard(
    enabled: Boolean,
    token: String,
    permissionGranted: Boolean,
    location: com.gowayki.nesh.app.features.reports.domain.model.OsmLatLng?,
    error: String?,
    onTokenChange: (String) -> Unit,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.cream.copy(alpha = 0.98f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "GPS de este teléfono → $SOS_DEVICE_ID",
            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
            color = AppColors.ink,
        )
        Text(
            when {
                !enabled -> "Comparte la ubicación solo mientras esta pantalla permanezca abierta."
                error != null -> error
                !permissionGranted -> "Esperando permiso de ubicación…"
                location == null -> "Esperando una lectura GPS reciente…"
                else -> "GPS activo · ${location.lat}, ${location.lon}"
            },
            style = AppTextStyles.waykiMini,
            color = if (error != null) SosRed else AppColors.slate,
        )
        if (!enabled) {
            BasicTextField(
                value = token,
                onValueChange = onTokenChange,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                textStyle = AppTextStyles.waykiMini.copy(color = AppColors.ink),
                decorationBox = { innerTextField ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(10.dp))
                            .padding(horizontal = 11.dp, vertical = 10.dp),
                    ) {
                        if (token.isEmpty()) {
                            Text("Token SOS_MOBILE_LOCATION_TOKEN", style = AppTextStyles.waykiMini, color = AppColors.slate)
                        }
                        innerTextField()
                    }
                },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (enabled) SosRed.copy(alpha = 0.12f) else AppColors.primary, RoundedCornerShape(12.dp))
                .clickable(enabled = enabled || token.length >= 32, onClick = onToggle)
                .padding(11.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (enabled) "Dejar de compartir GPS" else "Compartir GPS mientras la app esté abierta",
                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                color = if (enabled) SosRed else AppColors.ink,
            )
        }
    }
}

private const val ALERT_RADIUS_METERS = 250.0
private const val MAX_PHONE_ACCURACY_METERS = 100f
private const val SOS_DEVICE_ID = "ESP32-AQP-017"
