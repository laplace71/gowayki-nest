package com.gowayki.nesh.app.router

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gowayki.nesh.app.common.modules.auth.AuthCopy
import com.gowayki.nesh.app.features.sign_in.presentation.StateFlow.SignInViewModel
import com.gowayki.nesh.app.features.sign_in.presentation.screens.SignInScreen
import com.gowayki.nesh.app.features.sign_in.presentation.screens.SignUpScreen
import com.gowayki.nesh.core.theme.NeshTheme

/**
 * Host raíz:
 * - sin sesión → SignIn
 * - sesión + needs_registration → SignUp (usr_profiles)
 * - sesión + profile_complete → home
 */
@Composable
fun AppRootHost(
    viewModel: SignInViewModel = viewModel { SignInViewModel() },
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        !uiState.isAuthenticated -> SignInScreen(viewModel = viewModel)

        uiState.isAccountLoading && !uiState.profileComplete && !uiState.needsRegistration -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NeshTheme.colors.background),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = NeshTheme.colors.primary)
            }
        }

        uiState.needsRegistration || !uiState.profileComplete -> {
            SignUpScreen(viewModel = viewModel)
        }

        else -> AuthenticatedHome(
            email = uiState.userEmail,
            displayName = uiState.displayName,
            onSignOut = viewModel::signOut,
        )
    }
}

@Composable
private fun AuthenticatedHome(
    email: String?,
    displayName: String?,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeshTheme.colors.background)
            .safeContentPadding()
            .padding(NeshTheme.spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = AuthCopy.signedInAs,
            style = NeshTheme.typography.headlineSmall,
            color = NeshTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(NeshTheme.spacing.sm))
        Text(
            text = displayName.orEmpty().ifBlank { email.orEmpty().ifBlank { "—" } },
            style = NeshTheme.typography.bodyLarge,
            color = NeshTheme.colors.onSurfaceMuted,
        )
        if (!email.isNullOrBlank() && !displayName.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(NeshTheme.spacing.xs))
            Text(
                text = email,
                style = NeshTheme.typography.bodySmall,
                color = NeshTheme.colors.onSurfaceMuted,
            )
        }
        Spacer(modifier = Modifier.height(NeshTheme.spacing.xl))
        Button(onClick = onSignOut) {
            Text(AuthCopy.signOut)
        }
    }
}
