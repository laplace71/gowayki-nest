package com.gowayki.nesh.app.common.util

actual fun formatTime(millis: Long): String {
    if (millis <= 0L) return ""
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(millis))
}