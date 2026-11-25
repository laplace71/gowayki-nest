package com.gowayki.nesh.app.features.home.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gowayki.nesh.app.common.ui.shared.ToastManager
import com.gowayki.nesh.app.common.ui.shared.WaykiPrimaryButton
import com.gowayki.nesh.app.common.ui.shared.WaykiSecondaryButton
import com.gowayki.nesh.app.common.ui.shared.WaykiSkeleton
import com.gowayki.nesh.app.common.ui.shared.WaykiTextButton
import com.gowayki.nesh.app.features.map.domain.model.CombiRoute
import com.gowayki.nesh.core.config.BetaFlags
import com.gowayki.nesh.core.session.TurnoSession
import com.gowayki.nesh.core.session.UserSession
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
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
    data class Linea(val id: Int, val nombre: String, val color: Color) : FilaRuta()
    data class Subruta(
        val ruta: CombiRoute,
        val cantidadCombis: Int,
        val seleccionada: Boolean,
        val actual: Boolean,
    ) : FilaRuta()
    data class SinSubruta(val cantidad: Int) : FilaRuta()
    data class Unidad(
        val data: BackendUnidadDto,
        val ocupada: Boolean,
        val indentado: Boolean = false,
    ) : FilaRuta()
    data class Vacia(val id: Int, val linea: String) : FilaRuta()
}

/**
 * Código de línea (C2, C3…) a partir del nombre real ("Cono Norte").
 * Usa el mapeo del catálogo; si aún no cargó, lo resuelve por `parentName`.
 */
private fun mismoIdSubruta(a: String?, b: String?): Boolean {
    if (a == null || b == null) return false
    fun norm(value: String) = value.trim()
        .removePrefix("SB-").removePrefix("sb-").trim()
    return norm(a).equals(norm(b), ignoreCase = true)
}

/** Normaliza un código de subruta a `SB-…` para cruzarlo con la BD. */
private fun codigoSubruta(valor: String?): String? = valor?.trim()
    ?.removePrefix("SB-")?.removePrefix("sb-")?.trim()
    ?.takeIf { it.isNotBlank() }
    ?.let { "SB-$it" }

/**
 * Una combi pertenece a la subruta por su código SmartBus o por su id numérico
 * de gowayki_db. El backend ya persiste ese id al crear/editar desde el panel.
 */
private fun unidadEsDeSubruta(
    unidad: BackendUnidadDto,
    codigo: String?,
    subrutaDbId: Int?,
): Boolean {
    if (codigo != null && mismoIdSubruta(unidad.subrutaCodigo, codigo)) return true
    return subrutaDbId != null && unidad.subrutaId == subrutaDbId
}

private fun codigoDeLinea(nombre: String?): String? {
    SelectedRouteHolder.codigoParaNombre(nombre)?.let { return it }
    val limpio = nombre?.trim()?.takeIf { it.isNotBlank() } ?: return null
    SelectedRouteHolder.catalog.value.firstOrNull {
        it.parentName?.equals(limpio, ignoreCase = true) == true && !it.parent.isNullOrBlank()
    }?.parent?.let { return it }
    return limpio
        .removePrefix("Línea").removePrefix("Linea").trim()
        .split(" ").firstOrNull()?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
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
    onSolicitarCambioSubruta: (String) -> Unit = {},
    obligatorio: Boolean = false,
) {
    if (!visible) return

    val scope = rememberCoroutineScope()

    // 0 = turno actual / listado, 1 = confirmar cambio
    var step by remember { mutableStateOf(0) }
    var lineas by remember { mutableStateOf<List<BackendLineaDto>>(emptyList()) }
    var unidades by remember { mutableStateOf<List<BackendUnidadDto>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var unidadSeleccionada by remember { mutableStateOf<BackendUnidadDto?>(null) }
    // El diálogo se crea al hacerse visible: arranca en carga para que no se
    // vea un frame con "no hay combis" antes de que lleguen los datos.
    var loading by remember { mutableStateOf(true) }
    var assigning by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Elección explícita del chofer dentro de este diálogo. La selección global
    // de SelectedRouteHolder puede venir de una pantalla anterior y no significa
    // que el chofer haya elegido esa subruta para la unidad actual.
    var subrutaElegida by remember { mutableStateOf<String?>(null) }
    val selectedGlobal by SelectedRouteHolder.selected.collectAsState()

    // `subrutaElegida` guarda SOLO lo que el chofer tocó. La ruta actual se
    // muestra con la etiqueta «Actual» (actualGlobal) y, si no hay override
    // explícito, se toma de la unidad elegida al confirmar.

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        step = 0
        query = ""
        unidadSeleccionada = null
        subrutaElegida = null
        error = null
        loading = true
        TurnoSession.refresh()
        // Cargar catálogo de subrutas desde BD (reemplaza hardcode local)
        runCatching { SelectedRouteHolder.refreshCatalog(force = true) }
        if (BetaFlags.selfAssignRoutes) {
            lineas = BackendLineasApi.lineas(force = true)
            unidades = BackendTurnosApi.unidadesTodas()
        }
        loading = false
    }

    val ocupadas = TurnoSession.unidadesOcupadas
    val catalogSubrutas by SelectedRouteHolder.catalog.collectAsState()

    // Una sola referencia de "dónde estoy" para TODO el diálogo: el turno si
    // existe, si no la unidad elegida, y si no la ruta que ya trae la app.
    val actualGlobal = TurnoSession.turno?.subrutaCodigo?.trim()
        ?: unidadSeleccionada?.subrutaCodigo?.trim()
        ?: selectedGlobal?.id?.trim()

    val rol = UserSession.currentUser?.rol.orEmpty()
    val esAdmin = rol.equals("ADMIN", ignoreCase = true)
    val esChofer = rol.equals("CHOFER", ignoreCase = true)
    val esJefe = rol.equals("JEFE_RUTA", ignoreCase = true)

    // Subruta ya aprobada para este usuario. Puede venir como código SmartBus
    // o como id numérico de gowayki_db (lo que guarda el panel).
    val subrutaFijaCruda = TurnoSession.turno?.subrutaCodigo?.takeIf { it.isNotBlank() }
        ?: UserSession.currentUser?.subrutaId
    val subrutaFijaNumerica = subrutaFijaCruda?.trim()?.toIntOrNull()
    val subrutaFijaCodigo = when {
        esAdmin || subrutaFijaNumerica != null -> null
        esChofer || esJefe -> codigoSubruta(subrutaFijaCruda)
        else -> null
    }
    val cambioSolicitado = !esAdmin && subrutaElegida != null &&
        !mismoIdSubruta(subrutaElegida, subrutaFijaCodigo)

    // Datos de la subruta asignada: id numérico y código. El id numérico es el
    // que guarda el panel al crear/editar una combi.
    val subrutaFija = remember(catalogSubrutas, subrutaFijaCodigo, subrutaFijaNumerica) {
        catalogSubrutas.firstOrNull { ruta ->
            (subrutaFijaCodigo != null &&
                (mismoIdSubruta(ruta.id, subrutaFijaCodigo) || mismoIdSubruta(ruta.routeId, subrutaFijaCodigo))) ||
                (subrutaFijaNumerica != null && ruta.subrutaDbId == subrutaFijaNumerica)
        }
    }
    val subrutaFijaDbId = remember(subrutaFija, subrutaFijaNumerica) {
        subrutaFija?.subrutaDbId ?: subrutaFijaNumerica
    }
    val tieneSubrutaFija = subrutaFijaCodigo != null || subrutaFijaDbId != null
    val codigoSubrutaFijo = subrutaFija?.let { ruta ->
        val id = ruta.routeId?.trim()?.removePrefix("SB-")?.removePrefix("sb-")?.trim()
            ?: ruta.id.trim().removePrefix("SB-").removePrefix("sb-").trim()
        if (id.isBlank()) null else "SB-$id"
    }

    // Combis disponibles en la subruta asignada al chofer/jefe. Se toman del
    // catálogo real (el turno guardado puede apuntar a una unidad dada de baja).
    val combisDeMiSubruta = remember(unidades, subrutaFijaCodigo, subrutaFijaDbId) {
        if (subrutaFijaCodigo == null && subrutaFijaDbId == null) {
            emptyList()
        } else {
            unidades.filter { unidadEsDeSubruta(it, subrutaFijaCodigo, subrutaFijaDbId) }
        }
    }
    val subrutaFijaNombre = subrutaFija?.name

    val filas = remember(
        lineas, unidades, query, ocupadas, catalogSubrutas,
        TurnoSession.turno?.subrutaCodigo, esAdmin, esChofer, subrutaFijaCodigo, subrutaFijaDbId,
        cambioSolicitado, subrutaElegida, actualGlobal,
    ) {
        val unitsByLine = unidades.groupBy { it.lineaId }
        // El chofer/jefe solo ve su línea y su subruta asignada; el admin ve
        // todo el catálogo para elegir línea + subruta + combi.
        val lineasConSubruta = lineas.filter { linea ->
            codigoDeLinea(linea.nombre)?.let { code ->
                val subs = SelectedRouteHolder.subrutasForLine(code)
                when {
                    subrutaFijaCodigo != null -> subs.any { mismoIdSubruta(it.id, subrutaFijaCodigo) }
                    subrutaFijaDbId != null -> subs.any { it.subrutaDbId == subrutaFijaDbId }
                    else -> subs.isNotEmpty()
                }
            } == true
        }
        val filteredLineas = if (query.isBlank()) {
            lineasConSubruta
        } else {
            lineasConSubruta.filter { linea ->
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
                var empresaAgregada = false
                ls.forEach { linea ->
                    val code = codigoDeLinea(linea.nombre)
                    var subrutas = if (code != null) {
                        SelectedRouteHolder.subrutasForLine(code)
                    } else {
                        emptyList()
                    }
                    if (subrutaFijaCodigo != null) {
                        subrutas = subrutas.filter { sr -> mismoIdSubruta(sr.id, subrutaFijaCodigo) }
                    } else if (subrutaFijaDbId != null) {
                        subrutas = subrutas.filter { sr -> sr.subrutaDbId == subrutaFijaDbId }
                    }
                    if (subrutas.isEmpty()) return@forEach
                    if (!empresaAgregada) {
                        list.add(FilaRuta.Empresa(empresa))
                        empresaAgregada = true
                    }
                    list.add(FilaRuta.Linea(linea.id, linea.nombre ?: "Línea ${linea.id}", colorFromHex(linea.colorHex)))
                    val todas = unitsByLine[linea.id] ?: emptyList()
                    val us = if (!esAdmin && tieneSubrutaFija) {
                        todas.filter { unidadEsDeSubruta(it, subrutaFijaCodigo, subrutaFijaDbId) }
                    } else {
                        todas
                    }
                    val ocultarCombis = !esAdmin && (!tieneSubrutaFija || cambioSolicitado)
                    val actualId = if (!esAdmin) subrutaFijaCodigo ?: actualGlobal else actualGlobal

                    // Orden: línea → cada subruta con su cantidad de combis →
                    // las combis de esa subruta justo debajo.
                    val asignadas = mutableSetOf<Int>()
                    subrutas.forEach { sr ->
                        val combis = us.filter { u ->
                            val coincide = mismoIdSubruta(u.subrutaCodigo, sr.id) ||
                                mismoIdSubruta(u.subrutaCodigo, sr.routeId) ||
                                (sr.subrutaDbId != null && u.subrutaId == sr.subrutaDbId)
                            if (coincide) asignadas.add(u.id)
                            coincide
                        }
                        list.add(
                            FilaRuta.Subruta(
                                ruta = sr,
                                cantidadCombis = combis.size,
                                seleccionada = mismoIdSubruta(subrutaElegida, sr.id),
                                actual = mismoIdSubruta(actualId, sr.id),
                            )
                        )
                        if (!ocultarCombis) {
                            combis.forEach { u ->
                                list.add(FilaRuta.Unidad(u, u.id in ocupadas, indentado = true))
                            }
                        }
                    }

                    // Combis cuyo código no está en el catálogo: grupo aparte.
                    val sinAsignar = us.filter { it.id !in asignadas }
                    if (sinAsignar.isNotEmpty() && !ocultarCombis) {
                        list.add(FilaRuta.SinSubruta(sinAsignar.size))
                        sinAsignar.forEach { u ->
                            list.add(FilaRuta.Unidad(u, u.id in ocupadas, indentado = true))
                        }
                    }

                    if (us.isEmpty() && !ocultarCombis) {
                        val lineaActual = esAdmin || subrutas.any {
                            mismoIdSubruta(it.id, subrutaFijaCodigo)
                        }
                        if (lineaActual) {
                            list.add(FilaRuta.Vacia(linea.id, linea.nombre ?: "Línea ${linea.id}"))
                        }
                    }
                }
            }
        list
    }

    // Líneas del catálogo que contienen la subruta asignada al chofer/jefe.
    val lineasDeMiSubruta = remember(filas) { filas.filterIsInstance<FilaRuta.Linea>() }

    Dialog(
        onDismissRequest = { if (!assigning && !obligatorio) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !obligatorio,
            dismissOnClickOutside = !obligatorio,
        ),
    ) {
        // El alto se adapta a la pantalla real y el contenido se desplaza:
        // así ningún botón queda cortado en teléfonos pequeños o con teclado.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            val alturaMaxima = maxHeight * 0.94f
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = AppColors.cream,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .heightIn(max = alturaMaxima),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
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
                    WaykiTextButton(text = "Cancelar", onClick = { step = 0 }, modifier = Modifier.fillMaxWidth())
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

                    Spacer(Modifier.height(10.dp))
                    CombiActualCard(turno)
                    Spacer(Modifier.height(12.dp))
                    if (!esAdmin && tieneSubrutaFija) {
                        if (lineasDeMiSubruta.isNotEmpty()) {
                            Text(
                                text = "Líneas de tu subruta",
                                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                                color = AppColors.slate,
                            )
                            Spacer(Modifier.height(8.dp))
                            lineasDeMiSubruta.forEach { linea ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(linea.color, CircleShape)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = linea.nombre,
                                        style = AppTextStyles.waykiLabelMedium,
                                        color = AppColors.ink,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        Text(
                            text = subrutaFijaNombre?.let { "Combis de $it" }
                                ?: "Combis de tu subruta",
                            style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.slate,
                        )
                        Spacer(Modifier.height(8.dp))
                        when {
                            loading -> CombiLoadingPanel()
                            combisDeMiSubruta.isEmpty() -> CombiEmptyPanel(
                                title = "No hay combis disponibles en tu subruta.",
                                detail = "Las líneas de tu subruta aparecen arriba. Cuando registres una combi aquí, aparecerá en la lista.",
                                onRetry = {
                                    scope.launch {
                                        loading = true
                                        error = null
                                        unidades = BackendTurnosApi.unidadesTodas()
                                        loading = false
                                    }
                                },
                            )
                            else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                combisDeMiSubruta.forEach { u ->
                                    UnidadCard(
                                        unidad = u,
                                        ocupada = u.id in ocupadas && u.id != turno?.unidadId,
                                        enUso = u.id == turno?.unidadId,
                                        seleccionada = unidadSeleccionada?.id == u.id,
                                        onClick = { unidadSeleccionada = u },
                                    )
                                }
                            }
                        }
                    } else {
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
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = when {
                            !esAdmin && tieneSubrutaFija && loading -> "Estamos consultando tus combis."
                            !esAdmin && tieneSubrutaFija && combisDeMiSubruta.isEmpty() -> ""
                            !esAdmin && tieneSubrutaFija -> "Selecciona la combi con la que vas a trabajar."
                            else -> "Turno en curso."
                        },
                        style = AppTextStyles.waykiMini,
                        color = AppColors.slate,
                    )

                    Spacer(Modifier.height(18.dp))
                    if (!esAdmin && tieneSubrutaFija && unidadSeleccionada != null &&
                        unidadSeleccionada?.id != turno?.unidadId
                    ) {
                        WaykiPrimaryButton(
                            text = "TOMAR ESTA COMBI",
                            enabled = !assigning,
                            isLoading = assigning,
                            showArrow = false,
                            onClick = {
                                val choferId = UserSession.currentUser?.id
                                val unidad = unidadSeleccionada
                                if (choferId.isNullOrBlank() || unidad == null) {
                                    error = "No hay una sesión activa."
                                    return@WaykiPrimaryButton
                                }
                                scope.launch {
                                    assigning = true
                                    error = null
                                    val cierre = turno?.id?.let { BackendTurnosApi.finalizar(it) }
                                    if (cierre != null && cierre.isFailure) {
                                        error = "No se pudo cerrar el turno anterior: " +
                                            (cierre.exceptionOrNull()?.message ?: "intenta de nuevo")
                                        assigning = false
                                        return@launch
                                    }
                                    BackendTurnosApi.asignar(
                                        choferId = choferId,
                                        unidadId = unidad.id,
                                        subrutaId = unidad.subrutaId ?: subrutaFijaDbId,
                                        subrutaCodigo = subrutaFijaCodigo ?: codigoSubrutaFijo,
                                    )
                                        .onSuccess { nuevo ->
                                            TurnoSession.set(nuevo)
                                            ToastManager.show("Combi actualizada: ${TurnoSession.resumen}")
                                            assigning = false
                                            onDismiss()
                                        }
                                        .onFailure {
                                            error = it.message ?: "No se pudo tomar la combi"
                                            assigning = false
                                        }
                                }
                            },
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    WaykiPrimaryButton(text = "Continuar", onClick = onDismiss, showArrow = false)

                    // El cambio de subruta vive en Ajustes; aquí solo se elige
                    // la combi con la que se va a trabajar.
                    return@Column
                }

                // ---------- Escoger ruta / solicitar subruta ----------
                val tituloLista = when {
                    esAdmin -> "Escoge tu ruta"
                    !tieneSubrutaFija -> "Solicita tu subruta"
                    cambioSolicitado -> "Solicitud de cambio"
                    else -> "Escoge tu combi"
                }
                val subtituloLista = when {
                    esAdmin -> "Elige la línea, la subruta y la combi que vas a manejar hoy."
                    !tieneSubrutaFija ->
                        "Elige la subruta a la que quieres pertenecer; el jefe debe aprobarla."
                    cambioSolicitado ->
                        "La nueva subruta se aplicará cuando el jefe la apruebe."
                    else -> "Elige la combi disponible de tu subruta asignada."
                }
                DialogTitle(tituloLista)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtituloLista,
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

                if (!loading && filas.isNotEmpty()) {
                    Text(
                        text = "${filas.count { it is FilaRuta.Unidad }} combis disponibles",
                        style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.slate,
                    )
                    Spacer(Modifier.height(AppSpacing.xs))
                }

                when {
                    loading -> CombiLoadingPanel()

                    filas.isEmpty() -> CombiEmptyPanel(
                        title = error ?: "No hay combis disponibles para esta ruta.",
                        detail = if (error != null) "Revisa tu conexión e inténtalo de nuevo."
                        else "Cuando registres una combi en esta subruta aparecerá aquí.",
                        onRetry = {
                            scope.launch {
                                loading = true
                                error = null
                                lineas = BackendLineasApi.lineas(force = true)
                                unidades = BackendTurnosApi.unidadesTodas()
                                if (lineas.isEmpty()) error = "No se pudieron cargar las líneas."
                                loading = false
                            }
                        },
                    )

                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            filas.forEach { fila ->
                                when (fila) {
                                    is FilaRuta.Empresa -> EmpresaHeader(fila.nombre)
                                    is FilaRuta.Linea -> LineaHeader(fila.nombre, fila.color)
                                    is FilaRuta.Subruta -> SubrutaItem(
                                        ruta = fila.ruta,
                                        cantidadCombis = fila.cantidadCombis,
                                        seleccionada = fila.seleccionada,
                                        actual = fila.actual,
                                        onSelect = { subrutaElegida = it.id },
                                    )
                                    is FilaRuta.SinSubruta -> Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 18.dp, top = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AppColors.surfaceVariant.copy(alpha = 0.6f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            text = "Sin subruta asignada · ${fila.cantidad} combi(s)",
                                            style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                                            color = AppColors.slate,
                                        )
                                    }
                                    is FilaRuta.Unidad -> UnidadCard(
                                        unidad = fila.data,
                                        ocupada = fila.ocupada,
                                        seleccionada = unidadSeleccionada?.id == fila.data.id,
                                        indentado = fila.indentado,
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

                // Confirmación visible de lo que se va a asignar.
                val subrutaElegidaNombre = subrutaElegida?.let { id ->
                    catalogSubrutas.firstOrNull { mismoIdSubruta(it.id, id) }?.name
                }
                if (subrutaElegidaNombre != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Subruta elegida: $subrutaElegidaNombre",
                        style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (cambioSolicitado) {
                        Text(
                            text = "Las combis se habilitarán cuando se apruebe el cambio.",
                            style = AppTextStyles.waykiMini,
                            color = AppColors.slate,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                if (cambioSolicitado) {
                    WaykiPrimaryButton(
                        text = "SOLICITAR CAMBIO",
                        enabled = subrutaElegida != null,
                        onClick = {
                            val destino = subrutaElegida?.let { id ->
                                catalogSubrutas.firstOrNull { mismoIdSubruta(it.id, id) }?.subrutaDbId
                            }
                            if (destino != null) {
                                onSolicitarCambioSubruta(destino.toString())
                                subrutaElegida = null
                            } else {
                                error = "Selecciona una subruta válida."
                            }
                        },
                        showArrow = false,
                    )
                } else WaykiPrimaryButton(
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
                            val lineCode = codigoDeLinea(unidad.lineaNombre)
                            val catalogo = SelectedRouteHolder.catalog.value
                            // El backend resuelve la subruta por `codigo` exacto (SB-<routeId>).
                            fun codigo(v: String?) = v?.trim()
                                ?.removePrefix("SB-")?.removePrefix("sb-")?.trim()
                                ?.takeIf { it.isNotBlank() }
                                ?.let { "SB-$it" }

                            // 1) Si el chofer tocó una subruta de ESTA línea, gana.
                            // 2) Si no, gana la subruta asignada al vehículo, aunque
                            //    no esté en el catálogo (se manda su código tal cual).
                            // 3) Solo al final se usa un default de la línea.
                            val elegida = if (esChofer) null else subrutaElegida?.let { id ->
                                catalogo.firstOrNull { mismoIdSubruta(it.id, id) }
                            }
                            val eleccionValida = elegida != null &&
                                (lineCode == null || elegida.parent.equals(lineCode, true))
                            if (!esChofer && subrutaElegida != null && !eleccionValida) {
                                // Antes se caía al respaldo de la unidad y parecía
                                // que la marca de selección se perdía. Mejor decir
                                // por qué no se puede asignar y conservar la marca.
                                error = "La subruta elegida no corresponde a la línea de la unidad. Elige una compatible."
                                assigning = false
                                return@launch
                            }
                            val codigoElegido = if (eleccionValida) {
                                codigo(elegida.routeId) ?: codigo(elegida.id)
                            } else {
                                null
                            }
                            val codigoVehiculo = codigo(unidad.subrutaCodigo)
                            val codigoDefault = lineCode?.let { lc ->
                                SelectedRouteHolder.subrutasForLine(lc).firstOrNull()
                            }?.let { codigo(it.routeId) ?: codigo(it.id) }
                            // El chofer usa SIEMPRE la subruta que aprobó el jefe,
                            // aunque la combi traiga otro código heredado.
                            val codigoTurno = if (esChofer) {
                                subrutaFijaCodigo ?: codigoSubrutaFijo ?: codigoVehiculo ?: codigoDefault
                            } else {
                                codigoElegido ?: codigoVehiculo ?: codigoDefault
                            }

                            // El backend solo permite un turno activo por chofer/unidad:
                            // cerramos el actual antes de asignar la nueva ruta/unidad.
                            // Si el cierre falla, abortamos para no dejar dos turnos.
                            val turnoActual = com.gowayki.nesh.core.session.TurnoSession.turno
                            if (turnoActual != null) {
                                val cierre = BackendTurnosApi.finalizar(turnoActual.id)
                                if (cierre.isFailure) {
                                    error = "No se pudo cerrar el turno anterior: " +
                                        (cierre.exceptionOrNull()?.message ?: "intenta de nuevo")
                                    assigning = false
                                    return@launch
                                }
                            }

                            BackendTurnosApi.asignar(
                                choferId = choferId,
                                unidadId = unidad.id,
                                subrutaId = unidad.subrutaId ?: elegida?.subrutaDbId ?: subrutaFijaDbId,
                                subrutaCodigo = codigoTurno,
                            )
                                .onSuccess { turno ->
                                    TurnoSession.set(turno)
                                    val subCode = turno.subrutaCodigo?.takeIf { it.isNotBlank() }
                                        ?: codigoTurno
                                        ?: unidad.subrutaCodigo
                                    if (subCode != null) {
                                        SelectedRouteHolder.selectById(subCode)
                                    }
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
                WaykiTextButton(text = "Ahora no", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
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
private fun SubrutaItem(
    ruta: CombiRoute,
    cantidadCombis: Int,
    seleccionada: Boolean,
    actual: Boolean,
    onSelect: (CombiRoute) -> Unit,
) {
    val color = ruta.color
    val fondo = if (seleccionada) color else color.copy(alpha = 0.12f)
    val texto = if (seleccionada) textoContraste(color) else AppColors.ink
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = fondo),
        border = BorderStroke(
            width = if (actual) 2.dp else 1.dp,
            color = if (actual) AppColors.ink else color.copy(alpha = 0.6f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = color.copy(alpha = 0.3f)),
                onClick = { onSelect(ruta) },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (seleccionada) texto else color),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ruta.name,
                    style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                    color = texto,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (actual) {
                    Text(
                        text = "Actual",
                        style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                        color = texto,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$cantidadCombis combi${if (cantidadCombis == 1) "" else "s"}",
                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                color = texto,
            )
            if (seleccionada) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    FeatherIcons.Check,
                    contentDescription = "Seleccionada",
                    tint = texto,
                    modifier = Modifier.size(14.dp),
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

/** La combi que el chofer está usando ahora, siempre visible en el diálogo. */
@Composable
private fun CombiActualCard(turno: BackendTurnoDto?) {
    val placa = turno?.placa?.takeIf { it.isNotBlank() }
    val padron = turno?.numeroPadron?.takeIf { it.isNotBlank() }
    val detalle = listOfNotNull(
        padron?.let { "Unidad $it" },
        placa,
    ).joinToString(" · ").ifBlank { "Sin datos de la unidad" }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.primary.copy(alpha = 0.10f)),
        border = BorderStroke(1.dp, AppColors.primary.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = SolidGroup.Bus,
                    contentDescription = null,
                    tint = AppColors.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(AppSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tu combi actual",
                    style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.primary,
                )
                Text(
                    text = detalle,
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.ink,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppColors.primary)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "En uso",
                    style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = AppColors.onPrimary,
                )
            }
        }
    }
}

/** Estado de carga del selector: texto + tarjetas esqueleto, sin frames vacíos. */
@Composable
private fun CombiLoadingPanel() {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                color = AppColors.primary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(AppSpacing.sm))
            Text(
                text = "Cargando tus combis…",
                style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                color = AppColors.ink,
            )
        }
        Spacer(Modifier.height(AppSpacing.xs))
        repeat(3) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.surface),
                border = BorderStroke(1.dp, AppColors.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WaykiSkeleton(modifier = Modifier.size(38.dp), shape = RoundedCornerShape(10.dp))
                    Spacer(Modifier.width(AppSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        WaykiSkeleton(modifier = Modifier.fillMaxWidth().height(14.dp))
                        Spacer(Modifier.height(AppSpacing.xs))
                        WaykiSkeleton(modifier = Modifier.fillMaxWidth(0.6f).height(11.dp))
                    }
                }
            }
        }
    }
}

/** Estado vacío o de error del selector, con salida clara (reintentar). */
@Composable
private fun CombiEmptyPanel(
    title: String,
    detail: String,
    onRetry: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.surface),
        border = BorderStroke(1.dp, AppColors.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = SolidGroup.Bus,
                contentDescription = null,
                tint = AppColors.slate,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.height(AppSpacing.sm))
            Text(
                text = title,
                style = AppTextStyles.waykiLabelMedium,
                color = AppColors.ink,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(AppSpacing.xs))
            Text(
                text = detail,
                style = AppTextStyles.waykiMini,
                color = AppColors.slate,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(AppSpacing.md))
            WaykiSecondaryButton(text = "Reintentar", onClick = onRetry)
        }
    }
}

/** Tarjeta de combi: identidad, subruta y estado de un vistazo. */
@Composable
private fun UnidadCard(
    unidad: BackendUnidadDto,
    ocupada: Boolean,
    seleccionada: Boolean,
    enUso: Boolean = false,
    indentado: Boolean = false,
    onClick: () -> Unit,
) {
    val colorEstado = when {
        enUso || seleccionada -> AppColors.primary
        ocupada -> AppColors.slate
        else -> AppColors.tertiary
    }
    val fondo = when {
        enUso || seleccionada -> AppColors.primary.copy(alpha = 0.10f)
        ocupada -> AppColors.surfaceVariant.copy(alpha = 0.5f)
        else -> AppColors.surface
    }
    val borde = when {
        enUso || seleccionada -> AppColors.primary
        ocupada -> AppColors.outlineVariant
        else -> AppColors.outline.copy(alpha = 0.35f)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = fondo),
        border = BorderStroke(if (seleccionada) 2.dp else 1.dp, borde),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indentado) 18.dp else 0.dp)
            .clickable(
                enabled = !ocupada && !enUso,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = AppColors.primary.copy(alpha = 0.18f)),
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorEstado.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = SolidGroup.Bus,
                    contentDescription = null,
                    tint = colorEstado,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(AppSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Unidad ${unidad.numeroPadron ?: "—"}",
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (ocupada) AppColors.slate else AppColors.ink,
                )
                Text(
                    text = unidad.placa ?: "Sin placa",
                    style = AppTextStyles.waykiMini,
                    color = AppColors.slate,
                )
                if (!unidad.subrutaNombre.isNullOrBlank()) {
                    Spacer(Modifier.height(AppSpacing.xxs))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppColors.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = unidad.subrutaNombre,
                            style = AppTextStyles.waykiMini.copy(fontSize = 10.sp),
                            color = AppColors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.width(AppSpacing.sm))
            Column(horizontalAlignment = Alignment.End) {
                when {
                    enUso -> Text(
                        text = "En uso",
                        style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = AppColors.primary,
                    )
                    ocupada -> Text(
                        text = "En ruta",
                        style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = AppColors.error,
                    )
                    seleccionada -> Icon(
                        imageVector = FeatherIcons.Check,
                        contentDescription = "Seleccionada",
                        tint = AppColors.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    else -> Text(
                        text = "Disponible",
                        style = AppTextStyles.waykiMini.copy(fontSize = 10.sp),
                        color = AppColors.tertiary,
                    )
                }
            }
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

/** Texto legible sobre el color de la línea: oscuro en fondos claros. */
private fun textoContraste(fondo: Color): Color =
    if (fondo.luminance() > 0.55f) AppColors.ink else Color.White

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
