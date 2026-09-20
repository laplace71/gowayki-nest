package com.gowayki.nesh.core.theme.src

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * âœï¸ Estilos tipogrÃ¡ficos â€” Fuente: Nunito (Google Fonts)
 *
 * Â¿Por quÃ© Nunito?
 *   â€¢ Formas redondeadas que armonizan con la calidez de la paleta Culpeo.
 *   â€¢ Muy legible en tamaÃ±os pequeÃ±os para datos de trÃ¡nsito.
 *   â€¢ VersÃ¡til: desde displays hasta labels de 11sp.
 *   â€¢ Personalidad amable y moderna â€” refleja el espÃ­ritu de movilidad.
 *
 * Para cargar la fuente en tu proyecto KMP, agrega los archivos .ttf
 * en `shared/src/commonMain/composeResources/font/` y reemplaza
 * [FontFamily.Default] con:
 * ```kotlin
 * FontFamily(
 *     Font(Res.font.nunito_regular, weight = FontWeight.Normal),
 *     Font(Res.font.nunito_medium,  weight = FontWeight.Medium),
 *     Font(Res.font.nunito_semibold,weight = FontWeight.SemiBold),
 *     Font(Res.font.nunito_bold,    weight = FontWeight.Bold),
 * )
 * ```
 *
 * Descarga: https://fonts.google.com/specimen/Nunito
 *
 * Uso â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * ```kotlin
 * Text(text = "Ruta 42", style = AppTextStyles.titleLarge)
 * // O vÃ­a MaterialTheme (recomendado):
 * Text(text = "Body", style = MaterialTheme.typography.bodyMedium)
 * ```
 */
object AppTextStyles {

    // â”€â”€ Familia tipogrÃ¡fica â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Reemplazar con FontFamily de Nunito cuando se carguen los assets.
    private val nunito = FontFamily.Default

    // â”€â”€ Display â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    val displayLarge = TextStyle(
        fontFamily    = nunito,
        fontWeight    = FontWeight.Normal,
        fontSize      = 57.sp,
        lineHeight    = 64.sp,
        letterSpacing = (-0.25).sp
    )

    val displayMedium = TextStyle(
        fontFamily    = nunito,
        fontWeight    = FontWeight.Normal,
        fontSize      = 45.sp,
        lineHeight    = 52.sp,
        letterSpacing = 0.sp
    )

    val displaySmall = TextStyle(
        fontFamily    = nunito,
        fontWeight    = FontWeight.Normal,
        fontSize      = 36.sp,
        lineHeight    = 44.sp,
        letterSpacing = 0.sp
    )

    // â”€â”€ Headline â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    val headlineLarge = TextStyle(
        fontFamily    = nunito,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 32.sp,
        lineHeight    = 40.sp,
        letterSpacing = 0.sp
    )

    val headlineMedium = TextStyle(
        fontFamily    = nunito,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 28.sp,
        lineHeight    = 36.sp,
        letterSpacing = 0.sp
    )

    val headlineSmall = TextStyle(
        fontFamily    = nunito,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 24.sp,
        lineHeight    = 32.sp,
        letterSpacing = 0.sp
    )

    // â”€â”€ Title â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    val titleLarge = TextStyle(
        fontFamily    = nunito,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 22.sp,
        lineHeight    = 28.sp,
        letterSpacing = 0.sp
    )

    val titleMedium = TextStyle(
        fontFamily    = nunito,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.15.sp
    )

    val titleSmall = TextStyle(
        fontFamily    = nunito,
        fontWeight    = FontWeight.Medium,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.1.sp
    )

    // â”€â”€ Body â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    val bodyLarge = TextStyle(
        fontFamily    = nunito,
        fontWeight    = FontWeight.Normal,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.5.sp
    )

    val bodyMedium = TextStyle(
        fontFamily    = nunito,
        fontWeight    = FontWeight.Normal,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.25.sp
    )

    val bodySmall = TextStyle(
        fontFamily    = nunito,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.4.sp
    )

    // â”€â”€ Label â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    val labelLarge = TextStyle(
        fontFamily    = nunito,
        fontWeight    = FontWeight.Medium,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.1.sp
    )

    val labelMedium = TextStyle(
        fontFamily    = nunito,
        fontWeight    = FontWeight.Medium,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.5.sp
    )

    val labelSmall = TextStyle(
        fontFamily    = nunito,
        fontWeight    = FontWeight.Medium,
        fontSize      = 11.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.5.sp
    )

    // â”€â”€ IntegraciÃ³n con Material 3 â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    val typography = Typography(
        displayLarge   = displayLarge,
        displayMedium  = displayMedium,
        displaySmall   = displaySmall,
        headlineLarge  = headlineLarge,
        headlineMedium = headlineMedium,
        headlineSmall  = headlineSmall,
        titleLarge     = titleLarge,
        titleMedium    = titleMedium,
        titleSmall     = titleSmall,
        bodyLarge      = bodyLarge,
        bodyMedium     = bodyMedium,
        bodySmall      = bodySmall,
        labelLarge     = labelLarge,
        labelMedium    = labelMedium,
        labelSmall     = labelSmall,
    )
}

