package com.gowayki.nesh.data.repository

import platform.Foundation.NSUserDefaults

actual object ChatStore {
    private const val KEY = "wayki_communication_chat_state"

    private fun clave(userId: String) = "${KEY}_${userId.ifBlank { "anon" }}"

    actual fun read(userId: String): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(clave(userId))

    actual fun write(userId: String, value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, clave(userId))
    }
}
