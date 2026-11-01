package com.gowayki.nesh.core

import androidx.compose.runtime.mutableStateOf

/**
 * Preferencias globales de la app (notificaciones, etc.).
 */
object AppSettings {
    val notificationsEnabled = mutableStateOf(true)
    val controlPointAlertsEnabled = mutableStateOf(true)
    val timeAlertsEnabled = mutableStateOf(true)
    val vibrationEnabled = mutableStateOf(true)
    val locationEnabled = mutableStateOf(true)
    val language = mutableStateOf("Español")
    val isOnRoute = mutableStateOf(false)
    val detectOtherDrivers = mutableStateOf(true)
    val lunchEnabled = mutableStateOf(true)
    val lunchTime = mutableStateOf("13:00")
    val lunchDurationMinutes = mutableStateOf(30)
    // Hora de inicio del turno del conductor (formato "HH:mm").
    val shiftStartTime = mutableStateOf("05:00")
    // Duración total del turno en minutos (para el % de cumplimiento).
    val shiftDurationMinutes = mutableStateOf(720)
}