package com.gowayki.nesh.app.features.map.data

import platform.Foundation.NSUserDefaults

actual object RouteSelectionStore {
    private const val KEY = "wayki_selected_route_id"

    actual suspend fun save(
        routeId: String,
        routeName: String,
        idaRouteId: String?,
        returnRouteId: String?,
        direction: String
    ) {
        runCatching {
            NSUserDefaults.standardUserDefaults.setObject(routeId, KEY)
        }
    }

    actual suspend fun load(): String? = runCatching {
        NSUserDefaults.standardUserDefaults.stringForKey(KEY)
    }.getOrNull()

    actual suspend fun clear() {
        runCatching { NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY) }
    }
}
