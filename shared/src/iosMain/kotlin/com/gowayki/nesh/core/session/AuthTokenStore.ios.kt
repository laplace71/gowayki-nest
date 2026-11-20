package com.gowayki.nesh.core.session

import platform.Foundation.NSUserDefaults

actual object AuthTokenStore {
    private const val KEY = "wayki_session_auth_token"

    actual fun read(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY)?.takeIf { it.isNotBlank() }

    actual fun write(token: String?) {
        if (token.isNullOrBlank()) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(token, KEY)
        }
    }
}
