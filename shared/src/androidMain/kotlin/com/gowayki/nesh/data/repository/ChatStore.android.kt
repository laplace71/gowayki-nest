package com.gowayki.nesh.data.repository

import android.content.Context
import com.gowayki.nesh.app.features.sos.domain.model.AndroidSosContext

actual object ChatStore {
    private const val FILE = "wayki_communication"
    private const val KEY = "chat_state"

    actual fun read(): String? = runCatching {
        AndroidSosContext.appContext
            ?.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            ?.getString(KEY, null)
    }.getOrNull()

    actual fun write(value: String) {
        runCatching {
            AndroidSosContext.appContext
                ?.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(KEY, value)
                ?.apply()
        }
    }
}
