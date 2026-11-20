package com.gowayki.nesh.core.session

/**
 * En iOS el token NO se escribe en NSUserDefaults: ese almacenamiento es
 * legible por backups y por cualquier código con acceso al contenedor de la
 * app. Hasta integrar Keychain con interop nativo, el bearer token vive solo
 * en memoria, así que cerrar la app obliga a volver a entrar.
 */
actual object AuthTokenStore {
    private var token: String? = null

    actual fun read(): String? = token?.takeIf { it.isNotBlank() }

    actual fun write(token: String?) {
        this.token = token?.takeIf { it.isNotBlank() }
    }
}
