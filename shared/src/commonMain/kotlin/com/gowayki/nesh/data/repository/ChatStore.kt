package com.gowayki.nesh.data.repository

/** Almacenamiento local persistente para el historial de Comunicación. */
expect object ChatStore {
    fun read(): String?
    fun write(value: String)
}
