package com.gowayki.nesh.app.features.sos.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gowayki.nesh.app.common.ui.shared.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.app.features.reports.domain.model.ReportItem
import com.gowayki.nesh.app.features.reports.presentation.StateFlow.ReportsViewModel
import com.gowayki.nesh.app.features.reports.presentation.components.OsmMarker
import com.gowayki.nesh.app.common.util.currentTimeMillis
import com.gowayki.nesh.app.features.sos.domain.model.SosMemory
import com.gowayki.nesh.app.features.sos.presentation.components.SosBleDevices
import com.gowayki.nesh.app.features.sos.presentation.components.SosHeatMap
import com.gowayki.nesh.app.features.sos.presentation.StateFlow.SosViewModel
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronDown
import compose.icons.feathericons.Crosshair
import compose.icons.feathericons.Maximize2
import compose.icons.feathericons.Minimize2
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.MapPin

@Composable
fun SosScreen(
    onBack: () -> Unit = {},
    onMapaClick: () -> Unit = {},
    // Mapa optimizado reutilizado de reportes (tiles + gestos) + BLE botón físico.
    // Defaults: la navegación existente (sidebar → SOS) sigue funcionando sin cambios.
    reportsVm: ReportsViewModel = viewModel { ReportsViewModel() },
    sosVm: SosViewModel = viewModel {
        SosViewModel(
            onSosPoint = { lat, lon -> reportsVm.addSosEmergency(lat, lon) },
            onLocate = { lat, lon -> reportsVm.focusOn(lat, lon) },
        )
    },
) {
    BackHandler(true) { onBack() }
    val reports = reportsVm.ui.reports
    val markers = remember(reports) { reports.map { OsmMarker(it.id, it.pos, it.type) } }
    // Solo puntos SOS del botón (sin presets): calor + tarjetas.
    val sosPoints = remember(reports) { reports.filter { it.id.startsWith("SOS-") } }
    val activeCount = sosPoints.size
    // Mapa minimizable / maximizable (no ocupa toda la pantalla).
    var mapMax by remember { mutableStateOf(false) }
    val mapH by animateDpAsState(
        targetValue = if (mapMax) 420.dp else 200.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "sosMapH",
    )
    // Al salir de la ventana se guarda la cámara para no reiniciar al volver.
    DisposableEffect(Unit) {
        onDispose {
            SosMemory.lastCenter = reportsVm.map.center
            SosMemory.lastZoom = reportsVm.map.zoom
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.cream)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SosHeader(onBack = onBack, activeCount = activeCount)

        // Mapa compacto: Mapbox en Android (heatmap nativo), OSM en iOS.
        // Solo puntos con calor, sin líneas de ruta. Pins centrados por SDK.
        Box(
            Modifier.fillMaxWidth()
                .height(mapH)
                .clip(RoundedCornerShape(20.dp))
                .background(AppColors.steel.copy(alpha = 0.25f)),
        ) {
            SosHeatMap(
                markers = markers,
                selectedId = reportsVm.ui.selectedId,
                reportsVm = reportsVm,
                onMarkerClick = { reportsVm.select(it.id) },
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp)
                    .size(40.dp)
                    .background(AppColors.cream.copy(alpha = 0.94f), CircleShape)
                    .clickable { mapMax = !mapMax },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (mapMax) FeatherIcons.Minimize2 else FeatherIcons.Maximize2,
                    null, tint = AppColors.ink, modifier = Modifier.size(20.dp),
                )
            }
            // Calibrar / mi posición: centra el mapa en tu GPS real.
            Box(
                Modifier.align(Alignment.BottomStart).padding(8.dp)
                    .size(40.dp)
                    .background(AppColors.cream.copy(alpha = 0.94f), CircleShape)
                    .clickable { sosVm.locateMe() },
                contentAlignment = Alignment.Center,
            ) {
                if (sosVm.ui.locating) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = AppColors.ink)
                } else {
                    Icon(FeatherIcons.Crosshair, null, tint = AppColors.ink, modifier = Modifier.size(20.dp))
                }
            }
        }

        // BLE botón físico ESP32 → punto GPS real en este mapa.
        SosBleBar(vm = sosVm, activeCount = activeCount)

        // Tarjetas apiladas (LazyColumn), se eliminan deslizando.
        if (sosPoints.isEmpty()) {
            Text(
                "Aún no hay emergencias: presiona el botón físico para marcar tu punto exacto.",
                color = AppColors.slate,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(sosPoints, key = { it.id }) { p ->
                    SosSwipeCard(
                        point = p,
                        reportsVm = reportsVm,
                        accuracyM = sosVm.ui.gps?.accuracyM,
                        onDelete = { reportsVm.removeReport(p.id) },
                        // Apretar la tarjeta vuela al punto en el mapa.
                        onCardClick = { reportsVm.select(p.id) },
                    )
                }
            }
        }
    }
}

// Tarjeta con su zona + swipe para eliminar.
@Composable
private fun SosSwipeCard(
    point: ReportItem,
    reportsVm: ReportsViewModel,
    accuracyM: Float?,
    onDelete: () -> Unit,
    onCardClick: () -> Unit,
) {
    var zone by remember(point.id) { mutableStateOf<String?>(null) }
    var zoneDone by remember(point.id) { mutableStateOf(false) }
    LaunchedEffect(point.id) {
        zone = runCatching { reportsVm.zoneName(point.pos.lat, point.pos.lon) }.getOrNull()
        zoneDone = true
    }
    val dismiss = rememberSwipeToDismissBoxState()
    // Al deslizar fuera se elimina el punto (y se persiste).
    if (dismiss.currentValue != SwipeToDismissBoxValue.Settled) {
        LaunchedEffect(dismiss.currentValue) { onDelete() }
    }
    SwipeToDismissBox(
        state = dismiss,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize()
                    .background(Color(0xFFC64049).copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("Eliminar", color = Color(0xFFC64049), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        content = {
            SosActiveCard(
                point = point,
                zone = zone ?: if (zoneDone) "Referencia no disponible (sin internet)" else "Buscando zona…",
                accuracyM = accuracyM,
                onClick = onCardClick,
            )
        },
    )
}

@Composable
private fun SosHeader(onBack: () -> Unit, activeCount: Int = 2) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WaykiBackButton(onClick = onBack)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "S.O.S.",
                        style = AppTextStyles.waykiHeader,
                        color = AppColors.ink
                    )
                    Text(
                        text = "Emergencias cercanas",
                        style = AppTextStyles.waykiLabelMedium,
                        color = AppColors.ink
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(AppColors.cream, CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FeatherIcons.MoreVertical,
                    contentDescription = "Opciones",
                    tint = AppColors.ink,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emergencias activas pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(AppColors.cream, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFC64049), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$activeCount emergencias activas",
                    style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.ink
                )
            }
            
            // Ultima actualizacion
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = FeatherIcons.RefreshCw,
                    contentDescription = null,
                    tint = AppColors.ink,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Actualizado 08:41",
                    style = AppTextStyles.waykiMini,
                    color = AppColors.ink
                )
            }
        }
    }
}

@Composable
private fun SosStat(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = AppTextStyles.waykiMini,
            color = AppColors.slate
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
            color = AppColors.ink
        )
    }
}

// ---------- BLE botón físico (añadido, no toca el diseño Figma de arriba) ----------
// Conecta el ESP32 "WAYKI_SOS" sin WiFi ni tokens. Cada pulsación (GPIO14)
// marca un punto con el GPS real del teléfono en el mapa de esta pantalla.

@Composable
private fun SosBleBar(vm: SosViewModel, activeCount: Int) {
    val ui = vm.ui
    val ble = ui.ble
    var showDevices by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(AppColors.cream.copy(alpha = 0.94f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(10.dp).background(
                    if (ble.connected) Color(0xFF2E7D32) else AppColors.slate,
                    CircleShape,
                ),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    // Contador REAL de emergencias (baja al eliminar tarjetas).
                    text = if (ble.connected) "ESP32 conectado · $activeCount activas" else "Botón SOS sin conectar",
                    style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.ink,
                )
                Text(
                    text = ui.gps?.let { "GPS ${it.lat}, ${it.lon}" } ?: ui.info,
                    style = AppTextStyles.waykiMini,
                    color = AppColors.slate,
                    maxLines = 1,
                )
            }
            Box(
                Modifier.background(AppColors.ink, RoundedCornerShape(12.dp))
                    .clickable { if (ble.connected) vm.disconnect() else vm.connect() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (ble.connected) "Salir" else "Conectar",
                    style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.cream,
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.background(AppColors.mauve, RoundedCornerShape(12.dp))
                    .clickable { showDevices = !showDevices }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "BT",
                        style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.ink,
                    )
                    Icon(
                        FeatherIcons.ChevronDown, null,
                        tint = AppColors.ink,
                        modifier = Modifier.size(14.dp).rotate(if (showDevices) 180f else 0f),
                    )
                }
            }
        }
        // Lista estilo WiFi: elige a cuál Bluetooth conectar (el ESP32 sale con SOS).
        AnimatedVisibility(visible = showDevices) {
            Column {
                Spacer(Modifier.height(8.dp))
                SosBleDevices(vm = vm)
            }
        }
        if (ble.error != null) {
            Spacer(Modifier.height(6.dp))
            Text(ble.error, style = AppTextStyles.waykiMini, color = Color(0xFFC64049))
        }
    }
}

// ---------- Tarjeta del punto SOS activo (zona marcada con tu GPS) ----------
@Composable
private fun SosActiveCard(point: ReportItem, zone: String?, accuracyM: Float?, onClick: () -> Unit = {}) {
    val agoMin = point.id.removePrefix("SOS-").toLongOrNull()
        ?.let { ((currentTimeMillis() - it) / 60_000).coerceAtLeast(0) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = AppColors.shadow.copy(alpha = 0.2f))
            .background(AppColors.cream, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFC64049), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Emergencia activa",
                        style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFC64049)
                    )
                }
                Text(
                    text = "Botón físico",
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.ink
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = FeatherIcons.MapPin,
                    contentDescription = null,
                    tint = Color(0xFFC64049),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Zona de referencia",
                        style = AppTextStyles.waykiMini,
                        color = AppColors.slate
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = zone ?: "Buscando zona…",
                        style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = AppColors.ink,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${point.pos.lat}, ${point.pos.lon}",
                        style = AppTextStyles.waykiMini,
                        color = AppColors.slate
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SosStat(
                    label = "Activada",
                    value = when {
                        agoMin == null -> "ahora"
                        agoMin < 1 -> "ahora mismo"
                        agoMin < 60 -> "hace $agoMin min"
                        else -> "hace ${agoMin / 60} h"
                    }
                )
                SosStat(
                    label = "Precisión GPS",
                    value = accuracyM?.let { "±${it.toInt()} m" } ?: "GPS real"
                )
                SosStat(label = "Radio alerta", value = "250 m")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Punto marcado con tu botón SOS",
                style = AppTextStyles.waykiMini.copy(fontSize = 10.sp),
                color = AppColors.slate,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
