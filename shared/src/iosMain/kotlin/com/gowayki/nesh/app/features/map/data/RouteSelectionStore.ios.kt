package com.gowayki.nesh.app.features.map.data

import platform.Foundation.NSUserDefaults

actual object RouteSelectionStore {
    private const val KEY = "wayki_selected_route_id"

    actual suspend fun save(routeId: String) {
        runCatching {
            NSUserDefaults.standardUserDefaults.setObject(routeId, KEY)
        }
    }

    actual suspend fun load(): String? = runCatching {
        NSUserDefaults.standardUserDefaults.stringForKey(KEY)
    }.getOrNull()
}
