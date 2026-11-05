// Puente de plataforma para fotos (perfil y chats).
// Android: galería del sistema + copia al almacenamiento interno de la app.
// iOS: stub por ahora (el avatar cae a las iniciales).
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.common.platform

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Abre la galería del sistema y devuelve la ruta local de la copia.
 * Devuelve null si el usuario cancela o la plataforma no lo soporta.
 */
expect suspend fun pickImageFromGallery(): String?

/** Decodifica una imagen local (foto de perfil o de conversación). */
expect fun loadImageBitmap(path: String?): ImageBitmap?
