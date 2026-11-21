package com.gowayki.nesh.core.session

import android.content.Context
import com.gowayki.nesh.app.features.sos.domain.model.AndroidSosContext

actual object LastDniStore {
    private const val FILE = "wayki_session"
    private const val KEY = "last_dni"

    actual fun read(): String? = runCatching {
        AndroidSosContext.appContext
            ?.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            ?.getString(KEY, null)
            ?.takeIf { it.length == 8 }
    }.getOrNull()

    actual fun write(dni: String?) {
        runCatching {
            val prefs = AndroidSosContext.appContext
                ?.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                ?: return
            prefs.edit().apply {
                if (dni.isNullOrBlank()) remove(KEY) else putString(KEY, dni)
            }.apply()
        }
    }
}
