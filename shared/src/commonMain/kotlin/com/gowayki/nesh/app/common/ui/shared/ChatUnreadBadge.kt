package com.gowayki.nesh.app.common.ui.shared

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Total de mensajes no leídos. Lo alimenta el repositorio de chat y lo lee la
 * barra inferior, así el puntito aparece en cualquier módulo sin pasar parámetros.
 */
object ChatUnreadBadge {
    var total by mutableIntStateOf(0)
}
