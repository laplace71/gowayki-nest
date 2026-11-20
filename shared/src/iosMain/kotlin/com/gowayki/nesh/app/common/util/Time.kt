package com.gowayki.nesh.app.common.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSISO8601DateFormatterOptions
import platform.posix.gettimeofday
import platform.posix.timeval

@OptIn(ExperimentalForeignApi::class)
actual fun currentTimeMillis(): Long = memScoped {
    val tv = alloc<timeval>()
    gettimeofday(tv.ptr, null)
    tv.tv_sec * 1000L + tv.tv_usec / 1000L
}

actual fun parseIsoToMillis(isoString: String?): Long? {
    if (isoString.isNullOrBlank()) return null
    isoString.toLongOrNull()?.let { return it }
    return runCatching {
        // El formato por defecto no acepta segundos fraccionarios ("...15.123Z"),
        // que es lo que envía el backend. Probamos primero con fracciones y
        // luego con el formato clásico (y el fallback numérico de arriba).
        val conFracciones = platform.Foundation.NSISO8601DateFormatter().apply {
            formatOptions = NSISO8601DateFormatterOptions.DateTimeWithInternetDateTime or
                NSISO8601DateFormatterOptions.DateTimeWithFractionalSeconds
        }
        val basico = platform.Foundation.NSISO8601DateFormatter()
        val date = conFracciones.dateFromString(isoString)
            ?: basico.dateFromString(isoString)
            ?: return null
        (date.timeIntervalSince1970 * 1000.0).toLong()
    }.getOrNull()
}
