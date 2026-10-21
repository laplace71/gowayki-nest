package com.gowayki.nesh.app.features.map.data

import com.gowayki.nesh.app.features.sos.domain.model.AndroidSosContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object RouteSelectionStore {
    private const val FILE = "wayki_route_prefs"
    private const val KEY = "selected_route_id"

    actual suspend fun save(routeId: String) {
        val ctx = AndroidSosContext.appContext ?: return
        withContext(Dispatchers.IO) {
            runCatching {
                ctx.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
                    .edit().putString(KEY, routeId).commit()
            }
        }
    }

    actual suspend fun load(): String? {
        val ctx = AndroidSosContext.appContext ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
                ctx.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
                    .getString(KEY, null)
            }.getOrNull()
        }
    }
}
