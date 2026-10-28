package com.gowayki.nesh.app.common.ui.shared

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.unit.dp

/**
 * Icono "Recorrido" (route) tomado del diseno de Figma.
 * Se construye como ImageVector a partir del path exacto (trazo) para lograr
 * paridad 1:1 con el diseno sin depender de iconos externos.
 */
internal val WaykiRouteIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "WaykiRoute",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 28f,
        viewportHeight = 28f
    ).addPath(
        pathData = PathData {
            // Circulo inferior izquierdo.
            moveTo(10.5f, 22.167f)
            curveTo(10.5f, 24.1001f, 8.933f, 25.6672f, 7f, 25.6672f)
            curveTo(5.067f, 25.6672f, 3.5f, 24.1001f, 3.5f, 22.167f)
            curveTo(3.5f, 20.2338f, 5.067f, 18.6667f, 7f, 18.6667f)
            curveTo(8.933f, 18.6667f, 10.5f, 20.2338f, 10.5f, 22.167f)
            close()
            // Serpenteo central.
            moveTo(10.5f, 22.167f)
            horizontalLineTo(20.4167f)
            curveTo(21.4996f, 22.167f, 22.5382f, 21.7367f, 23.304f, 20.9709f)
            curveTo(24.0698f, 20.205f, 24.5f, 19.1663f, 24.5f, 18.0833f)
            curveTo(24.5f, 17.0002f, 24.0698f, 15.9615f, 23.304f, 15.1957f)
            curveTo(22.5382f, 14.4299f, 21.4996f, 13.9996f, 20.4167f, 13.9996f)
            horizontalLineTo(7.58333f)
            curveTo(6.50037f, 13.9996f, 5.46175f, 13.5694f, 4.69598f, 12.8036f)
            curveTo(3.93021f, 12.0377f, 3.5f, 10.999f, 3.5f, 9.91597f)
            curveTo(3.5f, 8.83292f, 3.93021f, 7.79422f, 4.69598f, 7.02839f)
            curveTo(5.46175f, 6.26255f, 6.50037f, 5.83231f, 7.58333f, 5.83231f)
            horizontalLineTo(17.5f)
            // Circulo superior derecho.
            moveTo(17.5f, 5.83231f)
            curveTo(17.5f, 7.76546f, 19.067f, 9.33259f, 21f, 9.33259f)
            curveTo(22.933f, 9.33259f, 24.5f, 7.76546f, 24.5f, 5.83231f)
            curveTo(24.5f, 3.89916f, 22.933f, 2.33203f, 21f, 2.33203f)
            curveTo(19.067f, 2.33203f, 17.5f, 3.89916f, 17.5f, 5.83231f)
            close()
        },
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()
}
