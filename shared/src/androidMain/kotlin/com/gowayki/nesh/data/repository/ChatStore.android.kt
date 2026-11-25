package com.gowayki.nesh.data.repository

import android.content.Context
import com.gowayki.nesh.app.features.sos.domain.model.AndroidSosContext

actual object ChatStore {
    private const val FILE = "wayki_communication"
    private const val KEY = "chat_state"

    private fun clave(userId: String) = "${KEY}_${userId.ifBlank { "anon" }}"

    actual fun read(userId: String): String? = runCatching {
        AndroidSosContext.appContext
            ?.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            ?.getString(clave(userId), null)
    }.getOrNull()

    actual fun write(userId: String, value: String) {
        runCatching {
            AndroidSosContext.appContext
                ?.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(clave(userId), value)
                ?.apply()
        }
    }
}
