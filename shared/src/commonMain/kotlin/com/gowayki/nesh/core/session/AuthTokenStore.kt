// Guarda el JWT que devuelve el login. El chat lo manda en cada petición y en
// el handshake del WebSocket; sin él el backend responde 401.
package com.gowayki.nesh.core.session

import kotlin.concurrent.Volatile

expect object AuthTokenStore {
    fun read(): String?
    fun write(token: String?)
}

/**
 * Token de la sesión en memoria, respaldado por el almacenamiento del sistema.
 *
 * Se lee una sola vez al arrancar. `UserSession.clear()` lo borra junto con el
 * usuario, para que cerrar sesión deje de dar acceso al chat.
 */
object AuthTokenState {

    @Volatile
    var token: String? = runCatching { AuthTokenStore.read()?.takeIf { it.isNotBlank() } }.getOrNull()
        private set

    /** Cabecera lista para usar, o null si no hay sesión. */
    val authorizationHeader: String?
        get() = token?.let { "Bearer $it" }

    fun save(nuevo: String?) {
        val limpio = nuevo?.trim()?.takeIf { it.isNotBlank() }
        token = limpio
        runCatching { AuthTokenStore.write(limpio) }
    }

    fun clear() {
        save(null)
    }
}
