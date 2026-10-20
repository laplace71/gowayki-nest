// Lista de dispositivos BLE cercanos, estilo lista WiFi.
// WAYKI_SOS va primero con insignia SOS; tocar conecta.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.sos.presentation.components

import androidx.compose.foundation.background
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.features.sos.domain.model.BleDevice
import com.gowayki.nesh.app.features.sos.presentation.StateFlow.SosViewModel
import com.gowayki.nesh.core.theme.src.core.AppColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bluetooth
import compose.icons.feathericons.Check
import compose.icons.feathericons.RefreshCw

@Composable
fun SosBleDevices(
    vm: SosViewModel,
    modifier: Modifier = Modifier,
) {
    val devices by vm.devices.collectAsState()
    val ble = vm.ui.ble

    Column(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.cream.copy(alpha = 0.94f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Dispositivos cercanos",
                color = AppColors.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            if (ble.scanning) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = AppColors.onBackground)
                Spacer(Modifier.width(6.dp))
                Text("Buscando…", color = AppColors.onSurfaceMuted, fontSize = 11.sp)
            } else {
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp))
                        .clickable { vm.connect() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(FeatherIcons.RefreshCw, null, tint = AppColors.onBackground, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reescanear", color = AppColors.onBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (devices.isEmpty()) {
            Text(
                if (ble.scanning) "Escuchando Bluetooth…" else "Sin dispositivos: prende el ESP32 o reescanea.",
                color = AppColors.onSurfaceMuted,
                fontSize = 12.sp,
            )
        } else {
            devices.take(8).forEach { d ->
                BleRow(
                    d = d,
                    connected = ble.connected && ble.deviceAddress == d.address,
                    onClick = { vm.connectDevice(d) },
                )
            }
        }
    }
}

@Composable
private fun BleRow(d: BleDevice, connected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).background(
                if (d.isSos) AppColors.onBackground else AppColors.primary.copy(alpha = 0.4f),
                CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                FeatherIcons.Bluetooth, null,
                tint = if (d.isSos) AppColors.cream else AppColors.onBackground,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    d.name,
                    color = AppColors.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
                if (d.isSos) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier.background(AppColors.secondary, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text("SOS", color = AppColors.cream, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(d.address, color = AppColors.onSurfaceMuted, fontSize = 10.sp)
        }
        SignalBars(rssi = d.rssi)
        Spacer(Modifier.width(8.dp))
        if (connected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(FeatherIcons.Check, null, tint = AppColors.onBackground, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(2.dp))
                Text("Listo", color = AppColors.onBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Barras de señal estilo WiFi según RSSI.
@Composable
private fun SignalBars(rssi: Int) {
    val level = when {
        rssi >= -60 -> 4
        rssi >= -70 -> 3
        rssi >= -80 -> 2
        else -> 1
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        for (i in 1..4) {
            Box(
                Modifier.size(width = 3.dp, height = (4 + i * 3).dp)
                    .background(
                        if (i <= level) AppColors.onBackground else AppColors.outline,
                        RoundedCornerShape(1.dp),
                    ),
            )
        }
    }
}
