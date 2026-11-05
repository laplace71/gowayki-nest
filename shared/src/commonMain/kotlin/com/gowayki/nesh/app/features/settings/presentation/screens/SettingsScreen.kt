package com.gowayki.nesh.app.features.settings.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import com.gowayki.nesh.app.common.ui.shared.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.ui.shared.WaykiAvatar
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bell
import compose.icons.feathericons.Bluetooth
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.ChevronRight
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Globe
import compose.icons.feathericons.HelpCircle
import compose.icons.feathericons.Lock
import compose.icons.feathericons.MapPin
import compose.icons.feathericons.Moon
import compose.icons.feathericons.Volume2
import com.gowayki.nesh.core.AppSettings
import com.gowayki.nesh.core.theme.ThemeManager
import com.gowayki.nesh.app.features.map.presentation.SelectedRouteHolder
import com.gowayki.nesh.app.features.map.domain.model.ArequipaRoutes
import com.gowayki.nesh.app.common.ui.shared.ToastManager
import com.gowayki.nesh.app.features.sos.presentation.StateFlow.SosViewModel
import com.gowayki.nesh.app.features.sos.presentation.components.SosBleDevices

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onMapaClick: () -> Unit = {},
    onRoutePickerClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    // Instancia SOS compartida del NavHost: sobrevive al cambio entre módulos.
    sosVm: SosViewModel = viewModel()
) {
    val uriHandler = LocalUriHandler.current
    val profilePhotoPath by com.gowayki.nesh.data.repository.ProfilePhotoState.path
    var showEsp32Menu by remember { mutableStateOf(false) }
    var timeAlertsEnabled by remember { mutableStateOf(AppSettings.timeAlertsEnabled.value) }
    var vibrationEnabled by remember { mutableStateOf(AppSettings.vibrationEnabled.value) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLunchDialog by remember { mutableStateOf(false) }
    var showLunchDurationDialog by remember { mutableStateOf(false) }
    var showShiftStartDialog by remember { mutableStateOf(false) }
    var shiftStartInput by remember { mutableStateOf(AppSettings.shiftStartTime.value) }
    var lunchTimeInput by remember { mutableStateOf(AppSettings.lunchTime.value) }
    var lunchDurationInput by remember { mutableStateOf(AppSettings.lunchDurationMinutes.value.toString()) }
    var darkModeEnabled by remember { mutableStateOf(ThemeManager.isDark.value) }
    var showPinDialog by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.cream)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = AppSpacing.pageHorizontal, vertical = AppSpacing.sm)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Encabezado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WaykiBackButton(onClick = onBack)
                    Text(
                        text = "Configuración",
                        style = AppTextStyles.waykiHeader,
                        color = AppColors.ink
                    )
                }

                Icon(
                    imageVector = FeatherIcons.Bell,
                    contentDescription = "Notificaciones",
                    tint = AppColors.ink,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onNotificationsClick() }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "A tu manera, sin perder el ritmo.",
                style = AppTextStyles.waykiMini.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = AppColors.slate
            )

            Spacer(modifier = Modifier.height(24.dp))

                    // 2. Perfil del conductor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppColors.mauve)
                    .clickable(onClick = onProfileClick)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WaykiAvatar(
                        text = "CM",
                        size = 48.dp,
                        dark = false,
                        photoPath = profilePhotoPath
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Carlos Mendoza",
                            style = AppTextStyles.waykiLabelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = AppColors.cream
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Línea 7 · Unidad 08",
                            style = AppTextStyles.waykiMini,
                            color = AppColors.cream.copy(alpha = 0.8f)
                        )
                    }

                    Icon(
                        imageVector = FeatherIcons.CheckCircle,
                        contentDescription = "Verificado",
                        tint = AppColors.cream,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 3. AVISOS Y COMUNICACIÓN
            Text(
                text = "AVISOS Y COMUNICACIÓN",
                style = AppTextStyles.waykiMini.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = AppColors.slate
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingSwitchItem(
                    icon = FeatherIcons.Clock,
                    title = "Alertas de tiempo",
                    subtitle = "Al adelantar o atrasar tu recorrido",
                    checked = timeAlertsEnabled,
                    onCheckedChange = {
                        timeAlertsEnabled = it
                        AppSettings.timeAlertsEnabled.value = it
                        ToastManager.show(if (it) "Alertas de tiempo activadas" else "Alertas de tiempo desactivadas")
                    }
                )

                SettingSwitchItem(
                    icon = FeatherIcons.Volume2,
                    title = "Vibración",
                    subtitle = "Avisos discretos de puntos de control",
                    checked = vibrationEnabled,
                    onCheckedChange = {
                        vibrationEnabled = it
                        AppSettings.vibrationEnabled.value = it
                        ToastManager.show(if (it) "Vibración activada" else "Vibración desactivada")
                    }
                )


            }

            Spacer(modifier = Modifier.height(28.dp))

            // 3b. APARIENCIA
            Text(
                text = "APARIENCIA",
                style = AppTextStyles.waykiMini.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = AppColors.slate
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingSwitchItem(
                    icon = FeatherIcons.Moon,
                    title = "Modo oscuro",
                    subtitle = "Tema oscuro de la aplicación",
                    checked = darkModeEnabled,
                    onCheckedChange = {
                        darkModeEnabled = it
                        ThemeManager.setDark(it)
                        ToastManager.show(if (it) "Modo oscuro activado" else "Modo claro activado")
                    }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 4. CUENTA Y APLICACIÓN
            Text(
                text = "CUENTA Y APLICACIÓN",
                style = AppTextStyles.waykiMini.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = AppColors.slate
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingActionItem(
                    icon = FeatherIcons.Lock,
                    title = "Cambiar PIN",
                    subtitle = "Protege tu código de 6 dígitos",
                    value = null,
                    onClick = { showPinDialog = true }
                )

                SettingActionItem(
                    icon = FeatherIcons.MapPin,
                    title = "Ubicación",
                    subtitle = "Activa durante tu recorrido",
                    value = if (AppSettings.locationEnabled.value) "GPS activo" else "GPS inactivo",
                    onClick = {
                        AppSettings.locationEnabled.value = !AppSettings.locationEnabled.value
                        ToastManager.show(if (AppSettings.locationEnabled.value) "Ubicación activada" else "Ubicación desactivada")
                    }
                )

                SettingActionItem(
                    icon = FeatherIcons.Globe,
                    title = "Idioma",
                    subtitle = "Idioma de la aplicación",
                    value = AppSettings.language.value,
                    onClick = { showLanguageDialog = true }
                )

                SettingActionItem(
                    icon = FeatherIcons.Clock,
                    title = "Hora de almuerzo",
                    subtitle = "Notificación cuando sea hora",
                    value = if (AppSettings.lunchEnabled.value) AppSettings.lunchTime.value else "Desactivado",
                    onClick = { if (AppSettings.lunchEnabled.value) showLunchDialog = true }
                )

                SettingActionItem(
                    icon = FeatherIcons.Clock,
                    title = "Duración de almuerzo",
                    subtitle = "Tiempo del descanso",
                    value = if (AppSettings.lunchEnabled.value) "${AppSettings.lunchDurationMinutes.value} min" else "Desactivado",
                    onClick = { if (AppSettings.lunchEnabled.value) showLunchDurationDialog = true }
                )

                SettingActionItem(
                    icon = FeatherIcons.Clock,
                    title = "Hora de inicio del turno",
                    subtitle = "Hora de entrada de la combi (para el cumplimiento)",
                    value = AppSettings.shiftStartTime.value,
                    onClick = { showShiftStartDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 4b. NOTIFICACIONES
            Text(
                text = "NOTIFICACIONES",
                style = AppTextStyles.waykiMini.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = AppColors.slate
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingSwitchItem(
                    icon = FeatherIcons.Bell,
                    title = "Alertas de puntos de control",
                    subtitle = "Aviso 5 min antes de llegar",
                    checked = AppSettings.controlPointAlertsEnabled.value,
                    onCheckedChange = {
                        AppSettings.controlPointAlertsEnabled.value = it
                        ToastManager.show(if (it) "Alertas de puntos de control activadas" else "Alertas de puntos de control desactivadas")
                    }
                )

                SettingSwitchItem(
                    icon = FeatherIcons.Bell,
                    title = "Rutas cercanas",
                    subtitle = "Avisos de combis atras/adelante",
                    checked = AppSettings.notificationsEnabled.value,
                    onCheckedChange = {
                        AppSettings.notificationsEnabled.value = it
                        ToastManager.show(if (it) "Notificaciones de rutas cercanas activadas" else "Notificaciones de rutas cercanas desactivadas")
                    }
                )

                SettingSwitchItem(
                    icon = FeatherIcons.Bell,
                    title = "Detectar otros conductores",
                    subtitle = "Aviso cuando otro se une a la ruta",
                    checked = AppSettings.detectOtherDrivers.value,
                    onCheckedChange = {
                        AppSettings.detectOtherDrivers.value = it
                        ToastManager.show(if (it) "Detección de otros conductores activada" else "Detección desactivada")
                    }
                )

                SettingActionItem(
                    icon = FeatherIcons.MapPin,
                    title = "Ruta actual de micro",
                    subtitle = "Escoge tu ruta actual de micro",
                    value = SelectedRouteHolder.selected.collectAsState().value?.name,
                    onClick = onRoutePickerClick
                )

                SettingSwitchItem(
                    icon = FeatherIcons.Clock,
                    title = "Alertas de almuerzo",
                    subtitle = "Desactiva las 2 alertas (inicio y fin)",
                    checked = AppSettings.lunchEnabled.value,
                    onCheckedChange = {
                        AppSettings.lunchEnabled.value = it
                        ToastManager.show(if (it) "Notificación de almuerzo activada" else "Notificación de almuerzo desactivada")
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(AppColors.softSage)
                        .clickable { ToastManager.show("Estás a 5 minutos de tu próximo punto de control") }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Probar notificación",
                        style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        color = AppColors.ink
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 5. Botón SOS (ESP32 BLE)
            Text(
                text = "EMERGENCIA",
                style = AppTextStyles.waykiMini.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = AppColors.slate
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingActionItem(
                    icon = FeatherIcons.Bluetooth,
                    title = "Conexión SOS (ESP32)",
                    subtitle = "Despliega las conexiones Bluetooth",
                    value = if (sosVm.ui.ble.connected) "Conectado" else "No conectado",
                    onClick = { showEsp32Menu = !showEsp32Menu }
                )

                if (showEsp32Menu) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(AppColors.softSage)
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Conexiones Bluetooth",
                                        style = AppTextStyles.waykiLabelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = AppColors.ink
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = if (sosVm.ui.ble.connected)
                                            "WAYKI_SOS · ${sosVm.ui.ble.deviceAddress ?: "Conectado"}"
                                        else
                                            "Pulsa buscar para encontrar tu ESP32",
                                        style = AppTextStyles.waykiMini,
                                        color = AppColors.slate,
                                        maxLines = 1,
                                    )
                                }
                                Icon(
                                    imageVector = FeatherIcons.ChevronRight,
                                    contentDescription = if (showEsp32Menu) "Cerrar menú" else "Abrir menú",
                                    tint = AppColors.slate,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(if (showEsp32Menu) 90f else 0f)
                                )
                            }

                            SosBleDevices(vm = sosVm)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (sosVm.ui.ble.connected) AppColors.mauve else AppColors.primary)
                                    .clickable {
                                        if (sosVm.ui.ble.connected) {
                                            sosVm.disconnect()
                                            ToastManager.show("Desconectado del botón SOS")
                                        } else {
                                            sosVm.connect()
                                            ToastManager.show("Buscando WAYKI_SOS…")
                                        }
                                    }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (sosVm.ui.ble.connected) "Desconectar ESP32" else "Buscar mi ESP32",
                                    style = AppTextStyles.waykiLabelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = AppColors.cream
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 6. Botón Soporte Técnico
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(AppColors.mauve)
                    .clickable { onSupportClick() }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = FeatherIcons.HelpCircle,
                        contentDescription = null,
                        tint = AppColors.cream,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Soporte técnico",
                        style = AppTextStyles.waykiLabelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = AppColors.cream
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Versión al pie
            Text(
                text = "Wayki Nest para Android · Versión 1.0.0",
                style = AppTextStyles.waykiMini.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = AppColors.slate,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Atribución de datos geográficos",
                style = AppTextStyles.waykiMini.copy(fontSize = 10.sp),
                color = AppColors.slate,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { uriHandler.openUri("https://www.openstreetmap.org/copyright") }
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showLunchDurationDialog) {
            AlertDialog(
                onDismissRequest = { showLunchDurationDialog = false },
                title = { Text("Duración de almuerzo") },
                text = {
                    Column {
                        Text("Ingresa los minutos o selecciona una sugerencia", color = AppColors.slate, style = AppTextStyles.waykiMini)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = lunchDurationInput,
                            onValueChange = { lunchDurationInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Minutos") },
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 15, 30, 45, 60).forEach { d ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AppColors.softSage)
                                        .clickable { lunchDurationInput = d.toString() }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("$d", color = AppColors.ink, style = AppTextStyles.waykiMini)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val v = lunchDurationInput.toIntOrNull()
                        if (v != null && v in 1..240) {
                            AppSettings.lunchDurationMinutes.value = v
                            showLunchDurationDialog = false
                            ToastManager.show("Duración de almuerzo: $v min")
                        }
                    }) { Text("Guardar") }
                },
                dismissButton = {
                    TextButton(onClick = { showLunchDurationDialog = false }) { Text("Cancelar") }
                }
            )
        }

        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = { Text("Idioma") },
                text = {
                    Column {
                        listOf("Español", "English").forEach { lang ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        AppSettings.language.value = lang
                                        showLanguageDialog = false
                                        ToastManager.show("Idioma cambiado a $lang")
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(lang, color = AppColors.ink)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguageDialog = false }) { Text("Cerrar") }
                }
            )
        }

        if (showLunchDialog) {
            AlertDialog(
                onDismissRequest = { showLunchDialog = false },
                title = { Text("Hora de almuerzo") },
                text = {
                    Column {
                        Text("Ingresa la hora (HH:mm) o selecciona una sugerencia", color = AppColors.slate, style = AppTextStyles.waykiMini)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = lunchTimeInput,
                            onValueChange = { lunchTimeInput = it },
                            label = { Text("Hora (HH:mm)") },
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("12:00", "12:30", "13:00", "13:30", "14:00").forEach { t ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AppColors.softSage)
                                        .clickable { lunchTimeInput = t }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(t, color = AppColors.ink, style = AppTextStyles.waykiMini)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val v = lunchTimeInput.trim()
                        if (Regex("^\\d{2}:\\d{2}$").matches(v)) {
                            AppSettings.lunchTime.value = v
                            showLunchDialog = false
                            ToastManager.show("Hora de almuerzo: $v")
                        }
                    }) { Text("Guardar") }
                },
                dismissButton = {
                    TextButton(onClick = { showLunchDialog = false }) { Text("Cancelar") }
                }
            )
        }

        if (showShiftStartDialog) {
            AlertDialog(
                onDismissRequest = { showShiftStartDialog = false },
                title = { Text("Hora de inicio del turno") },
                text = {
                    Column {
                        Text("Hora de entrada de la combi (para el % de cumplimiento)", color = AppColors.slate, style = AppTextStyles.waykiMini)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = shiftStartInput,
                            onValueChange = { shiftStartInput = it },
                            label = { Text("Hora (HH:mm)") },
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("04:30", "05:00", "05:30", "06:00").forEach { t ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AppColors.softSage)
                                        .clickable { shiftStartInput = t }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(t, color = AppColors.ink, style = AppTextStyles.waykiMini)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val v = shiftStartInput.trim()
                        if (Regex("^\\d{2}:\\d{2}$").matches(v)) {
                            AppSettings.shiftStartTime.value = v
                            showShiftStartDialog = false
                            ToastManager.show("Hora de inicio: $v")
                        }
                    }) { Text("Guardar") }
                },
                dismissButton = {
                    TextButton(onClick = { showShiftStartDialog = false }) { Text("Cancelar") }
                }
            )
        }

        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false; newPin = "" },
                title = { Text("Cambiar PIN") },
                text = {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                        label = { Text("Nuevo PIN (6 dígitos)") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newPin.length == 6) {
                            showPinDialog = false
                            ToastManager.show("PIN actualizado correctamente")
                            newPin = ""
                        }
                    }) { Text("Guardar") }
                },
                dismissButton = {
                    TextButton(onClick = { showPinDialog = false; newPin = "" }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
private fun SettingSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.softSage)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AppColors.ink.copy(alpha = 0.08f)),
                onClick = { onCheckedChange(!checked) }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.ink,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTextStyles.waykiLabelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = AppColors.ink
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = AppTextStyles.waykiMini.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = AppColors.slate
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                interactionSource = interactionSource,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppColors.cream,
                    checkedTrackColor = AppColors.ink,
                    uncheckedThumbColor = AppColors.cream,
                    uncheckedTrackColor = AppColors.steel.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun SettingActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String?,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.softSage)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AppColors.ink.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.ink,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTextStyles.waykiLabelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = AppColors.ink
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = AppTextStyles.waykiMini.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = AppColors.slate
                )
            }

            if (value != null) {
                Text(
                    text = value,
                    style = AppTextStyles.waykiMini.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = AppColors.slate,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 120.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Icon(
                imageVector = FeatherIcons.ChevronRight,
                contentDescription = null,
                tint = AppColors.slate,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
