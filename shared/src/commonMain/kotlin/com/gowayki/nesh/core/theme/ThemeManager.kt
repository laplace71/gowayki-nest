package com.gowayki.nesh.core.theme

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

/**
 * Estado global del tema (claro/oscuro).
 *
 * `userPreference` permite forzar un modo desde la UI (Ajustes). Si es null,
 * se respeta el modo del sistema (`systemDark`), que se actualiza desde [App].
 */
object ThemeManager {
    /** Modo oscuro reportado por el sistema. */
    val systemDark = mutableStateOf(false)

    /** Preferencia explícita del usuario (null = seguir al sistema). */
    val userPreference = mutableStateOf<Boolean?>(null)

    /** Modo oscuro efectivo. */
    val isDark = mutableStateOf(false)

    init {
        sync()
    }

    fun sync() {
        isDark.value = userPreference.value ?: systemDark.value
    }

    fun setDark(enabled: Boolean?) {
        userPreference.value = enabled
        sync()
    }
}
