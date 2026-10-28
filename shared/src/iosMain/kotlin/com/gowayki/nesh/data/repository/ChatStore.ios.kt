package com.gowayki.nesh.data.repository

import platform.Foundation.NSUserDefaults

actual object ChatStore {
    private const val KEY = "wayki_communication_chat_state"

    actual fun read(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY)

    actual fun write(value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, KEY)
    }
}
