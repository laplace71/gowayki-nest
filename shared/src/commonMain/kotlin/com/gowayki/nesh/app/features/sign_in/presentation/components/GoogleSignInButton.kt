package com.gowayki.nesh.app.features.sign_in.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gowayki.nesh.app.common.modules.auth.AuthCopy
import com.gowayki.nesh.core.theme.NeshTheme
import gowaykinesh.shared.generated.resources.Res
import gowaykinesh.shared.generated.resources.icons_google_logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    text: String = AuthCopy.googleSignIn,
    loadingText: String = AuthCopy.connectingGoogle,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = NeshTheme.radius.button,
        border = BorderStroke(1.dp, NeshTheme.colors.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = NeshTheme.colors.surface,
            contentColor = NeshTheme.colors.onSurface,
            disabledContainerColor = NeshTheme.colors.surfaceVariant,
            disabledContentColor = NeshTheme.colors.onSurfaceMuted,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = NeshTheme.colors.primary,
                )
            } else {
                Image(
                    painter = painterResource(Res.drawable.icons_google_logo),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(NeshTheme.spacing.md))
            Text(
                text = if (loading) loadingText else text,
                style = NeshTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
