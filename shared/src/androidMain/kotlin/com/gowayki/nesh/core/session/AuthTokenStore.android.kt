package com.gowayki.nesh.core.session

import android.content.Context
import com.gowayki.nesh.app.features.sos.domain.model.AndroidSosContext

actual object AuthTokenStore {
    private const val FILE = "wayki_session"
    private const val KEY = "auth_token"

    actual fun read(): String? = runCatching {
        AndroidSosContext.appContext
            ?.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            ?.getString(KEY, null)
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()

    actual fun write(token: String?) {
        runCatching {
            val prefs = AndroidSosContext.appContext
                ?.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                ?: return
            prefs.edit().apply {
                if (token.isNullOrBlank()) remove(KEY) else putString(KEY, token)
            }.apply()
        }
    }
}
