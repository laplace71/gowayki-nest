package com.gowayki.nesh.core.theme.src.core

import androidx.compose.ui.graphics.Color

/**
 * ðŸš¦ Colores de estado semÃ¡nticos â€” adaptados a la paleta Culpeo.
 *
 * Los colores de estado se derivan de la paleta principal
 * para mantener armonÃ­a visual:
 *   - Activo/Ã‰xito: verde ocre cÃ¡lido (no el verde Material genÃ©rico)
 *   - Retrasado:    Ocre Ruta #D9A441
 *   - Cancelado:    Terracota con mayor saturaciÃ³n
 *   - Inactivo:     Gris Roca #55636B
 *
 * Uso â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * ```kotlin
 * Text(text = "En servicio", color = StatusColors.active)
 * Surface(color = StatusColors.delayedBg)
 * ```
 */
object StatusColors {

    // â”€â”€ Estados de servicio â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Servicio activo / en marcha â€” verde cÃ¡lido armonizado. */
    val active        = Color(0xFF6A9E6A)
    val activeBg      = Color(0xFF1E2E1E)
    val onActive      = Color(0xFFFFFFFF)

    /** Servicio inactivo / fuera de servicio â€” Gris Roca. */
    val inactive      = Color(0xFF55636B)
    val inactiveBg    = Color(0xFF252E32)
    val onInactive    = Color(0xFFEDE4D3)

    /** Servicio con retraso â€” Ocre Ruta. */
    val delayed       = Color(0xFFD9A441)
    val delayedBg     = Color(0xFF3A2A00)
    val onDelayed     = Color(0xFF2B2420)

    /** Servicio cancelado â€” Terracota oscura. */
    val cancelled     = Color(0xFFBC5433)
    val cancelledBg   = Color(0xFF3A1A10)
    val onCancelled   = Color(0xFFFFFFFF)

    /** Servicio en mantenimiento â€” azul/gris frÃ­o (contraste con la paleta cÃ¡lida). */
    val maintenance   = Color(0xFF7A9AB0)
    val maintenanceBg = Color(0xFF1A252E)
    val onMaintenance = Color(0xFFFFFFFF)

    // â”€â”€ Niveles de ocupaciÃ³n â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Bus con baja ocupaciÃ³n. */
    val occupancyLow    = Color(0xFF6A9E6A)

    /** Bus con ocupaciÃ³n media â€” Ocre Ruta. */
    val occupancyMedium = Color(0xFFD9A441)

    /** Bus lleno / alta ocupaciÃ³n â€” Terracota. */
    val occupancyHigh   = Color(0xFFBC5433)

    // â”€â”€ Alertas del sistema â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Alerta informativa. */
    val alertInfo       = Color(0xFF7A9AB0)
    val alertInfoBg     = Color(0xFF1A252E)

    /** Alerta de advertencia â€” Ocre Claro. */
    val alertWarning    = Color(0xFFF0D08A)
    val alertWarningBg  = Color(0xFF3A2A00)

    /** Alerta crÃ­tica â€” Terracota. */
    val alertCritical   = Color(0xFFBC5433)
    val alertCriticalBg = Color(0xFF3A1A10)
}

