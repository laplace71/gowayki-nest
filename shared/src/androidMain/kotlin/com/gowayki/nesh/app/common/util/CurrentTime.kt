package com.gowayki.nesh.app.common.util

actual fun currentHourMinute(): String {
    val now = java.time.LocalTime.now()
    return String.format("%02d:%02d", now.hour, now.minute)
}