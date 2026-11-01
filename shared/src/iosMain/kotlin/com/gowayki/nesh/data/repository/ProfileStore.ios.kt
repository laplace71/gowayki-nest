package com.gowayki.nesh.data.repository

import platform.Foundation.NSUserDefaults

actual object ProfileStore {
    private const val KEY = "wayki_profile_photo"

    actual fun readPhotoPath(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY)?.takeIf { it.isNotBlank() }

    actual fun writePhotoPath(path: String?) {
        NSUserDefaults.standardUserDefaults.setObject(path ?: "", KEY)
    }
}
