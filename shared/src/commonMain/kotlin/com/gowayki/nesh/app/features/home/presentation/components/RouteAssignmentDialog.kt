package com.gowayki.nesh.app.features.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.gowayki.nesh.app.features.map.presentation.SelectedRouteHolder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gowayki.nesh.app.common.ui.shared.ToastManager
import com.gowayki.nesh.app.common.ui.shared.WaykiPrimaryButton
import com.gowayki.nesh.app.features.map.domain.model.ArequipaRoutes
import com.gowayki.nesh.app.features.map.domain.model.CombiRoute
import com.gowayki.nesh.core.config.BetaFlags
import com.gowayki.nesh.core.session.TurnoSession
import com.gowayki.nesh.core.session.UserSession
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.data.entities.backend.BackendLineaDto
import com.gowayki.nesh.data.entities.backend.BackendTurnoDto
import com.gowayki.nesh.data.entities.backend.BackendUnidadDto
import com.gowayki.nesh.data.repository.BackendLineasApi
import com.gowayki.nesh.data.repository.BackendTurnosApi
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.Search
import compose.icons.fontawesomeicons.SolidGroup
import compose.icons.fontawesomeicons.solid.Bus
import kotlinx.coroutines.launch

private sealed class FilaRuta {
    data class Empresa(val nombre: String) : FilaRuta()
    data class Linea(val nombre: String, val color: Color) : FilaRuta()
    data class Subrutas(val rutas: List<CombiRoute>) : FilaRuta()
    data class Unidad(val data: BackendUnidadDto, val ocupada: Boolean) : FilaRuta()
    data class Vacia(val linea: String) : FilaRuta()
}

/**
 * Ventana emergente de ruta/unidad.
 *
 * - Si el chofer ya tiene turno: muestra "Tu ruta de hoy" y permite cambiarla.
 * - Si no tiene: en beta puede escoger línea + unidad (placa), agrupadas por
 *   empresa y línea, como el selector de rutas del mapa.
 */
@Composable
fun RouteAssignmentDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAssigned: (BackendTurnoDto) -> Unit = {},
) {
    if (!visible) return

    val scope = rememberCoroutineScope()

    // 0 = turno actual / listado, 1 = confirmar cambio
    var step by remember { mutableStateOf(0) }
    var lineas by remember { mutableStateOf<List<BackendLineaDto>>(emptyList()) }
    var unidades by remember { mutableStateOf<List<BackendUnidadDto>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var unidadSeleccionada by remember { mutableStateOf<BackendUnidadDto?>(null) }
    var loading by remember { mutableStateOf(false) }
    var assigning by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        step = 0
        query = ""
        unidadSeleccionada = null
        error = null
        loading = true
        TurnoSession.refresh()
        if (BetaFlags.selfAssignRoutes) {
            lineas = BackendLineasApi.lineas(force = true)
            unidades = BackendTurnosApi.unidadesTodas()
        }
        loading = false
    }

    val ocupadas = TurnoSession.unidadesOcupadas

    val filas = remember(lineas, unidades, query, ocupadas) {
        val lineMap = lineas.associateBy { it.id }
        val unitsByLine = unidades.groupBy { it.lineaId }
        val filteredLineas = if (query.isBlank()) {
            lineas
        } else {
            lineas.filter { linea ->
                linea.nombre?.contains(query, true) == true ||
                    linea.corredor?.contains(query, true) == true ||
                    (unitsByLine[linea.id] ?: emptyList()).any { u ->
                        u.placa?.contains(query, true) == true ||
                            u.numeroPadron?.contains(query, true) == true
                    }
            }
        }
        val list = mutableListOf<FilaRuta>()
        filteredLineas
            .groupBy { it.corredor ?: it.nombre ?: "Sin empresa" }
            .forEach { (empresa, ls) ->
                list.add(FilaRuta.Empresa(empresa))
                ls.forEach { linea ->
                    val code = linea.nombre?.trim()
                        ?.removePrefix("Línea")?.removePrefix("Linea")?.trim()
                        ?.split(" ")?.firstOrNull()?.uppercase()
                    list.add(FilaRuta.Linea(linea.nombre ?: "Línea ${linea.id}", colorFromHex(linea.colorHex)))
                    val subrutas = if (code != null) {
                        ArequipaRoutes.all.filter { it.isSub && it.parent == code }
                    } else {
                        emptyList()
                    }
                    if (subrutas.isNotEmpty()) {
                        list.add(FilaRuta.Subrutas(subrutas))
                    }
                    val us = unitsByLine[linea.id] ?: emptyList()
                    if (us.isEmpty()) {
                        list.add(FilaRuta.Vacia(linea.nombre ?: "Línea ${linea.id}"))
                    } else {
                        us.forEach { u -> list.add(FilaRuta.Unidad(u, u.id in ocupadas)) }
                    }
                }
            }
        list
    }

    Dialog(
        onDismissRequest = { if (!assigning) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = AppColors.cream,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .heightIn(max = 680.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                // ---------- Sin beta: solo informativo ----------
                if (!BetaFlags.selfAssignRoutes) {
                    DialogTitle("Mi ruta y unidad")
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (TurnoSession.turno != null) {
                            "Tu ruta de hoy es ${TurnoSession.resumen}."
                        } else {
                            "Tu empresa todavía no te asignó una unidad."
                        },
                        style = AppTextStyles.waykiLabelMedium,
                        color = AppColors.ink,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "El cambio de ruta lo gestiona tu empresa desde el panel de administración.",
                        style = AppTextStyles.waykiMini,
                        color = AppColors.slate,
                    )
                    Spacer(Modifier.height(20.dp))
                    WaykiPrimaryButton(text = "Entendido", onClick = onDismiss, showArrow = false)
                    return@Column
                }

                // ---------- Confirmar cambio ----------
                if (step == 1) {
                    DialogTitle("Cambiar de ruta")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Se finalizará tu turno actual (${TurnoSession.resumen}) y podrás elegir otra unidad. ¿Continuar?",
                        style = AppTextStyles.waykiLabelMedium,
                        color = AppColors.ink,
                    )
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(text = error!!, style = AppTextStyles.waykiMini, color = AppColors.error)
                    }
                    Spacer(Modifier.height(20.dp))
                    WaykiPrimaryButton(
                        text = "Sí, finalizar y cambiar",
                        onClick = {
                            val actual = TurnoSession.turno ?: return@WaykiPrimaryButton
                            scope.launch {
                                assigning = true
                                error = null
                                BackendTurnosApi.finalizar(actual.id)
                                    .onSuccess {
                                        TurnoSession.clearTurno()
                                        ToastManager.show("Turno finalizado. Elige tu nueva ruta.")
                                        step = 0
                                        unidadSeleccionada = null
                                        assigning = false
                                    }
                                    .onFailure {
                                        error = it.message ?: "No se pudo finalizar el turno"
                                        assigning = false
                                    }
                            }
                        },
                        isLoading = assigning,
                        showArrow = false,
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { step = 0 }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancelar", color = AppColors.slate, fontWeight = FontWeight.Bold)
                    }
                    return@Column
                }

                // ---------- Turno actual ----------
                if (TurnoSession.turno != null) {
                    val turno = TurnoSession.turno
                    DialogTitle("Tu ruta de hoy")
                    Spacer(Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(colorFromHex(turno?.lineaColorHex), CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = turno?.lineaNombre ?: "Línea",
                            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                            color = AppColors.ink,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(AppColors.softSage)
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = SolidGroup.Bus,
                                contentDescription = null,
                                tint = AppColors.primary,
                                modifier = Modifier.size(26.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Unidad ${turno?.numeroPadron ?: "—"}",
                                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AppColors.ink,
                                )
                                Text(
                                    text = turno?.placa ?: "Sin placa",
                                    style = AppTextStyles.waykiMini,
                                    color = AppColors.slate,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Turno en curso.",
                        style = AppTextStyles.waykiMini,
                        color = AppColors.slate,
                    )

                    Spacer(Modifier.height(20.dp))
                    WaykiPrimaryButton(text = "Continuar", onClick = onDismiss, showArrow = false)
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cambiar de ruta", color = AppColors.primary, fontWeight = FontWeight.Bold)
                    }
                    return@Column
                }

                // ---------- Escoger ruta ----------
                DialogTitle("Escoge tu ruta")
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Elige la línea y la combi que vas a manejar hoy.",
                    style = AppTextStyles.waykiMini,
                    color = AppColors.slate,
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar línea, empresa o placa", color = AppColors.slate) },
                    leadingIcon = { Icon(FeatherIcons.Search, null, tint = AppColors.slate) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.ink,
                        unfocusedBorderColor = AppColors.slate.copy(alpha = 0.4f),
                        focusedContainerColor = AppColors.surface,
                        unfocusedContainerColor = AppColors.surface,
                        cursorColor = AppColors.ink,
                        focusedTextColor = AppColors.ink,
                        unfocusedTextColor = AppColors.ink,
                    ),
                )

                Spacer(Modifier.height(12.dp))

                when {
                    loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = AppColors.primary, strokeWidth = 3.dp)
                        }
                    }

                    filas.isEmpty() -> {
                        Text(
                            text = error ?: "No se pudieron cargar las líneas.",
                            style = AppTextStyles.waykiMini,
                            color = AppColors.error,
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            scope.launch {
                                loading = true
                                error = null
                                lineas = BackendLineasApi.lineas(force = true)
                                unidades = BackendTurnosApi.unidadesTodas()
                                if (lineas.isEmpty()) error = "No se pudieron cargar las líneas."
                                loading = false
                            }
                        }) {
                            Text("Reintentar", color = AppColors.primary, fontWeight = FontWeight.Bold)
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(filas, key = { filaKey(it) }) { fila ->
                                when (fila) {
                                    is FilaRuta.Empresa -> EmpresaHeader(fila.nombre)
                                    is FilaRuta.Linea -> LineaHeader(fila.nombre, fila.color)
                                    is FilaRuta.Subrutas -> SubrutasRow(fila.rutas)
                                    is FilaRuta.Unidad -> UnidadCard(
                                        unidad = fila.data,
                                        ocupada = fila.ocupada,
                                        seleccionada = unidadSeleccionada?.id == fila.data.id,
                                        onClick = { unidadSeleccionada = fila.data },
                                    )
                                    is FilaRuta.Vacia -> Text(
                                        text = "No hay combis disponibles para esta línea.",
                                        style = AppTextStyles.waykiMini,
                                        color = AppColors.slate,
                                        modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                if (error != null && filas.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = error!!, style = AppTextStyles.waykiMini, color = AppColors.error)
                }

                Spacer(Modifier.height(16.dp))
                WaykiPrimaryButton(
                    text = "Comenzar turno",
                    onClick = {
                        val unidad = unidadSeleccionada ?: return@WaykiPrimaryButton
                        val choferId = UserSession.currentUser?.id
                        if (choferId.isNullOrBlank()) {
                            error = "No hay una sesión activa."
                            return@WaykiPrimaryButton
                        }
                        scope.launch {
                            assigning = true
                            error = null
                            BackendTurnosApi.asignar(choferId, unidad.id)
                                .onSuccess { turno ->
                                    TurnoSession.set(turno)
                                    SelectedRouteHolder.selectLineByName(turno.lineaNombre)
                                    ToastManager.show("Turno iniciado: ${TurnoSession.resumen}")
                                    assigning = false
                                    onAssigned(turno)
                                }
                                .onFailure {
                                    error = it.message ?: "No se pudo asignar la ruta"
                                    assigning = false
                                }
                        }
                    },
                    enabled = unidadSeleccionada != null && (unidadSeleccionada?.id !in ocupadas),
                    isLoading = assigning,
                    showArrow = false,
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Ahora no", color = AppColors.slate, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EmpresaHeader(nombre: String) {
    Text(
        text = nombre,
        style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
        color = AppColors.slate,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
    )
}

@Composable
private fun SubrutasRow(rutas: List<CombiRoute>) {
    val selected by SelectedRouteHolder.selected.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rutas.forEach { ruta ->
            val isSelected = selected?.id == ruta.id
            val fondo = if (isSelected) AppColors.primary else AppColors.primary.copy(alpha = 0.10f)
            val texto = if (isSelected) AppColors.cream else AppColors.primary
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(fondo)
                    .clickable {
                        SelectedRouteHolder.select(ruta)
                        ToastManager.show("Ruta seleccionada: ${ruta.name}")
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = ruta.name,
                    style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = texto,
                )
            }
        }
    }
}

@Composable
private fun LineaHeader(nombre: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = nombre,
            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
            color = AppColors.ink,
        )
    }
}

@Composable
private fun UnidadCard(
    unidad: BackendUnidadDto,
    ocupada: Boolean,
    seleccionada: Boolean,
    onClick: () -> Unit,
) {
    val fondo = when {
        seleccionada -> AppColors.primary.copy(alpha = 0.14f)
        ocupada -> AppColors.slate.copy(alpha = 0.10f)
        else -> AppColors.softSage
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(fondo)
            .clickable(enabled = !ocupada, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = SolidGroup.Bus,
            contentDescription = null,
            tint = if (ocupada) AppColors.slate.copy(alpha = 0.5f) else AppColors.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Unidad ${unidad.numeroPadron ?: "—"}",
                style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (ocupada) AppColors.slate.copy(alpha = 0.6f) else AppColors.ink,
            )
            Text(
                text = unidad.placa ?: "Sin placa",
                style = AppTextStyles.waykiMini,
                color = AppColors.slate,
            )
        }
        when {
            ocupada -> Text(
                text = "En ruta",
                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                color = AppColors.error,
            )
            seleccionada -> Icon(
                imageVector = FeatherIcons.Check,
                contentDescription = "Seleccionada",
                tint = AppColors.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun DialogTitle(text: String) {
    Text(
        text = text,
        style = AppTextStyles.waykiHeader,
        color = AppColors.ink,
    )
}

private fun colorFromHex(hex: String?): Color {
    val clean = hex?.trim()?.removePrefix("#") ?: return AppColors.primary
    return runCatching {
        when (clean.length) {
            6 -> Color(0xFF000000 or clean.toLong(16))
            8 -> Color(clean.toLong(16))
            else -> AppColors.primary
        }
    }.getOrDefault(AppColors.primary)
}

private fun filaKey(fila: FilaRuta): String = when (fila) {
    is FilaRuta.Empresa -> "e_${fila.nombre}"
    is FilaRuta.Linea -> "l_${fila.nombre}"
    is FilaRuta.Subrutas -> "s_${fila.rutas.firstOrNull()?.parent}_${fila.rutas.size}"
    is FilaRuta.Unidad -> "u_${fila.data.id}"
    is FilaRuta.Vacia -> "v_${fila.linea}"
}
