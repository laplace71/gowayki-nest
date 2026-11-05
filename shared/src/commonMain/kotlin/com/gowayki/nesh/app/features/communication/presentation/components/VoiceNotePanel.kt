// Interfaz de grabación de notas de voz, estilo Material Design 3.
// Se usa en la ventana emergente (SOS/ESP32 y Comunicación) y en la barra
// del chat. Reemplaza el diseño anterior (botones planos y texto suelto) por
// una tarjeta M3: cabecera con icono, onda animada, cronómetro, progreso real
// de reproducción y acciones circulares para detener, reproducir, enviar y
// descartar.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.communication.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.AlertTriangle
import compose.icons.feathericons.Mic
import compose.icons.feathericons.Pause
import compose.icons.feathericons.Play
import compose.icons.feathericons.Send
import compose.icons.feathericons.Square
import compose.icons.feathericons.Trash2
import kotlin.math.PI
import kotlin.math.sin

/** Estado visual de la nota de voz. */
enum class VoiceNoteMode { RECORDING, PREVIEW, PLAYING, ERROR }

/**
 * Tarjeta M3 con toda la interacción de una nota de voz.
 *
 * @param elapsedMillis cronómetro de grabación (modo RECORDING).
 * @param totalMillis duración de la grabación lista para enviar.
 * @param positionMillis avance real de la reproducción.
 * @param compact true para la barra inferior del chat (menos aire).
 */
@Composable
fun VoiceNotePanel(
    mode: VoiceNoteMode,
    statusText: String,
    elapsedMillis: Long = 0L,
    totalMillis: Long = 0L,
    positionMillis: Long = 0L,
    error: String? = null,
    kicker: String = "NOTA DE VOZ",
    onCancel: () -> Unit = {},
    onStop: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onSend: () -> Unit = {},
    onDiscard: () -> Unit = {},
    onDismissError: () -> Unit = {},
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val accent = if (mode == VoiceNoteMode.ERROR) AppColors.error else AppColors.primary
    val isRecording = mode == VoiceNoteMode.RECORDING
    val isPlaying = mode == VoiceNoteMode.PLAYING
    val hasRecording = mode == VoiceNoteMode.PREVIEW || mode == VoiceNoteMode.PLAYING

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(if (compact) 24.dp else 28.dp),
        color = AppColors.surface,
        contentColor = AppColors.onSurface,
        tonalElevation = if (compact) 2.dp else 6.dp,
        shadowElevation = if (compact) 0.dp else 14.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (compact) 14.dp else 20.dp,
                    vertical = if (compact) 12.dp else 20.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp),
        ) {
            // ── Cabecera ────────────────────────────────────────────────
            if (!compact) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(accent.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = FeatherIcons.Mic,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = kicker,
                            style = AppTextStyles.waykiMini.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            ),
                            color = AppColors.slate,
                        )
                        Text(
                            text = statusText,
                            style = AppTextStyles.waykiLabelMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = AppColors.ink,
                            maxLines = 2,
                        )
                    }
                    if (isRecording) RecordingPulseDot()
                }
            } else {
                Text(
                    text = statusText,
                    style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.ink,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }

            // ── Onda + tiempo / progreso ────────────────────────────────
            VoiceWaveform(
                active = isRecording || isPlaying,
                color = if (isRecording) AppColors.error else accent,
                height = if (compact) 24.dp else 40.dp,
                modifier = Modifier.fillMaxWidth(),
            )

            if (isRecording) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Grabando",
                        style = AppTextStyles.waykiMini,
                        color = AppColors.slate,
                    )
                    Text(
                        text = formatVoiceTime(elapsedMillis),
                        style = AppTextStyles.waykiLabelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                        ),
                        color = AppColors.error,
                    )
                }
            } else if (hasRecording) {
                val total = totalMillis
                val position = if (isPlaying) positionMillis.coerceAtMost(total.coerceAtLeast(1L)) else 0L
                val progress = if (total > 0L) {
                    (position.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = accent,
                        trackColor = accent.copy(alpha = 0.18f),
                        drawStopIndicator = {},
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = formatVoiceTime(position),
                            style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Medium),
                            color = AppColors.slate,
                        )
                        Text(
                            text = formatVoiceTime(total),
                            style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.ink,
                        )
                    }
                }
            }

            // ── Acciones ────────────────────────────────────────────────
            when (mode) {
                VoiceNoteMode.RECORDING -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoundAction(
                        icon = FeatherIcons.Trash2,
                        description = "Cancelar grabación",
                        background = AppColors.error.copy(alpha = 0.12f),
                        tint = AppColors.error,
                        onClick = onCancel,
                    )
                    PrimaryAction(
                        text = "Detener y guardar",
                        icon = FeatherIcons.Square,
                        modifier = Modifier.weight(1f),
                        onClick = onStop,
                    )
                }

                VoiceNoteMode.PREVIEW -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoundAction(
                        icon = FeatherIcons.Play,
                        description = "Reproducir nota de voz",
                        background = AppColors.primary.copy(alpha = 0.14f),
                        tint = AppColors.primary,
                        onClick = onPlayPause,
                    )
                    PrimaryAction(
                        text = "Enviar",
                        icon = FeatherIcons.Send,
                        modifier = Modifier.weight(1f),
                        onClick = onSend,
                    )
                    RoundAction(
                        icon = FeatherIcons.Trash2,
                        description = "Descartar nota de voz",
                        background = AppColors.error.copy(alpha = 0.12f),
                        tint = AppColors.error,
                        onClick = onDiscard,
                    )
                }

                VoiceNoteMode.PLAYING -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoundAction(
                        icon = FeatherIcons.Pause,
                        description = "Pausar reproducción",
                        background = AppColors.primary.copy(alpha = 0.14f),
                        tint = AppColors.primary,
                        onClick = onPlayPause,
                    )
                    PrimaryAction(
                        text = "Enviar",
                        icon = FeatherIcons.Send,
                        modifier = Modifier.weight(1f),
                        onClick = onSend,
                    )
                    RoundAction(
                        icon = FeatherIcons.Trash2,
                        description = "Descartar nota de voz",
                        background = AppColors.error.copy(alpha = 0.12f),
                        tint = AppColors.error,
                        onClick = onDiscard,
                    )
                }

                VoiceNoteMode.ERROR -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = FeatherIcons.AlertTriangle,
                        contentDescription = null,
                        tint = AppColors.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = error ?: "No se pudo grabar el audio.",
                        style = AppTextStyles.waykiMini,
                        color = AppColors.error,
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryAction(
                        text = "Entendido",
                        icon = null,
                        onClick = onDismissError,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.primary,
            contentColor = AppColors.onPrimary,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = AppTextStyles.waykiMini.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun RoundAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    background: Color,
    tint: Color,
    onClick: () -> Unit,
) {
    OutlinedIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
        colors = androidx.compose.material3.IconButtonDefaults.outlinedIconButtonColors(
            containerColor = background,
            contentColor = tint,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Punto rojo que late mientras se graba. */
@Composable
private fun RecordingPulseDot() {
    val transition = rememberInfiniteTransition(label = "voiceRecordingDot")
    val scale by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "voiceRecordingDotScale",
    )
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(AppColors.error.copy(alpha = if (scale > 1f) 1f else 0.55f)),
    )
}

/**
 * Onda de voz dibujada: barras redondeadas con una fase que viaja.
 * Se detiene (altura mínima) cuando no hay grabación ni reproducción.
 */
@Composable
private fun VoiceWaveform(
    active: Boolean,
    color: Color,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "voiceWave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "voiceWavePhase",
    )

    Canvas(modifier = modifier.height(height)) {
        val bars = 28
        val gap = size.width * 0.006f
        val barWidth = (size.width - gap * (bars - 1)) / bars
        val minHeight = size.height * 0.18f
        val maxHeight = size.height * 0.92f

        repeat(bars) { i ->
            val envelope = sin(PI * (i + 1) / (bars + 1)).toFloat().coerceAtLeast(0.15f)
            val wave = if (active) {
                val s = sin(phase + i * 0.55f)
                (0.45f + 0.55f * ((s + 1f) / 2f))
            } else {
                0.35f
            }
            val barHeight = (minHeight + (maxHeight - minHeight) * envelope * wave)
                .coerceIn(minHeight, size.height)
            val x = i * (barWidth + gap)
            val y = (size.height - barHeight) / 2f
            drawRoundRect(
                color = color.copy(alpha = if (active) 0.85f else 0.35f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

/** "0:07" a partir de milisegundos. */
fun formatVoiceTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
