package com.gowayki.nesh.app.common.ui.shared

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ToastMessage(val id: Long, val text: String)

/**
 * Cola simple de toasts in-app.
 */
object ToastManager {
    private var counter = 0L
    val messages = mutableStateListOf<ToastMessage>()

    fun show(message: String) {
        messages.add(ToastMessage(++counter, message))
    }

    fun dismiss(id: Long) {
        messages.removeAll { it.id == id }
    }
}

@Composable
fun ToastHost(modifier: Modifier = Modifier) {
    val messages = ToastManager.messages
    if (messages.isNotEmpty()) {
        val current = messages.first()
        androidx.compose.runtime.LaunchedEffect(current.id) {
            kotlinx.coroutines.delay(2800)
            ToastManager.dismiss(current.id)
        }
        androidx.compose.material3.Surface(
            modifier = modifier,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            color = androidx.compose.ui.graphics.Color(0xFF3D4365),
            shadowElevation = 12.dp,
            tonalElevation = 4.dp
        ) {
            androidx.compose.material3.Text(
                text = current.text,
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            )
        }
    }
}
