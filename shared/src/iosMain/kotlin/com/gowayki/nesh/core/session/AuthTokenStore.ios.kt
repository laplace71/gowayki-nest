package com.gowayki.nesh.core.session

import platform.Foundation.NSUserDefaults

/**
 * En iOS el token NO se escribe en NSUserDefaults: ese almacenamiento es
 * legible por backups y por cualquier código con acceso al contenedor de la
 * app. Hasta integrar Keychain con interop nativo, el bearer token vive solo
 * en memoria, así que cerrar la app obliga a volver a entrar.
 */
actual object AuthTokenStore {
    private var token: String? = null
    private const val LEGACY_KEY = "wayki_session_auth_token"

    init {
        // Instalaciones anteriores guardaban el JWT en texto plano: se elimina
        // al cargar y en cada escritura para que no sobreviva al logout.
        NSUserDefaults.standardUserDefaults.removeObjectForKey(LEGACY_KEY)
    }

    actual fun read(): String? = token?.takeIf { it.isNotBlank() }

    actual fun write(token: String?) {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(LEGACY_KEY)
        this.token = token?.takeIf { it.isNotBlank() }
    }
}
