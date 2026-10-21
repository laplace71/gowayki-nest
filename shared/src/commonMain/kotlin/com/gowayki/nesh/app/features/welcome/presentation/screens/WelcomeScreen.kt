// Comentarios en español (Bienvenido, Soporte...).
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.welcome.presentation.screens

// Pantalla "Bienvenido" de Wayki Nest — equivalente al diseño web de app/welcome/page.tsx.
// Colores y tamaños: únicamente desde los tokens de core/theme (AppColors,
// AppSpacing, AppRadius, AppTextStyles).

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gowayki.nesh.app.common.ui.shared.WaykiLink
import com.gowayki.nesh.app.common.ui.shared.WaykiPrimaryButton
import com.gowayki.nesh.app.common.ui.shared.WaykiRoads
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppRadius
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import gowaykinesh.shared.generated.resources.Res
import gowaykinesh.shared.generated.resources.logo
import org.jetbrains.compose.resources.painterResource

// ---------- Logo (Downloads/logo.png, 1254x1254 con fondo transparente) ----------
// El PNG es azul oscuro (~#23274C): se tiñe al fondo claro para que se vea
// sobre la tarjeta hero oscura. Si lo quieres en su color original,
// quita el parámetro colorFilter.
@Composable
private fun BusIcon() {
    androidx.compose.foundation.Image(
        painter = painterResource(Res.drawable.logo),
        contentDescription = "Wayki Nest",
        modifier = Modifier.size(AppSpacing.busIconWidth, AppSpacing.busIconHeight),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(AppColors.cream),
    )
}

@Composable
fun WelcomeScreen(
    onEnterClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
) {
    // ── Animación de entrada: escala + opacidad, como si la app " respirara".
    val appear by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 700, easing = LinearOutSlowInEasing),
        label = "welcomeAppear"
    )

    // Ocupa todo el marco del celular de forma limpia y fluida (optimizado para Helio G99)
    Box(
        Modifier.fillMaxSize().background(AppColors.background).clipToBounds(),
    ) {
        // Rutas animadas optimizadas (hormigas + líneas punteadas)
        WaykiRoads()

        // Fondo limpio y moderno sin sobrecarga de blobs ni bucles infinitos
        // Contenido superior con scroll (hero + textos).
        // Se reserva padding inferior para no quedar bajo los botones anclados.
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(
                    start = AppSpacing.pageHorizontal,
                    end = AppSpacing.pageHorizontal,
                    top = AppSpacing.lg,
                    bottom = AppSpacing.welcomeBottomReserve,
                ),
        ) {
            // Tarjeta hero oscura
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(AppSpacing.heroCardHeight)
                    .clip(AppRadius.hero)
                    .background(AppColors.onBackground)
                    .graphicsLayer {
                        // Entra desde un poco más abajo y con escala menor.
                        scaleX = 0.92f + 0.08f * appear
                        scaleY = 0.92f + 0.08f * appear
                        translationY = 40.dp.toPx() * (1f - appear)
                        alpha = appear
                    },
            ) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = AppSpacing.xxl)) { BusIcon() }
                Text(
                    "Wayki Nest",
                    Modifier.align(Alignment.BottomCenter).padding(bottom = AppSpacing.topSection),
                    color = AppColors.cream,
                    style = AppTextStyles.waykiHeroTitle,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(AppSpacing.lg))

            // Textos de bienvenida
            Text(
                "Bienvenido!",
                color = AppColors.onBackground,
                style = AppTextStyles.waykiTitle,
                modifier = Modifier.graphicsLayer {
                    translationY = 24.dp.toPx() * (1f - appear)
                    alpha = appear
                },
            )
            Text(
                "Empieza con tu ruta...",
                color = AppColors.onSurfaceMuted,
                style = AppTextStyles.waykiSubtitle,
                modifier = Modifier.graphicsLayer {
                    translationY = 24.dp.toPx() * (1f - appear)
                    alpha = appear
                },
            )

            // NOTA: las 2 imágenes "ants" de la web apuntan a URLs temporales de
            // Builder.io que caducan; expórtalas de Figma como PNG a composeResources/drawable
            // (ant_1, ant_2) y colócalas aquí con Image() si las necesitas.
        }

        // Controles anclados abajo, como en el diseño original (y=685/874).
        Column(
            Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    start = AppSpacing.xl,
                    end = AppSpacing.xl,
                    top = AppSpacing.sm,
                    bottom = AppSpacing.md,
                ),
        ) {
            WaykiPrimaryButton(text = "ENTRAR", onClick = onEnterClick)
            WaykiLink(text = "Soporte técnico", onClick = onSupportClick)
        }
    }
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    WelcomeScreen()
}