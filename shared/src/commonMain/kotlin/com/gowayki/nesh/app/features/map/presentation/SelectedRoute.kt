package com.gowayki.nesh.app.features.map.presentation

import com.gowayki.nesh.app.features.map.data.RouteCatalogStore
import com.gowayki.nesh.app.features.map.data.RouteSelectionStore
import com.gowayki.nesh.app.features.map.domain.model.ArequipaRoutes
import com.gowayki.nesh.app.features.map.domain.model.CombiRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Sentido del recorrido seleccionado en la app (IDA o VUELTA). */
enum class RouteDirection { IDA, RETORNO }

/** Geometría y paraderos cacheados para restaurar el mapa al volver a entrar. */
data class RouteGeometryCache(
    val idaPoints: List<Pair<Double, Double>> = emptyList(),
    val returnPoints: List<Pair<Double, Double>> = emptyList(),
    val matchedIdaPoints: List<Pair<Double, Double>> = emptyList(),
    val matchedReturnPoints: List<Pair<Double, Double>> = emptyList(),
    val idaStops: List<com.gowayki.nesh.app.features.map.data.SitRouteStop> = emptyList(),
    val returnStops: List<com.gowayki.nesh.app.features.map.data.SitRouteStop> = emptyList(),
)

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

    /**
     * Evento explícito de confirmación de subruta. Se emite aunque el id sea
     * el mismo de antes: el repositorio de chat lo usa para re-unirse al grupo
     * de ruta tras una reactivación de cuenta.
     */
    private val _routeConfirmed = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val routeConfirmed: SharedFlow<String> = _routeConfirmed.asSharedFlow()

    private val _catalog = MutableStateFlow<List<CombiRoute>>(emptyList())
    /** Catálogo dinámico de subrutas (desde BD). */
    val catalog: StateFlow<List<CombiRoute>> = _catalog.asStateFlow()

    fun setDirection(direction: RouteDirection) {
        _direction.value = direction
        persistSelection()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var restored = false
    private var userSelected = false
    /** true si se restauró una ruta guardada previamente (el usuario ya había elegido). */
    private var restoredFromStore = false

    /** El usuario entró/eligió una ruta de forma efectiva (selección, turno o ruta guardada). */
    val hasEnteredRoute: Boolean
        get() = userSelected || restoredFromStore

    /** El usuario eligió una subruta explícitamente (no viene del turno). */
    val userSelectedRoute: Boolean
        get() = userSelected

    /** Carga catálogo de subrutas desde BD (con respaldo empaquetado). */
    suspend fun refreshCatalog(force: Boolean = false) {
        val subs = RouteCatalogStore.refresh(force)
        _catalog.value = subs
        // Si no hay selección concreta válida, asegurar una subruta por defecto.
        val cur = _selected.value
        if (cur == null || !cur.isSub) {
            val def = subs.firstOrNull() ?: ArequipaRoutes.all.firstOrNull { it.isSub && it.routeId != null }
            if (def != null) _selected.value = def
        }
    }

    /** Código C2/C3… desde el nombre real de la línea del catálogo. */
    fun codigoParaNombre(nombre: String?): String? = RouteCatalogStore.codigoParaNombre(nombre)

    /** Subrutas disponibles de una línea (C2, C3...). */
    fun subrutasForLine(lineCode: String): List<CombiRoute> {
        val fromCatalog = _catalog.value.filter { it.parent.equals(lineCode, ignoreCase = true) }
        if (fromCatalog.isNotEmpty()) return fromCatalog
        return ArequipaRoutes.all.filter { it.isSub && it.parent.equals(lineCode, ignoreCase = true) }
    }

    private fun findById(id: String): CombiRoute? {
        val norm = id.trim()
        val withoutSb = norm.removePrefix("SB-").removePrefix("sb-").trim()
        val withSb = "SB-$withoutSb"
        return _catalog.value.firstOrNull {
            it.id.equals(norm, ignoreCase = true) ||
            it.id.equals(withSb, ignoreCase = true) ||
            (withoutSb.isNotEmpty() && it.id.equals(withoutSb, ignoreCase = true)) ||
            it.routeId == norm || (withoutSb.isNotEmpty() && it.routeId == withoutSb)
        } ?: ArequipaRoutes.all.firstOrNull {
            it.id.equals(norm, ignoreCase = true) ||
            it.id.equals(withSb, ignoreCase = true) ||
            (withoutSb.isNotEmpty() && it.id.equals(withoutSb, ignoreCase = true)) ||
            it.routeId == norm || (withoutSb.isNotEmpty() && it.routeId == withoutSb)
        }
    }

    private var persistJob: Job? = null
    /** Serializa guardar/borrar para que un clear de logout no borre la ruta nueva. */
    private val storeMutex = Mutex()

    private fun persistSelection() {
        val route = _selected.value ?: return
        val dir = _direction.value.name
        persistJob?.cancel()
        persistJob = scope.launch {
            storeMutex.withLock {
                RouteSelectionStore.save(
                    routeId = route.id,
                    routeName = route.name,
                    idaRouteId = route.routeId,
                    returnRouteId = route.returnRouteId,
                    direction = dir
                )
            }
        }
    }

    fun select(route: CombiRoute?) {
        val mismaRuta = _selected.value?.id?.equals(route?.id, ignoreCase = true) == true
        userSelected = true
        // Solo se resetea el sentido al CAMBIAR de ruta: si se re-elige la
        // misma, el mapa y Recorrido deben conservar Ida/Vuelta.
        if (!mismaRuta) _direction.value = RouteDirection.IDA
        _selected.value = route
        if (route != null) {
            persistSelection()
            _routeConfirmed.tryEmit(route.id)
        }
    }

    fun selectById(id: String?) {
        if (id.isNullOrBlank()) return
        val route = findById(id) ?: run {
            val clean = id.trim()
            if (clean.equals("general", ignoreCase = true)) return
            CombiRoute(
                id = clean,
                name = clean,
                operator = "",
                color = androidx.compose.ui.graphics.Color(0xFFE53935),
                waypoints = emptyList(),
                isSub = true,
                parent = null
            )
        }
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
        val fromCatalog = _catalog.value.firstOrNull { it.isSub && it.parent.equals(code, true) && it.routeId != null }
        if (fromCatalog != null) return fromCatalog
        return ArequipaRoutes.all.firstOrNull { it.isSub && it.parent == code && it.routeId != null }
            ?: ArequipaRoutes.byId(code)
    }

    /** Restaura la última ruta guardada y carga catálogo desde BD. Idempotente. */
    suspend fun restore() {
        if (restored) return
        restored = true
        // Cargar catálogo de subrutas desde BD (con respaldo empaquetado)
        val subs = runCatching { RouteCatalogStore.refresh() }.getOrDefault(emptyList())
        if (subs.isNotEmpty()) _catalog.value = subs

        val id = RouteSelectionStore.load() ?: run {
            // Asegurar selección por defecto de subruta concreta
            if (_selected.value?.isSub != true) {
                val def = subs.firstOrNull() ?: ArequipaRoutes.all.firstOrNull { it.isSub && it.routeId != null }
                if (def != null) _selected.value = def
            }
            return
        }
        if (userSelected) return
        // Solo se restauran sub-rutas reales (no líneas agregadas).
        val saved = findById(id) ?: RouteCatalogStore.byId(id)
        if (saved != null && saved.isSub) {
            restoredFromStore = true
            _selected.value = saved
        } else {
            val def = subs.firstOrNull() ?: ArequipaRoutes.all.firstOrNull { it.isSub && it.routeId != null }
            if (def != null) _selected.value = def
        }
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

    private var _cachedGeometry: RouteGeometryCache? = null

    fun cachedGeometry(routeId: String?): RouteGeometryCache? =
        if (_cachedRouteId == routeId) _cachedGeometry else null

    fun cacheGeometry(routeId: String?, geometry: RouteGeometryCache) {
        _cachedRouteId = routeId
        _cachedGeometry = geometry
        _idaPoints.value = geometry.idaPoints
        _returnPoints.value = geometry.returnPoints
    }

    fun clearGeometryCache() {
        _cachedRouteId = null
        _cachedGeometry = null
        _idaPoints.value = emptyList()
        _returnPoints.value = emptyList()
    }

    /**
     * Cierra la sesión de ruta: ni la selección en memoria ni la persistida
     * pueden pasar al siguiente chofer que inicie sesión en este dispositivo.
     * Idempotente y sin llamar a UserSession (evita recursion con clear()).
     */
    fun resetForLogout() {
        persistJob?.cancel()
        persistJob = null
        userSelected = false
        restoredFromStore = false
        restored = false
        _selected.value = null
        _direction.value = RouteDirection.IDA
        clearGeometryCache()
        scope.launch { runCatching { storeMutex.withLock { RouteSelectionStore.clear() } } }
    }
}
