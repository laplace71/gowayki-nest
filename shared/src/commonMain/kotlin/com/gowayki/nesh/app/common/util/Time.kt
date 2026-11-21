package com.gowayki.nesh.app.common.util

/** Tiempo actual en milisegundos (multiplataforma). */
expect fun currentTimeMillis(): Long

/** Parsea una cadena de fecha ISO-8601 a milisegundos de época. */
expect fun parseIsoToMillis(isoString: String?): Long?

