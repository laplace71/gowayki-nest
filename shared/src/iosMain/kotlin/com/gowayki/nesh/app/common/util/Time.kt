package com.gowayki.nesh.app.common.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.gettimeofday
import platform.posix.timeval

@OptIn(ExperimentalForeignApi::class)
actual fun currentTimeMillis(): Long = memScoped {
    val tv = alloc<timeval>()
    gettimeofday(tv.ptr, null)
    tv.tv_sec * 1000L + tv.tv_usec / 1000L
}

/**
 * Parsea ISO-8601 sin depender de NSISO8601DateFormatter: las opciones de esa
 * API cambiaron entre versiones de Kotlin/Native y rompían la compilación de iOS.
 * Acepta `2026-11-23T13:15:36.020767Z`, offsets `±HH:MM` y epoch en milisegundos.
 */
actual fun parseIsoToMillis(isoString: String?): Long? {
    if (isoString.isNullOrBlank()) return null
    isoString.toLongOrNull()?.let { return it }
    return runCatching { iso8601AMilis(isoString) }.getOrNull()
}

private fun iso8601AMilis(s: String): Long {
    val anio = s.substring(0, 4).toInt()
    val mes = s.substring(5, 7).toInt()
    val dia = s.substring(8, 10).toInt()
    require(s.length >= 19 && (s[10] == 'T' || s[10] == ' '))
    val hora = s.substring(11, 13).toInt()
    val minuto = s.substring(14, 16).toInt()
    val segundo = s.substring(17, 19).toInt()

    var pos = 19
    var milis = 0L
    if (pos < s.length && s[pos] == '.') {
        pos++
        val inicio = pos
        while (pos < s.length && s[pos].isDigit()) pos++
        milis = s.substring(inicio, pos).padEnd(3, '0').take(3).toLong()
    }

    var offsetMinutos = 0L
    if (pos < s.length && (s[pos] == '+' || s[pos] == '-')) {
        val signo = if (s[pos] == '+') 1L else -1L
        offsetMinutos = signo * (
            s.substring(pos + 1, pos + 3).toLong() * 60 +
                s.substring(pos + 4, pos + 6).toLong()
            )
    }

    val dias = diasDesdeCivil(anio, mes, dia)
    return (dias * 86400L + hora * 3600L + minuto * 60L + segundo - offsetMinutos * 60L) * 1000 + milis
}

/** Días desde 1970-01-01 (algoritmo civil de Howard Hinnant). */
private fun diasDesdeCivil(anio: Int, mes: Int, dia: Int): Long {
    val y = anio - if (mes <= 2) 1 else 0
    val era = if (y >= 0) y / 400 else (y - 399) / 400
    val yoe = y - era * 400
    val mp = mes + if (mes > 2) -3 else 9
    val doy = (153 * mp + 2) / 5 + dia - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097L + doe - 719468L
}
