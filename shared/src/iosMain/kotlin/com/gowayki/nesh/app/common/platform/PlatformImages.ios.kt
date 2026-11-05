// Stub iOS: la galería y la decodificación de fotos quedan pendientes.
// El avatar cae a las iniciales para no romper la UI.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.common.platform

import androidx.compose.ui.graphics.ImageBitmap

actual suspend fun pickImageFromGallery(): String? = null

actual fun loadImageBitmap(path: String?): ImageBitmap? = null
