package com.gowayki.nesh.core.theme.src.core

import androidx.compose.ui.graphics.Color

/**
 * 🎨 Paleta de colores de Gowayki Nesh — "Puya" Edition.
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  Violeta Puya   #7B4FA0 · Primario                          │
 * │  Jade Andino    #2A8F6F · Secundario                        │
 * │  Malva          #6B5580 · Apoyo                             │
 * │  Crema Adobe    #EDE4D3 · On-dark (compartida Culpeo)       │
 * │  Obsidiana      #130F1E · Fondo oscuro                      │
 * │  Ocre Claro     #F0D08A · Acento (compartido Culpeo)        │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Familia Gowayki: comparte Crema Adobe y Ocre Claro con Culpeo.
 * Fuente tipográfica: Nunito
 */
object AppColors {

    // ── Marca · Violeta Puya ─────────────────────────────────────────────────

    val primary             = Color(0xFF7B4FA0)
    val primaryLight        = Color(0xFF9B70C0)
    val primaryDark         = Color(0xFF542E78)
    val primaryContainer    = Color(0xFF2E1040)
    val onPrimaryContainer  = Color(0xFFD4AEFF)
    val onPrimary           = Color(0xFFFFFFFF)

    // ── Secundario · Jade Andino ──────────────────────────────────────────────

    val secondary             = Color(0xFF2A8F6F)
    val secondaryLight        = Color(0xFF4AAF90)
    val secondaryDark         = Color(0xFF165A44)
    val secondaryContainer    = Color(0xFF0A2A1E)
    val onSecondaryContainer  = Color(0xFF8ADAC0)
    val onSecondary           = Color(0xFFFFFFFF)

    // ── Acento · Ocre Claro (compartido Culpeo) ───────────────────────────────

    val accent             = Color(0xFFF0D08A)
    val accentDark         = Color(0xFF4A3200)
    val onAccent           = Color(0xFF130F1E)

    // ── Apoyo · Malva ─────────────────────────────────────────────────────────

    val support            = Color(0xFF6B5580)
    val supportLight       = Color(0xFF8A74A0)
    val supportDark        = Color(0xFF4A3560)

    // ── Neutros (tema oscuro) ─────────────────────────────────────────────────

    val background         = Color(0xFF130F1E)
    val surface            = Color(0xFF1A1428)
    val surfaceVariant     = Color(0xFF221A35)
    val surfaceHigh        = Color(0xFF2C2245)
    val outline            = Color(0xFF3A2A50)
    val outlineVariant     = Color(0xFF221A35)

    val onBackground       = Color(0xFFEDE4D3)   // Crema Adobe
    val onSurface          = Color(0xFFEDE4D3)
    val onSurfaceMuted     = Color(0xFF9A8AAA)

    // ── Semánticos ────────────────────────────────────────────────────────────

    val success            = Color(0xFF4AAF90)
    val successLight       = Color(0xFF70CFAF)
    val successContainer   = Color(0xFF0A2A1E)
    val onSuccess          = Color(0xFFFFFFFF)

    val error              = Color(0xFFCF6679)
    val errorLight         = Color(0xFFE89AA8)
    val errorContainer     = Color(0xFF3A1A20)
    val onError            = Color(0xFFFFFFFF)

    val warning            = Color(0xFFD9A441)
    val warningContainer   = Color(0xFF3A2A00)
    val onWarning          = Color(0xFF130F1E)

    val info               = Color(0xFF9B70C0)
    val infoContainer      = Color(0xFF2E1040)
    val onInfo             = Color(0xFFFFFFFF)

    // ── Mapa / Nesh UI ────────────────────────────────────────────────────────

    val mapOverlay         = Color(0xCC1A1428)
    val routePrimary       = Color(0xFF9B70C0)
    val routeSecondary     = Color(0xFF4AAF90)
    val routeTertiary      = Color(0xFFF0D08A)
    val busStop            = Color(0xFFF0D08A)
    val busStopActive      = Color(0xFF7B4FA0)
    val userLocation       = Color(0xFF4AAF90)

    // ── Utilitarios ───────────────────────────────────────────────────────────

    val transparent        = Color(0x00000000)
    val scrim              = Color(0x99000000)
    val divider            = Color(0xFF221A35)
}
