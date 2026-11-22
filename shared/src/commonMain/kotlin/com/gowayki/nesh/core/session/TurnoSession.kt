package com.gowayki.nesh.core.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gowayki.nesh.app.features.map.domain.model.CombiRoute
import com.gowayki.nesh.data.entities.backend.BackendTurnoDto
import com.gowayki.nesh.data.repository.BackendTurnosApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Estado observable del turno del chofer (línea + unidad + placa).
 *
 * El backend relaciona al chofer con su línea mediante el turno:
 * chofer -> asignacion_turno -> unidad -> linea.
 */
object TurnoSession {
    var turno by mutableStateOf<BackendTurnoDto?>(null)
        private set

    /** Turnos activos del sistema; sirve para saber qué unidades están ocupadas. */
    var activeTurnos by mutableStateOf<List<BackendTurnoDto>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var loaded by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    /** Evita que la ventana "Tu ruta de hoy" reaparezca sola en la misma sesión. */
    var promptShown: Boolean = false

    private val refreshMutex = Mutex()

    val hasTurno: Boolean
        get() = turno != null

    val lineaNombre: String
        get() = turno?.lineaNombre?.takeIf { it.isNotBlank() } ?: "Sin ruta asignada"

    val subrutaNombre: String?
        get() = turno?.subrutaNombre?.takeIf { it.isNotBlank() }
            ?: com.gowayki.nesh.app.features.map.presentation.SelectedRouteHolder.selected.value?.name

    val unidadTexto: String
        get() = turno?.numeroPadron
            ?.takeIf { it.isNotBlank() }
            ?.let { "Unidad $it" }
            ?: "Sin unidad"

    val placa: String?
        get() = turno?.placa?.takeIf { it.isNotBlank() }

    /** Texto corto para cabeceras: "Línea C2 · Unidad 08 · V1A-954". */
    val resumen: String
        get() {
            val t = turno ?: return "Sin ruta asignada"
            return buildList {
                t.lineaNombre?.takeIf { it.isNotBlank() }?.let { add(it) }
                t.numeroPadron?.takeIf { it.isNotBlank() }?.let { add("Unidad $it") }
                t.placa?.takeIf { it.isNotBlank() }?.let { add(it) }
            }.joinToString(" · ").ifBlank { "Sin ruta asignada" }
        }

    /** Ids de unidades ocupadas por un turno activo. */
    val unidadesOcupadas: Set<Int>
        get() = activeTurnos.mapNotNull { it.unidadId }.toSet()

    /**
     * Otros choferes con turno activo en la MISMA subruta concreta. Es la fuente
     * simétrica para Home y para «Nuevo grupo»: todos consultan /turnos/activos,
     * así que si tú los ves, ellos te ven a ti.
     */
    fun companerosEnSubruta(route: CombiRoute?): List<BackendTurnoDto> {
        if (route == null) return emptyList()
        val miId = UserSession.currentUserId
        val miTurnoId = turno?.id
        return activeTurnos
            .filter { t ->
                !t.choferId.isNullOrBlank() && t.choferId != miId && t.id != miTurnoId &&
                    turnoMatchesSubroute(t, route)
            }
            .distinctBy { it.choferId }
    }

    /** Consulta el turno del usuario logueado y los turnos activos. */
    suspend fun refresh() {
        val choferId = UserSession.currentUser?.id?.takeIf { it.isNotBlank() }
        if (choferId == null) {
            loaded = true
            return
        }
        refreshMutex.withLock {
            isLoading = true
            error = null
            try {
                val holder = com.gowayki.nesh.app.features.map.presentation.SelectedRouteHolder
                runCatching { holder.refreshCatalog() }

                val consulta = BackendTurnosApi.turnoActivo(choferId)
                consulta
                    .onSuccess { nuevo ->
                        turno = nuevo
                        val subrutaId = nuevo?.subrutaCodigo?.takeIf { it.isNotBlank() }
                        val seleccionActual = holder.selected.value
                        val yaSeleccionada = subrutaId != null && (
                            seleccionActual?.id.equals(subrutaId, ignoreCase = true) ||
                                seleccionActual?.routeId.equals(subrutaId, ignoreCase = true)
                            )
                        // `selectById` reinicia el sentido a IDA: solo se llama
                        // cuando la subruta asignada cambia de verdad, no en
                        // cada refresco. La asignación de "Mi ruta / unidad"
                        // es la fuente oficial: si cambió, se impone aunque
                        // hubiera una selección manual.
                        if (subrutaId != null && !yaSeleccionada) {
                            holder.selectById(subrutaId)
                        }
                    }
                    .onFailure { e ->
                        // Se conserva el turno anterior: un fallo puntual de
                        // red no debe vaciar la tarjeta del Home.
                        error = e.message ?: "No se pudo consultar tu turno"
                    }

                activeTurnos = BackendTurnosApi.turnosActivos()
            } catch (e: Exception) {
                error = e.message ?: "No se pudo consultar tu turno"
            } finally {
                loaded = true
                isLoading = false
            }
        }
    }

    /** Refresca solo la lista de turnos activos (unidades ocupadas). */
    suspend fun refreshActiveTurnos() {
        activeTurnos = BackendTurnosApi.turnosActivos()
    }

    /** Guarda el turno recién asignado. */
    fun set(nuevo: BackendTurnoDto) {
        turno = nuevo
        activeTurnos = activeTurnos.filterNot { it.id == nuevo.id } + nuevo
        loaded = true
        error = null
    }

    /** Quita el turno actual (p. ej. al cambiar de unidad) sin borrar los demás. */
    fun clearTurno() {
        val id = turno?.id
        turno = null
        activeTurnos = if (id != null) activeTurnos.filterNot { it.id == id } else activeTurnos
        loaded = true
        error = null
    }

    fun clear() {
        turno = null
        activeTurnos = emptyList()
        loaded = false
        error = null
        promptShown = false
    }
}

/** ¿Pertenece el turno a la misma subruta concreta (no solo a la misma línea)? */
private fun turnoMatchesSubroute(turno: BackendTurnoDto, route: CombiRoute): Boolean {
    fun norm(value: String?) = value?.trim()
        ?.removePrefix("SB-")?.removePrefix("sb-")
        ?.trim()?.takeIf { it.isNotBlank() }
    val routeKeys = listOfNotNull(norm(route.id), norm(route.routeId), norm(route.returnRouteId))
    val turnoKeys = listOfNotNull(norm(turno.subrutaCodigo), norm(turno.subrutaId?.toString()))
    if (turnoKeys.any { k -> routeKeys.any { it.equals(k, ignoreCase = true) } }) return true
    val nombre = turno.subrutaNombre?.trim()
    return !nombre.isNullOrBlank() && nombre.equals(route.name.trim(), ignoreCase = true)
}
