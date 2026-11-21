package com.gowayki.nesh.core.session

import platform.Foundation.NSUserDefaults

actual object LastDniStore {
    private const val KEY = "wayki_session_last_dni"

    actual fun read(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY)?.takeIf { it.length == 8 }

    actual fun write(dni: String?) {
        if (dni.isNullOrBlank()) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(dni, KEY)
        }
    }
}
