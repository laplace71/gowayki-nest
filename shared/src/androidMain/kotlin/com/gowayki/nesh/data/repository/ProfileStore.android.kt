package com.gowayki.nesh.data.repository

import android.content.Context
import com.gowayki.nesh.app.features.sos.domain.model.AndroidSosContext

actual object ProfileStore {
    private const val FILE = "wayki_profile"
    private const val KEY = "profile_photo"
    private const val KEY_NAME = "display_name"

    // Claves distintas por cuenta. "anon" se usa antes del login.
    private fun keyFor(userId: String?): String = userId?.takeIf { it.isNotBlank() } ?: "anon"

    actual fun readPhotoPath(userId: String?): String? = runCatching {
        AndroidSosContext.appContext
            ?.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            ?.getString(KEY + "_" + keyFor(userId), null)
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()

    actual fun writePhotoPath(path: String?, userId: String?) {
        runCatching {
            AndroidSosContext.appContext
                ?.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(KEY + "_" + keyFor(userId), path ?: "")
                ?.apply()
        }
    }

    actual fun readDisplayName(userId: String?): String? = runCatching {
        AndroidSosContext.appContext
            ?.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            ?.getString(KEY_NAME + "_" + keyFor(userId), null)
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()

    actual fun writeDisplayName(name: String?, userId: String?) {
        runCatching {
            AndroidSosContext.appContext
                ?.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(KEY_NAME + "_" + keyFor(userId), name ?: "")
                ?.apply()
        }
    }
}
