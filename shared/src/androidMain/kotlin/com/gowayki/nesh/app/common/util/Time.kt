package com.gowayki.nesh.app.common.util

import java.time.Instant
import java.time.format.DateTimeFormatter

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun parseIsoToMillis(isoString: String?): Long? {
    if (isoString.isNullOrBlank()) return null
    return runCatching {
        Instant.parse(isoString).toEpochMilli()
    }.recoverCatching {
        DateTimeFormatter.ISO_DATE_TIME.parse(isoString, Instant::from).toEpochMilli()
    }.recoverCatching {
        isoString.toLongOrNull()
    }.getOrNull()
}

