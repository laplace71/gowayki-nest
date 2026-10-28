// Actual Android — SharedPreferences (sin librerías nuevas).
package com.gowayki.nesh.app.features.settings.data

import android.content.Context
import com.gowayki.nesh.app.features.sos.domain.model.AndroidSosContext

actual object ThemeStore {
    private const val FILE = "theme_store"
    private const val KEY = "dark_mode"

    actual fun isDark(): Boolean {
        val ctx = AndroidSosContext.appContext ?: return false
        return runCatching {
            ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY, false)
        }.getOrDefault(false)
    }

    actual fun setDark(value: Boolean) {
        val ctx = AndroidSosContext.appContext ?: return
        runCatching {
            ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY, value).apply()
        }
    }
}
