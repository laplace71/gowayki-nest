package com.gowayki.nesh.data.repository

/**
 * Almacenamiento local persistente para el historial de Comunicación.
 *
 * La clave incluye el usuario: así una cuenta nunca hereda las conversaciones
 * de otra en el mismo dispositivo.
 */
expect object ChatStore {
    fun read(userId: String): String?
    fun write(userId: String, value: String)
}
