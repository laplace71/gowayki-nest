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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
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
import compose.icons.feathericons.Map
import compose.icons.feathericons.MapPin
import compose.icons.feathericons.Moon
import compose.icons.feathericons.Trash2
import compose.icons.feathericons.Volume2
import com.gowayki.nesh.core.AppSettings
import com.gowayki.nesh.core.session.TurnoSession
import com.gowayki.nesh.core.session.UserSession
import com.gowayki.nesh.app.features.home.presentation.components.RouteAssignmentDialog
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
    onDeleteAccount: () -> Unit = {},
    onChangePin: (String, String) -> Unit = { _, _ -> },
    onCambiarSubruta: (String) -> Unit = {},
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
    var showRouteDialog by remember { mutableStateOf(false) }
    var showCambioSubrutaDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var pinActual by remember { mutableStateOf("") }
    val rol = UserSession.currentUser?.rol.orEmpty()
    val esAdmin = rol.equals("ADMIN", ignoreCase = true)
    val esJefe = rol.equals("JEFE_RUTA", ignoreCase = true)

    // El gesto atrás cierra primero lo que esté abierto encima (menú o diálogo).
    // Así no compite con la salida del NavHost y no aparece el zoom del sistema.
    val haySuperpuesto = showEsp32Menu || showRouteDialog || showCambioSubrutaDialog ||
        showDeleteDialog || showPinDialog || showShiftStartDialog ||
        showLunchDurationDialog || showLunchDialog || showLanguageDialog
    BackHandler(enabled = haySuperpuesto) {
        when {
            showEsp32Menu -> showEsp32Menu = false
            showRouteDialog -> showRouteDialog = false
            showCambioSubrutaDialog -> showCambioSubrutaDialog = false
            showDeleteDialog -> showDeleteDialog = false
            showPinDialog -> {
                showPinDialog = false
                newPin = ""
                pinActual = ""
            }
            showShiftStartDialog -> showShiftStartDialog = false
            showLunchDurationDialog -> showLunchDurationDialog = false
            showLunchDialog -> showLunchDialog = false
            showLanguageDialog -> showLanguageDialog = false
        }
    }

    LaunchedEffect(Unit) { TurnoSession.refresh() }

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
                        text = UserSession.displayInitials,
                        size = 48.dp,
                        dark = false,
                        photoPath = profilePhotoPath
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = UserSession.displayName,
                            style = AppTextStyles.waykiLabelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = AppColors.cream
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (TurnoSession.hasTurno) {
                                "${TurnoSession.lineaNombre} · ${TurnoSession.unidadTexto}"
                            } else {
                                "Sin unidad asignada"
                            },
                            style = AppTextStyles.waykiMini,
                            color = AppColors.cream.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

            // 2b. RUTA Y UNIDAD
            Text(
                text = "RUTA Y UNIDAD",
                style = AppTextStyles.waykiMini.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = AppColors.slate
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingActionItem(
                icon = FeatherIcons.Map,
                title = "Mi ruta / unidad",
                subtitle = if (com.gowayki.nesh.core.config.BetaFlags.selfAssignRoutes) {
                    "Escoge o cambia tu combi (beta)"
                } else {
                    "La unidad la asigna tu empresa"
                },
                value = TurnoSession.resumen,
                onClick = { showRouteDialog = true }
            )

            if (!esAdmin) {
                Spacer(modifier = Modifier.height(10.dp))
                SettingActionItem(
                    icon = FeatherIcons.MapPin,
                    title = "Enviar solicitud de cambio",
                    subtitle = if (esJefe) "Elige línea y subruta; lo aprueba el administrador"
                    else "Elige línea y subruta; el jefe debe aprobarla",
                    value = null,
                    onClick = { showCambioSubrutaDialog = true }
                )
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

                SettingActionItem(
                    icon = FeatherIcons.Trash2,
                    title = "Eliminar cuenta",
                    subtitle = "Sales de los chats; tus mensajes siguen para los demás",
                    value = null,
                    onClick = { showDeleteDialog = true }
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

                // Solo el administrador puede elegir ruta desde el mapa.
                // Choferes y jefes cambian de subruta enviando una solicitud.
                if (esAdmin) {
                    SettingActionItem(
                        icon = FeatherIcons.MapPin,
                        title = "Ruta actual de micro",
                        subtitle = if (com.gowayki.nesh.core.session.TurnoSession.hasTurno) {
                            "Tu ruta la define 'Mi ruta / unidad'"
                        } else {
                            "Escoge tu ruta actual de micro"
                        },
                        value = SelectedRouteHolder.selected.collectAsState().value?.name,
                        onClick = {
                            if (!com.gowayki.nesh.core.session.TurnoSession.hasTurno) {
                                onRoutePickerClick()
                            } else {
                                ToastManager.show("Cambia tu ruta en 'Mi ruta / unidad'")
                            }
                        }
                    )
                }

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
                                            "${sosVm.ui.ble.deviceName ?: "WAYKI"} · ${sosVm.ui.ble.deviceAddress ?: "Conectado"}"
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

                            Text(
                                text = "El teléfono necesita la ubicación activada: el Bluetooth no encuentra nada sin ella.",
                                style = AppTextStyles.waykiMini,
                                color = AppColors.slate,
                                maxLines = 2,
                            )
                            SosBleDevices(vm = sosVm)

                            if (sosVm.ui.ble.error != null) {
                                Text(
                                    text = sosVm.ui.ble.error.orEmpty(),
                                    style = AppTextStyles.waykiMini,
                                    color = AppColors.secondary,
                                    maxLines = 3,
                                )
                            }

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
                                            ToastManager.show("Buscando ESP32…")
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
                onDismissRequest = { showPinDialog = false; newPin = ""; pinActual = "" },
                title = { Text("Cambiar PIN") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = pinActual,
                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinActual = it },
                            label = { Text("PIN actual") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                            label = { Text("Nuevo PIN (6 dígitos)") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (pinActual.length == 6 && newPin.length == 6) {
                            onChangePin(pinActual, newPin)
                            showPinDialog = false
                            pinActual = ""
                            newPin = ""
                        } else {
                            ToastManager.show("Ingresa ambos PIN de 6 dígitos")
                        }
                    }) { Text("Guardar") }
                },
                dismissButton = {
                    TextButton(onClick = { showPinDialog = false; newPin = ""; pinActual = "" }) { Text("Cancelar") }
                }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Eliminar cuenta") },
                text = {
                    Text(
                        "Sales de todos los grupos y pierdes acceso a tus chats. " +
                            "Tus mensajes siguen en el historial de los demás. " +
                            "Podrás volver a registrarte con tu DNI para restaurar tu cuenta."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        onDeleteAccount()
                    }) { Text("Eliminar cuenta", color = AppColors.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
                }
            )
        }

        CambioSubrutaDialog(
            visible = showCambioSubrutaDialog,
            rol = rol,
            onDismiss = { showCambioSubrutaDialog = false },
            onEnviar = { id ->
                onCambiarSubruta(id)
                showCambioSubrutaDialog = false
            },
        )

        RouteAssignmentDialog(
            visible = showRouteDialog,
            onDismiss = { showRouteDialog = false },
            onSolicitarCambioSubruta = {
                onCambiarSubruta(it)
                showRouteDialog = false
            },
            onAssigned = { turno ->
                showRouteDialog = false
                // La subruta recién asignada manda también en el mapa/recorrido.
                turno.subrutaCodigo?.takeIf { it.isNotBlank() }?.let {
                    SelectedRouteHolder.selectById(it)
                }
            },
        )
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

/**
 * Solicitud de cambio de subruta desde Ajustes: línea y subruta en menús
 * desplegables. El backend aprueba el cambio; la app no lo aplica directo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CambioSubrutaDialog(
    visible: Boolean,
    rol: String,
    onDismiss: () -> Unit,
    onEnviar: (String) -> Unit,
) {
    if (!visible) return
    val paraAdministrador = rol.equals("JEFE_RUTA", ignoreCase = true)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val catalogo by SelectedRouteHolder.catalog.collectAsState()
    var cargando by remember { mutableStateOf(false) }
    var lineaExpandida by remember { mutableStateOf(false) }
    var subrutaExpandida by remember { mutableStateOf(false) }
    var lineaCodigo by remember { mutableStateOf<String?>(null) }
    var subrutaElegida by remember { mutableStateOf<com.gowayki.nesh.app.features.map.domain.model.CombiRoute?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        cargando = true
        runCatching { SelectedRouteHolder.refreshCatalog(force = true) }
        cargando = false
    }

    val lineas = remember(catalogo) {
        catalogo
            .groupBy { it.parent ?: "?" }
            .map { (codigo, rutas) ->
                codigo to (rutas.firstOrNull { !it.parentName.isNullOrBlank() }?.parentName ?: codigo)
            }
            .sortedBy { it.first }
    }
    val subrutas = remember(catalogo, lineaCodigo) {
        catalogo.filter { it.parent == lineaCodigo }
    }
    val nombreLinea = lineas.firstOrNull { it.first == lineaCodigo }?.second ?: ""

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AppColors.cream,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .heightIn(max = 520.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Text(
                    text = "Solicitar cambio de ruta",
                    style = AppTextStyles.waykiHeader,
                    color = AppColors.ink,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (paraAdministrador) {
                        "Elige la línea y la subruta a la que quieres pertenecer. El cambio lo aprueba el administrador."
                    } else {
                        "Elige la línea y la subruta a la que quieres pertenecer. El cambio se aplica cuando el jefe lo aprueba."
                    },
                    style = AppTextStyles.waykiMini,
                    color = AppColors.slate,
                )
                Spacer(Modifier.height(18.dp))

                ExposedDropdownMenuBox(
                    expanded = lineaExpandida,
                    onExpandedChange = { lineaExpandida = it },
                ) {
                    OutlinedTextField(
                        value = nombreLinea,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Línea") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lineaExpandida) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(14.dp),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = lineaExpandida,
                        onDismissRequest = { lineaExpandida = false },
                    ) {
                        lineas.forEach { (codigo, nombre) ->
                            DropdownMenuItem(
                                text = { Text("$codigo · $nombre") },
                                onClick = {
                                    lineaCodigo = codigo
                                    subrutaElegida = null
                                    lineaExpandida = false
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = subrutaExpandida,
                    onExpandedChange = { subrutaExpandida = it },
                ) {
                    OutlinedTextField(
                        value = subrutaElegida?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = lineaCodigo != null,
                        label = { Text("Subruta") },
                        placeholder = { Text("Primero elige la línea") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subrutaExpandida) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(14.dp),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = subrutaExpandida,
                        onDismissRequest = { subrutaExpandida = false },
                    ) {
                        subrutas.forEach { ruta ->
                            DropdownMenuItem(
                                text = { Text(ruta.name) },
                                onClick = {
                                    subrutaElegida = ruta
                                    subrutaExpandida = false
                                },
                            )
                        }
                    }
                }

                if (cargando) {
                    Spacer(Modifier.height(10.dp))
                    Text("Cargando catálogo…", style = AppTextStyles.waykiMini, color = AppColors.slate)
                }
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, style = AppTextStyles.waykiMini, color = AppColors.error)
                }
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = {
                        scope.launch { SelectedRouteHolder.refreshCatalog(force = true) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Actualizar catálogo", color = AppColors.primary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                com.gowayki.nesh.app.common.ui.shared.WaykiPrimaryButton(
                    text = "ENVIAR SOLICITUD",
                    enabled = subrutaElegida?.subrutaDbId != null,
                    showArrow = false,
                    onClick = {
                        val id = subrutaElegida?.subrutaDbId
                        if (id != null) {
                            onEnviar(id.toString())
                        } else {
                            error = "Selecciona una subruta válida."
                        }
                    },
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancelar", color = AppColors.slate, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
