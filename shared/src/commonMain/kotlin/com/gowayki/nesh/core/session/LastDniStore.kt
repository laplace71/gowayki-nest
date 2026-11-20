package com.gowayki.nesh.core.session

/**
 * Último DNI con el que se inició sesión o se registró una cuenta.
 *
 * Se guarda entre arranques para que el login no vuelva siempre al DNI de
 * prueba cuando el chofer cierra y abre la app.
 */
expect object LastDniStore {
    fun read(): String?
    fun write(dni: String?)
}
