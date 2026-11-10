// Persistencia de la foto de perfil del conductor (ruta local del archivo)
// y del nombre para mostrar (override local del nombre del backend).
package com.gowayki.nesh.data.repository

import androidx.compose.runtime.mutableStateOf

expect object ProfileStore {
    fun readPhotoPath(): String?
    fun writePhotoPath(path: String?)
    fun readDisplayName(): String?
    fun writeDisplayName(name: String?)
}

/**
 * Estado reactivo de la foto de perfil.
 */
object ProfilePhotoState {
    /** Ruta actual de la foto (null = usa las iniciales). */
    val path = mutableStateOf<String?>(runCatching { ProfileStore.readPhotoPath() }.getOrNull())

    /** Guarda la nueva foto (o la borra con null) y notifica a la UI. */
    fun update(newPath: String?) {
        path.value = newPath
        runCatching { ProfileStore.writePhotoPath(newPath) }
    }
}

/**
 * Nombre para mostrar, que puede sobrescribir el que viene del backend
 * (útil si el backend guardó un correo u otro valor no deseado).
 */
object DisplayNameState {
    val value = mutableStateOf<String?>(runCatching { ProfileStore.readDisplayName() }.getOrNull())

    fun update(newName: String?) {
        val clean = newName?.trim()?.takeIf { it.isNotBlank() }
        value.value = clean
        runCatching { ProfileStore.writeDisplayName(clean) }
    }

    fun clear() {
        value.value = null
        runCatching { ProfileStore.writeDisplayName(null) }
    }
}
