package com.gowayki.nesh.app.common.ui.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gowayki.nesh.app.common.modules.auth.AuthCopy
import com.gowayki.nesh.core.theme.NeshTheme

/**
 * Layout de autenticación (fuera del shell).
 *
 * Wide: hero de marca + formulario.
 * Compacto: solo formulario centrado.
 */
@Composable
fun AuthLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(NeshTheme.colors.background)
            .safeContentPadding(),
    ) {
        val wide = maxWidth >= NeshTheme.breakpoints.medium

        if (wide) {
            Row(modifier = Modifier.fillMaxSize()) {
                AuthBrandHero(
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight(),
                )
                AuthFormPane(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight(),
                    content = content,
                )
            }
        } else {
            AuthFormPane(
                modifier = Modifier.fillMaxSize(),
                content = content,
            )
        }
    }
}

@Composable
private fun AuthBrandHero(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        NeshTheme.colors.background,
                        NeshTheme.colors.surface,
                        NeshTheme.colors.primaryContainer,
                    ),
                ),
            )
            .padding(NeshTheme.spacing.xxl),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = AuthCopy.systemName,
                style = NeshTheme.typography.headlineSmall,
                color = NeshTheme.colors.onBackground,
                fontWeight = FontWeight.Bold,
            )

            Column {
                Text(
                    text = "Centro de operaciones",
                    style = NeshTheme.typography.headlineMedium,
                    color = NeshTheme.colors.onBackground,
                    fontWeight = FontWeight.Black,
                )
                Spacer(modifier = Modifier.height(NeshTheme.spacing.md))
                Text(
                    text = AuthCopy.brandTagline,
                    style = NeshTheme.typography.bodyMedium,
                    color = NeshTheme.colors.onSurfaceMuted,
                )
            }

            Text(
                text = "GoWayki MaaS",
                style = NeshTheme.typography.labelSmall,
                color = NeshTheme.colors.onSurfaceMuted,
            )
        }
    }
}

@Composable
private fun AuthFormPane(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.padding(
            horizontal = NeshTheme.spacing.xl,
            vertical = NeshTheme.spacing.lg,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
        Text(
            text = AuthCopy.legalNotice,
            style = NeshTheme.typography.labelSmall,
            color = NeshTheme.colors.onSurfaceMuted,
        )
    }
}
