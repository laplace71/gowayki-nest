package com.gowayki.nesh.core.theme.src.core

import androidx.compose.ui.graphics.Color
import com.gowayki.nesh.app.common.ui.shared.BlobLight
import com.gowayki.nesh.app.common.ui.shared.BlobMid
import com.gowayki.nesh.app.common.ui.shared.BlobPink
import com.gowayki.nesh.app.common.ui.shared.Ink
import com.gowayki.nesh.app.common.ui.shared.Lavender
import com.gowayki.nesh.app.common.ui.shared.Muted
import com.gowayki.nesh.app.common.ui.shared.Surface

// Paleta Wayki Nest volcada en la estructura del git: mismos nombres de
// tokens que el KMP, valores de tu marca (no queda nada del morado "Puya").
object AppColors {

    // ── Marca · Lavanda (botones) + Ink (textos/hero) ────────────────────────

    val primary             = Lavender
    val primaryLight        = Lavender
    val primaryDark         = Ink
    val primaryContainer    = BlobLight
    val onPrimaryContainer  = Ink
    val onPrimary           = Ink

    // ── Secundario · Muted ───────────────────────────────────────────────────

    val secondary             = Muted
    val secondaryLight        = BlobMid
    val secondaryDark         = Ink
    val secondaryContainer    = BlobLight
    val onSecondaryContainer  = Ink
    val onSecondary           = Surface

    // ── Acento · BlobPink ────────────────────────────────────────────────────

    val accent             = BlobPink
    val accentDark         = Muted
    val onAccent           = Ink

    // ── Apoyo · Muted ────────────────────────────────────────────────────────

    val support            = Muted
    val supportLight       = BlobMid
    val supportDark        = Ink

    // ── Neutros (tema claro Wayki) ───────────────────────────────────────────

    val background         = Surface
    val surface            = Color.White
    val surfaceVariant     = BlobLight
    val surfaceHigh        = Color.White
    val outline            = Lavender
    val outlineVariant     = BlobLight

    val onBackground       = Ink
    val onSurface          = Ink
    val onSurfaceMuted     = Muted

    // ── Semánticos ───────────────────────────────────────────────────────────

    val success            = Ink
    val successLight       = Muted
    val successContainer   = BlobLight
    val onSuccess          = Surface

    val error              = Color(0xFFBA1A1A)
    val errorLight         = Color(0xFFBA1A1A)
    val errorContainer     = BlobLight
    val onError            = Surface

    val warning            = Muted
    val warningContainer   = BlobLight
    val onWarning          = Ink

    val info               = Lavender
    val infoContainer      = BlobLight
    val onInfo             = Ink

    // ── Mapa / Nesh UI ───────────────────────────────────────────────────────

    val mapOverlay         = Surface.copy(alpha = 0.8f)
    val routePrimary       = Lavender
    val routeSecondary     = Muted
    val routeTertiary      = BlobPink
    val busStop            = BlobPink
    val busStopActive      = Lavender
    val userLocation       = Muted

    // ── Utilitarios ──────────────────────────────────────────────────────────

    val transparent        = Color(0x00000000)
    val scrim              = Color(0x99000000)
    val divider            = Lavender
}