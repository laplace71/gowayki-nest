// Actual iOS — NSUserDefaults (sin librerías nuevas).
package com.gowayki.nesh.app.features.settings.data

import platform.Foundation.NSUserDefaults

actual object ThemeStore {
    private const val KEY = "wayki_dark_mode"

    actual fun isDark(): Boolean = runCatching {
        NSUserDefaults.standardUserDefaults.boolForKey(KEY)
    }.getOrDefault(false)

    actual fun setDark(value: Boolean) {
        runCatching {
            NSUserDefaults.standardUserDefaults.setBool(value, KEY)
        }
    }
}
