package com.gowayki.nesh.app.features.sign_in.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gowayki.nesh.app.common.modules.auth.AuthCopy
import com.gowayki.nesh.app.common.ui.layouts.AuthLayout
import com.gowayki.nesh.app.di.SupabaseDi
import com.gowayki.nesh.app.features.sign_in.presentation.StateFlow.SignInViewModel
import com.gowayki.nesh.app.features.sign_in.presentation.components.AuthCard
import com.gowayki.nesh.app.features.sign_in.presentation.components.GoogleSignInButton
import com.gowayki.nesh.core.theme.NeshTheme
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composeAuth

@Composable
fun SignInScreen(
    viewModel: SignInViewModel = viewModel { SignInViewModel() },
) {
    val uiState by viewModel.uiState.collectAsState()
    val client = remember { SupabaseDi.client() }

    val googleSignIn = client.composeAuth.rememberSignInWithGoogle(
        onResult = { result ->
            when (result) {
                NativeSignInResult.ClosedByUser -> viewModel.onNativeSignInClosed()
                is NativeSignInResult.Error -> viewModel.onNativeSignInError(result.message)
                is NativeSignInResult.NetworkError -> viewModel.onNativeSignInError(result.message)
                is NativeSignInResult.Success -> {
                    // SessionStatus flow updates UI.
                }
            }
        },
        fallback = {
            viewModel.signInWithGoogleOAuth()
        },
    )

    AuthLayout {
        AuthCard {
            if (uiState.error != null) {
                ErrorBanner(message = uiState.error!!)
                Spacer(modifier = Modifier.height(NeshTheme.spacing.md))
            }

            GoogleSignInButton(
                loading = uiState.isLoading,
                onClick = {
                    viewModel.onSignInStarted()
                    googleSignIn.startFlow()
                },
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = NeshTheme.colors.errorContainer,
                shape = NeshTheme.radius.sm,
            )
            .padding(NeshTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NeshTheme.spacing.sm),
    ) {
        Text(
            text = "!",
            color = NeshTheme.colors.error,
            style = NeshTheme.typography.titleMedium,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = message.ifBlank { AuthCopy.authFailed },
            color = NeshTheme.colors.error,
            style = NeshTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}
