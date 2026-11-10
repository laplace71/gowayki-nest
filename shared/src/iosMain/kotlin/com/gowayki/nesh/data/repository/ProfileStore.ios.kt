package com.gowayki.nesh.data.repository

import platform.Foundation.NSUserDefaults

actual object ProfileStore {
    private const val KEY = "wayki_profile_photo"
    private const val KEY_NAME = "wayki_profile_display_name"

    actual fun readPhotoPath(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY)?.takeIf { it.isNotBlank() }

    actual fun writePhotoPath(path: String?) {
        NSUserDefaults.standardUserDefaults.setObject(path ?: "", KEY)
    }

    actual fun readDisplayName(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_NAME)?.takeIf { it.isNotBlank() }

    actual fun writeDisplayName(name: String?) {
        NSUserDefaults.standardUserDefaults.setObject(name ?: "", KEY_NAME)
    }
}
