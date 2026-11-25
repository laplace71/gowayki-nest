// Persistencia de la foto de perfil del conductor (ruta local del archivo)
// y del nombre para mostrar (override local del nombre del backend).
// Ambos valores se guardan POR CUENTA: un segundo usuario que inicie sesión en
// el mismo dispositivo no debe heredar la identidad del anterior.
package com.gowayki.nesh.data.repository

import androidx.compose.runtime.mutableStateOf
import com.gowayki.nesh.core.session.UserSession

expect object ProfileStore {
    fun readPhotoPath(userId: String?): String?
    fun writePhotoPath(path: String?, userId: String?)
    fun readDisplayName(userId: String?): String?
    fun writeDisplayName(name: String?, userId: String?)
}

private fun currentKey(): String? = UserSession.currentUserId

/**
 * Estado reactivo de la foto de perfil.
 */
object ProfilePhotoState {
    /** Ruta actual de la foto (null = usa las iniciales). */
    val path = mutableStateOf<String?>(null)

    init { reloadForUser() }

    /** Recarga el valor de la cuenta activa (tras login o logout). */
    fun reloadForUser() {
        path.value = runCatching { ProfileStore.readPhotoPath(currentKey()) }.getOrNull()
    }

    /** Guarda la nueva foto (o la borra con null) y notifica a la UI. */
    fun update(newPath: String?) {
        path.value = newPath
        runCatching { ProfileStore.writePhotoPath(newPath, currentKey()) }
    }
}

/**
 * Nombre para mostrar, que puede sobrescribir el que viene del backend
 * (útil si el backend guardó un correo u otro valor no deseado).
 */
object DisplayNameState {
    val value = mutableStateOf<String?>(null)

    init { reloadForUser() }

    /** Recarga el valor de la cuenta activa (tras login o logout). */
    fun reloadForUser() {
        value.value = runCatching { ProfileStore.readDisplayName(currentKey()) }.getOrNull()
    }

    fun update(newName: String?) {
        val clean = newName?.trim()?.takeIf { it.isNotBlank() }
        value.value = clean
        runCatching { ProfileStore.writeDisplayName(clean, currentKey()) }
    }

    fun clear() {
        value.value = null
        runCatching { ProfileStore.writeDisplayName(null, currentKey()) }
    }
}
