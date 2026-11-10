package com.gowayki.nesh.data.repository

import android.content.Context
import com.gowayki.nesh.app.features.sos.domain.model.AndroidSosContext

actual object ProfileStore {
    private const val FILE = "wayki_profile"
    private const val KEY = "profile_photo"
    private const val KEY_NAME = "display_name"

    actual fun readPhotoPath(): String? = runCatching {
        AndroidSosContext.appContext
            ?.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            ?.getString(KEY, null)
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()

    actual fun writePhotoPath(path: String?) {
        runCatching {
            AndroidSosContext.appContext
                ?.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(KEY, path ?: "")
                ?.apply()
        }
    }

    actual fun readDisplayName(): String? = runCatching {
        AndroidSosContext.appContext
            ?.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            ?.getString(KEY_NAME, null)
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()

    actual fun writeDisplayName(name: String?) {
        runCatching {
            AndroidSosContext.appContext
                ?.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(KEY_NAME, name ?: "")
                ?.apply()
        }
    }
}
