// Comentarios en español (Bienvenido, Soporte...).
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.welcome.presentation.screens

// Pantalla "Bienvenido" de Wayki Nest — equivalente al diseño web de app/welcome/page.tsx.
// Fuentes variables (composeResources/font/unbounded.ttf e inter.ttf) con el peso exacto del diseño.

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.ui.shared.BlobLight
import com.gowayki.nesh.app.common.ui.shared.BlobMid
import com.gowayki.nesh.app.common.ui.shared.BlobPink
import com.gowayki.nesh.app.common.ui.shared.FloatingBlob
import com.gowayki.nesh.app.common.ui.shared.Ink
import com.gowayki.nesh.app.common.ui.shared.Inter
import com.gowayki.nesh.app.common.ui.shared.Lavender
import com.gowayki.nesh.app.common.ui.shared.Muted
import com.gowayki.nesh.app.common.ui.shared.Surface
import com.gowayki.nesh.app.common.ui.shared.Unbounded
import com.gowayki.nesh.app.common.ui.shared.WaykiLink
import com.gowayki.nesh.app.common.ui.shared.WaykiPrimaryButton
import com.gowayki.nesh.app.common.ui.shared.WaykiRoads
import gowaykinesh.shared.generated.resources.Res
import gowaykinesh.shared.generated.resources.logo
import org.jetbrains.compose.resources.painterResource

// ---------- Logo (Downloads/logo.png, 1254x1254 con fondo transparente) ----------
// El PNG es azul oscuro (~#23274C): se tiñe a Surface para que se vea
// sobre la tarjeta hero oscura. Si lo quieres en su color original,
// quita el parámetro colorFilter.
@Composable
private fun BusIcon() {
    androidx.compose.foundation.Image(
        painter = painterResource(Res.drawable.logo),
        contentDescription = "Wayki Nest",
        modifier = Modifier.size(150.dp, 160.dp),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(Surface),
    )
}

@Composable
fun WelcomeScreen(
    onEnterClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
) {
    // Ocupa todo el marco del celular: sin marco fijo 402x874 ni padding exterior.
    // El contenido hace scroll si la pantalla es más baja que el diseño.
    Box(
        Modifier.fillMaxSize().background(Surface).clipToBounds(),
    ) {
        // Blobs decorativos animados (fondo, relativos a la pantalla real)
        FloatingBlob((-80).dp, (-60).dp, 260.dp, 240.dp, BlobLight.copy(alpha = 0.7f), durationMillis = 4200, travelX = 18.dp, travelY = 24.dp)
        FloatingBlob(270.dp, (-40).dp, 180.dp, 170.dp, Muted.copy(alpha = 0.5f), durationMillis = 5200, delayMillis = 400, travelX = 22.dp, travelY = 18.dp)
        FloatingBlob(140.dp, (-30).dp, 80.dp, 80.dp, BlobMid.copy(alpha = 0.85f), durationMillis = 3400, delayMillis = 200, travelX = 12.dp, travelY = 16.dp)
        FloatingBlob((-30).dp, 600.dp, 110.dp, 100.dp, BlobPink.copy(alpha = 0.35f), durationMillis = 4800, delayMillis = 600, travelX = 16.dp, travelY = 26.dp)
        // Nuevos: rellenan zonas vacías
        FloatingBlob(300.dp, 180.dp, 64.dp, 64.dp, Lavender.copy(alpha = 0.5f), durationMillis = 3800, delayMillis = 800, travelX = 14.dp, travelY = 18.dp)
        FloatingBlob((-60).dp, 300.dp, 140.dp, 130.dp, BlobMid.copy(alpha = 0.4f), durationMillis = 5600, delayMillis = 300, travelX = 20.dp, travelY = 22.dp)
        FloatingBlob(330.dp, 420.dp, 90.dp, 90.dp, BlobLight.copy(alpha = 0.8f), durationMillis = 4400, delayMillis = 1000, travelX = 16.dp, travelY = 14.dp)
        FloatingBlob(200.dp, 540.dp, 50.dp, 50.dp, Muted.copy(alpha = 0.45f), durationMillis = 3200, delayMillis = 500, travelX = 10.dp, travelY = 14.dp)
        FloatingBlob((-50).dp, 720.dp, 70.dp, 70.dp, Lavender.copy(alpha = 0.35f), durationMillis = 4600, delayMillis = 900, travelX = 14.dp, travelY = 18.dp)

        WaykiRoads()  // las 3 rutas viven en Shared (sección 2)

        // Contenido superior con scroll (hero + textos).
        // Se reserva padding inferior para no quedar bajo los botones anclados.
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 190.dp),
        ) {
            // Tarjeta hero oscura
            Box(
                Modifier.fillMaxWidth().height(310.dp)
                    .clip(RoundedCornerShape(32.dp)).background(Ink),
            ) {
                FloatingBlob(60.dp, (-30).dp, 200.dp, 180.dp, Muted.copy(alpha = 0.3f), durationMillis = 5000, travelX = 12.dp, travelY = 14.dp)
                FloatingBlob(300.dp, 100.dp, 160.dp, 150.dp, Lavender.copy(alpha = 0.18f), durationMillis = 4400, delayMillis = 500, travelX = 10.dp, travelY = 16.dp)
                FloatingBlob(100.dp, 220.dp, 120.dp, 110.dp, BlobPink.copy(alpha = 0.25f), durationMillis = 3800, delayMillis = 250, travelX = 12.dp, travelY = 12.dp)
                FloatingBlob(270.dp, 241.dp, 140.dp, 130.dp, Lavender.copy(alpha = 0.45f), durationMillis = 5400, delayMillis = 700, travelX = 10.dp, travelY = 14.dp)
                // Nuevos dentro del hero
                FloatingBlob(20.dp, 130.dp, 60.dp, 60.dp, Surface.copy(alpha = 0.12f), durationMillis = 3600, delayMillis = 350, travelX = 8.dp, travelY = 10.dp)
                FloatingBlob(230.dp, 30.dp, 44.dp, 44.dp, BlobPink.copy(alpha = 0.4f), durationMillis = 4200, delayMillis = 850, travelX = 8.dp, travelY = 12.dp)
                Box(Modifier.align(Alignment.TopCenter).padding(top = 30.dp)) { BusIcon() }
                Text(
                    "Wayki Nest",
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
                    color = Surface, fontFamily = Unbounded,
                    fontWeight = FontWeight.ExtraBold, fontSize = 34.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Textos de bienvenida
            Text(
                "Bienvenido!", color = Ink, fontFamily = Unbounded,
                fontWeight = FontWeight.Bold, fontSize = 38.sp,
            )
            Text(
                "Empieza con tu ruta...",
                color = Muted, fontFamily = Inter,
                fontWeight = FontWeight.Normal, fontSize = 16.sp,
            )

            // NOTA: las 2 imágenes "ants" de la web apuntan a URLs temporales de
            // Builder.io que caducan; expórtalas de Figma como PNG a composeResources/drawable
            // (ant_1, ant_2) y colócalas aquí con Image() si las necesitas.
        }

        // Controles anclados abajo, como en el diseño original (y=685/874).
        Column(
            Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp),
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