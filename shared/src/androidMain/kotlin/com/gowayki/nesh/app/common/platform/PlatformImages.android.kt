// Implementación Android: selector de imágenes + copia al filesDir de la app.
// La Activity se registra desde MainActivity y el resultado se reenvía desde
// onActivityResult con [handleImagePickResult].
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.common.platform

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

/** Código de request para el selector de imágenes (lo usa MainActivity). */
const val REQUEST_CODE_PICK_IMAGE = 9101

/** Registro de la Activity actual y de la selección en curso. */
object AndroidImagePicker {
    @Volatile
    var activity: WeakReference<Activity>? = null

    internal var pending: CancellableContinuation<Uri?>? = null
}

actual suspend fun pickImageFromGallery(): String? {
    val activity = AndroidImagePicker.activity?.get() ?: return null
    val uri = suspendCancellableCoroutine<Uri?> { cont ->
        AndroidImagePicker.pending = cont
        cont.invokeOnCancellation { AndroidImagePicker.pending = null }
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        activity.startActivityForResult(
            Intent.createChooser(intent, "Elige una foto"),
            REQUEST_CODE_PICK_IMAGE,
        )
    } ?: return null
    return copyToInternal(activity, uri)
}

/** Reenvía el resultado de la galería a la corrutina que la abrió. */
fun handleImagePickResult(resultCode: Int, data: Intent?) {
    val cont = AndroidImagePicker.pending
    AndroidImagePicker.pending = null
    val uri = if (resultCode == Activity.RESULT_OK) data?.data else null
    cont?.resume(uri)
}

private fun copyToInternal(activity: Activity, uri: Uri): String? = runCatching {
    val dir = File(activity.filesDir, "photos").apply { mkdirs() }
    val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
    activity.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    } ?: return null
    file.absolutePath
}.getOrNull()

actual fun loadImageBitmap(path: String?): ImageBitmap? {
    if (path.isNullOrBlank()) return null
    return runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
}
