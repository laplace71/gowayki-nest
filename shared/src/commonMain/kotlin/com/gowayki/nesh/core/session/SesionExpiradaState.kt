package com.gowayki.nesh.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Señal global de "esta cuenta abrió sesión en otro dispositivo".
 *
 * El backend incrementa `token_version` en cada login, así que el token de aquí
 * deja de ser válido. El siguiente 401 (REST) enciende esta señal y la
 * navegación vuelve al inicio; mientras tanto el WebSocket deja de reconectarse.
 */
object SesionExpiradaState {

    private val _expirada = MutableStateFlow(false)
    val expirada: StateFlow<Boolean> = _expirada.asStateFlow()

    fun notificar() {
        _expirada.value = true
    }

    fun consumir() {
        _expirada.value = false
    }
}
