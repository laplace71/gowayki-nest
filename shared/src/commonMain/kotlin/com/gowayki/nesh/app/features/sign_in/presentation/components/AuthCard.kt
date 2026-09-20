package com.gowayki.nesh.app.features.sign_in.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.gowayki.nesh.app.common.modules.auth.AuthCopy
import com.gowayki.nesh.core.theme.NeshTheme

@Composable
fun AuthCard(
    modifier: Modifier = Modifier,
    title: String = AuthCopy.signInTitle,
    subtitle: String = AuthCopy.signInSubtitle,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = AuthCopy.systemName,
            style = NeshTheme.typography.titleLarge,
            color = NeshTheme.colors.primaryLight,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(NeshTheme.spacing.xl))
        Text(
            text = title,
            style = NeshTheme.typography.headlineSmall,
            color = NeshTheme.colors.onBackground,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(NeshTheme.spacing.sm))
        Text(
            text = subtitle,
            style = NeshTheme.typography.bodyMedium,
            color = NeshTheme.colors.onSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(NeshTheme.spacing.xl))
        content()
        Spacer(modifier = Modifier.height(NeshTheme.spacing.lg))
        Text(
            text = AuthCopy.securityTerms,
            style = NeshTheme.typography.labelSmall,
            color = NeshTheme.colors.onSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
