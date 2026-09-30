package com.gowayki.nesh.core.theme.src

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import gowaykinesh.shared.generated.resources.Res
import gowaykinesh.shared.generated.resources.inter
import gowaykinesh.shared.generated.resources.unbounded
import org.jetbrains.compose.resources.Font

/**
 * Estilos tipograficos de la app.
 *
 * Material 3 completa (Typography por defecto) mas los estilos del flujo Wayki,
 * que cargan las fuentes de composeResources: Inter (texto) y Unbounded
 * (titulos/logos). Archivos: `shared/src/commonMain/composeResources/font/`.
 *
 * Uso
 * ```kotlin
 * Text(text = "Bienvenido", style = AppTextStyles.waykiTitle)
 * // O via MaterialTheme (recomendado):
 * Text(text = "Body", style = MaterialTheme.typography.bodyMedium)
 * ```
 */
object AppTextStyles {

    // --- Familia tipografica (fallback Material) ---

    private val nunito = FontFamily.Default

    // --- Display ---

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

    // --- Headline ---

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

    // --- Title ---

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

    // --- Body ---

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

    // --- Label ---

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

    // --- Integracion con Material 3 ---

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

    // --- Wayki (fuentes variables de composeResources) ---
    // UNICO lugar donde se definen los tamanos de letra del flujo Wayki.
    // Font(FontResource) es @Composable en Compose MP: getters composables.

    private val waykiInter
        @Composable
        get() = FontFamily(
            Font(Res.font.inter, weight = FontWeight.Normal),
            Font(Res.font.inter, weight = FontWeight.Medium),
        )

    private val waykiUnbounded
        @Composable
        get() = FontFamily(
            Font(Res.font.unbounded, weight = FontWeight.Bold),
            Font(Res.font.unbounded, weight = FontWeight.ExtraBold),
        )

    val waykiTitle
        @Composable
        get() = TextStyle(
            fontFamily = waykiUnbounded, fontWeight = FontWeight.Bold,
            fontSize = 38.sp, lineHeight = 46.sp,
        )

    val waykiHeroTitle
        @Composable
        get() = TextStyle(
            fontFamily = waykiUnbounded, fontWeight = FontWeight.ExtraBold,
            fontSize = 34.sp,
        )

    val waykiSubtitle
        @Composable
        get() = TextStyle(
            fontFamily = waykiInter, fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
        )

    val waykiButtonLarge
        @Composable
        get() = TextStyle(
            fontFamily = waykiUnbounded, fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )

    val waykiButtonSmall
        @Composable
        get() = TextStyle(
            fontFamily = waykiUnbounded, fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )

    val waykiGoogle
        @Composable
        get() = TextStyle(
            fontFamily = waykiUnbounded, fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
        )

    val waykiBoxDigit
        @Composable
        get() = TextStyle(
            fontFamily = waykiUnbounded, fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
        )

    val waykiInput
        @Composable
        get() = TextStyle(
            fontFamily = waykiInter, fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
        )

    val waykiLabel
        @Composable
        get() = TextStyle(
            fontFamily = waykiInter, fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
        )

    val waykiLabelMedium
        @Composable
        get() = TextStyle(
            fontFamily = waykiInter, fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )

    val waykiMini
        @Composable
        get() = TextStyle(
            fontFamily = waykiInter, fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
        )
}