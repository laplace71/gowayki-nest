package com.gowayki.nesh.data.repository

import platform.Foundation.NSUserDefaults

actual object ProfileStore {
    private const val KEY = "wayki_profile_photo"
    private const val KEY_NAME = "wayki_profile_display_name"

    private fun keyFor(userId: String?): String = userId?.takeIf { it.isNotBlank() } ?: "anon"

    actual fun readPhotoPath(userId: String?): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY + "_" + keyFor(userId))?.takeIf { it.isNotBlank() }

    actual fun writePhotoPath(path: String?, userId: String?) {
        NSUserDefaults.standardUserDefaults.setObject(path ?: "", KEY + "_" + keyFor(userId))
    }

    actual fun readDisplayName(userId: String?): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_NAME + "_" + keyFor(userId))?.takeIf { it.isNotBlank() }

    actual fun writeDisplayName(name: String?, userId: String?) {
        NSUserDefaults.standardUserDefaults.setObject(name ?: "", KEY_NAME + "_" + keyFor(userId))
    }
}
