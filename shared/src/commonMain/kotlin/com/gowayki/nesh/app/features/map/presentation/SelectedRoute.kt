package com.gowayki.nesh.app.features.map.presentation

import com.gowayki.nesh.app.features.map.data.RouteSelectionStore
import com.gowayki.nesh.app.features.map.domain.model.ArequipaRoutes
import com.gowayki.nesh.app.features.map.domain.model.CombiRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Sentido del recorrido seleccionado en la app (IDA o VUELTA). */
enum class RouteDirection { IDA, RETORNO }

/**
 * Holder global (singleton) de la ruta de combi seleccionada por el usuario.
 * Se usa desde el Mapa y el Welcome, y se persiste para que no se reinicie
 * al salir de la vista o cerrar la app.
 */
object SelectedRouteHolder {
    private val _selected = MutableStateFlow<CombiRoute?>(
        ArequipaRoutes.all.firstOrNull { it.isSub && it.routeId != null }
            ?: ArequipaRoutes.all.firstOrNull()
    )
    val selected: StateFlow<CombiRoute?> = _selected.asStateFlow()

    private val _direction = MutableStateFlow(RouteDirection.IDA)

    /** Sentido compartido entre "Tu recorrido" y el mapa. */
    val direction: StateFlow<RouteDirection> = _direction.asStateFlow()

    fun setDirection(direction: RouteDirection) {
        _direction.value = direction
        persistSelection()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var restored = false
    private var userSelected = false

    private fun persistSelection() {
        val route = _selected.value ?: return
        val dir = _direction.value.name
        scope.launch {
            RouteSelectionStore.save(
                routeId = route.id,
                routeName = route.name,
                idaRouteId = route.routeId,
                returnRouteId = route.returnRouteId,
                direction = dir
            )
        }
    }

    fun select(route: CombiRoute?) {
        userSelected = true
        // Resetea el sentido al cambiar de ruta.
        _direction.value = RouteDirection.IDA
        _selected.value = route
        if (route != null) {
            persistSelection()
        }
    }

    fun selectById(id: String?) {
        val route = id?.let { ArequipaRoutes.byId(it) } ?: return
        select(route)
    }

    /**
     * Selecciona la línea del turno a partir de su nombre del backend
     * ("Línea C2", "Línea C3", ...). Si no coincide con el catálogo, no cambia.
     * No reemplaza una subruta ya elegida de la misma línea.
     */
    fun selectLineByName(nombre: String?) {
        val code = lineCode(nombre) ?: return
        val current = _selected.value
        if (current != null) {
            val currentCode = if (current.isSub) current.parent else current.id
            if (currentCode == code && current.isSub) return
        }
        val route = routeForLineName(nombre) ?: return
        select(route)
    }

    /**
     * Como [selectLineByName], pero no reemplaza la subruta que el usuario ya
     * eligió si pertenece a la misma línea (evita volver a "líneas en general").
     */
    fun selectLineByNameIfNeeded(nombre: String?) {
        val code = lineCode(nombre) ?: return
        val current = _selected.value
        if (current != null) {
            val currentCode = if (current.isSub) current.parent else current.id
            // Ya hay una subruta concreta de esta línea: no la pises.
            if (currentCode == code && current.isSub) return
            // Misma línea general ya seleccionada: nada que cambiar.
            if (currentCode == code && current.routeId != null) return
        }
        val route = routeForLineName(nombre) ?: return
        select(route)
    }

    private fun lineCode(nombre: String?): String? {
        val clean = nombre?.trim() ?: return null
        return clean
            .removePrefix("Línea")
            .removePrefix("Linea")
            .trim()
            .split(" ")
            .firstOrNull()
            ?.trim()
            ?.uppercase()
            ?.takeIf { it.isNotBlank() }
    }

    private fun routeForLineName(nombre: String?): CombiRoute? {
        val code = lineCode(nombre) ?: return null
        // Preferir una subruta concreta (con trazado real) antes que la línea general.
        return ArequipaRoutes.all.firstOrNull { it.isSub && it.parent == code && it.routeId != null }
            ?: ArequipaRoutes.byId(code)
    }

    /** Restaura la última ruta guardada. Idempotente. */
    suspend fun restore() {
        if (restored) return
        restored = true
        val id = RouteSelectionStore.load() ?: return
        if (userSelected) return
        // Solo se restauran sub-rutas reales (no líneas agregadas).
        val saved = ArequipaRoutes.byId(id) ?: return
        if (saved.isSub) _selected.value = saved
    }

    // --- Cache de geometría para que el mapa muestre la ruta al instante
    //     al volver a entrar al módulo (sin esperar a RouteGeometryStore).
    //     Se guarda el ID de ruta para evitar devolver geometría obsoleta
    //     cuando el usuario cambia de ruta. ---
    private var _cachedRouteId: String? = null
    private val _idaPoints = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    private val _returnPoints = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val idaPoints: StateFlow<List<Pair<Double, Double>>> = _idaPoints.asStateFlow()
    val returnPoints: StateFlow<List<Pair<Double, Double>>> = _returnPoints.asStateFlow()

    /**
     * Devuelve los puntos IDA cacheados solo si pertenecen a [routeId].
     * Evita que MapScreen inicialice con geometría de una ruta anterior.
     */
    fun cachedIdaPoints(routeId: String?): List<Pair<Double, Double>> =
        if (_cachedRouteId == routeId) _idaPoints.value else emptyList()

    /**
     * Devuelve los puntos de retorno cacheados solo si pertenecen a [routeId].
     */
    fun cachedReturnPoints(routeId: String?): List<Pair<Double, Double>> =
        if (_cachedRouteId == routeId) _returnPoints.value else emptyList()

    fun cacheGeometry(routeId: String?, ida: List<Pair<Double, Double>>, ret: List<Pair<Double, Double>>) {
        _cachedRouteId = routeId
        _idaPoints.value = ida
        _returnPoints.value = ret
    }

    fun clearGeometryCache() {
        _cachedRouteId = null
        _idaPoints.value = emptyList()
        _returnPoints.value = emptyList()
    }
}
