// Panel SOS — se muestra cuando el tab SOS está activo. No altera el mapa ni el historial.
// Botón físico ESP32 (GPIO14) por BLE -> punto GPS real en el mapa vía onSosPoint.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.sos.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.features.sos.presentation.StateFlow.SosViewModel
import com.gowayki.nesh.core.theme.src.core.AppColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bluetooth
import compose.icons.feathericons.Crosshair
import compose.icons.feathericons.MapPin
import compose.icons.feathericons.Zap

@Composable
fun SosPanel(
    vm: SosViewModel,
    modifier: Modifier = Modifier,
) {
    val ui = vm.ui
    val ble = ui.ble

    Column(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, AppColors.primary.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            .background(AppColors.cream)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(AppColors.onBackground, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(FeatherIcons.Zap, null, tint = AppColors.cream, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "SOS · Botón ESP32",
                    color = AppColors.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                )
                Text(
                    "BLE sin WiFi · GPIO14 → punto GPS",
                    color = AppColors.onSurfaceMuted,
                    fontSize = 12.sp,
                )
            }
            // Dot estado BLE
            Box(
                Modifier.size(12.dp).background(
                    if (ble.connected) AppColors.onBackground else AppColors.outline,
                    CircleShape,
                ),
            )
        }

        // Estado + info
        Text(ui.info, color = AppColors.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                FeatherIcons.Bluetooth, null,
                tint = if (ble.connected) AppColors.onBackground else AppColors.onSurfaceMuted,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                when {
                    ble.connected -> "Conectado: ${ble.deviceName ?: "WAYKI_SOS"} · pulsaciones: ${ble.pressCount}"
                    ble.scanning -> "Buscando WAYKI_SOS…"
                    else -> "Desconectado"
                },
                color = AppColors.onSurfaceMuted,
                fontSize = 12.sp,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(FeatherIcons.MapPin, null, tint = AppColors.onSurfaceMuted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                ui.gps?.let { "GPS: ${it.lat}, ${it.lon}" } ?: "GPS: sin fijar (activa ubicación)",
                color = AppColors.onSurfaceMuted,
                fontSize = 12.sp,
            )
            if (ui.locating) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = AppColors.onBackground)
            }
        }

        // Botones
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SosBtn(
                label = if (ble.connected) "Desconectar" else if (ble.scanning) "Buscando…" else "Conectar ESP32",
                primary = !ble.connected,
                enabled = !ble.scanning,
                modifier = Modifier.weight(1f),
                onClick = { if (ble.connected) vm.disconnect() else vm.connect() },
            )
            SosBtn(
                label = "GPS",
                primary = false,
                modifier = Modifier.weight(0.5f),
                onClick = { vm.refreshGps() },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SosBtn(
                label = "Probar punto GPS",
                primary = false,
                modifier = Modifier.weight(1f),
                onClick = { vm.simulatePress() },
            )
        }

        // Lista estilo WiFi: elige a cuál Bluetooth conectar.
        SosBleDevices(vm = vm)

        AnimatedVisibility(visible = ble.error != null, enter = fadeIn(), exit = fadeOut()) {
            Text(ble.error.orEmpty(), color = AppColors.secondary, fontSize = 12.sp)
        }

        Text(
            "1) Sube el firmware al ESP32 · 2) Conectar · 3) Presiona el botón en GPIO14 → aparece el pin rojo EMERGENCIA con tu ubicación real.",
            color = AppColors.onSurfaceMuted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
    }
}

@Composable
private fun SosBtn(
    label: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (primary) AppColors.onBackground else AppColors.primary.copy(alpha = 0.35f),
                RoundedCornerShape(14.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (primary) AppColors.cream else AppColors.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}
