package com.gowayki.nesh.core.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
                turno = BackendTurnosApi.turnoActivo(choferId)
                turno?.lineaNombre?.let { com.gowayki.nesh.app.features.map.presentation.SelectedRouteHolder.selectLineByName(it) }
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
