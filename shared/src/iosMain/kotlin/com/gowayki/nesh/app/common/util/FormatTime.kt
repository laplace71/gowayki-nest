package com.gowayki.nesh.app.common.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

@OptIn(ExperimentalForeignApi::class)
actual fun formatTime(millis: Long): String {
    if (millis <= 0L) return ""
    val formatter = NSDateFormatter()
    formatter.dateFormat = "HH:mm"
    // millis since epoch → seconds.
    val date = NSDate(millis.toDouble() / 1000.0)
    return formatter.stringFromDate(date)
}