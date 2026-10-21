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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var restored = false
    private var userSelected = false

    fun select(route: CombiRoute?) {
        userSelected = true
        _selected.value = route
        route?.id?.let { id ->
            scope.launch { RouteSelectionStore.save(id) }
        }
    }

    fun selectById(id: String?) {
        val route = id?.let { ArequipaRoutes.byId(it) } ?: return
        select(route)
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
}
