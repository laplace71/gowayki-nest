// Persistencia de la foto de perfil del conductor (ruta local del archivo).
package com.gowayki.nesh.data.repository

import androidx.compose.runtime.mutableStateOf

expect object ProfileStore {
    fun readPhotoPath(): String?
    fun writePhotoPath(path: String?)
}

/**
 * Estado reactivo de la foto de perfil.
 *
 * La pantalla Configuración y la de Perfil leen el mismo estado: al cambiar la
 * foto en una, la otra se actualiza sin recargar la app.
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
