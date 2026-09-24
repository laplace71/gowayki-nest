package com.gowayki.nesh.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gowayki.nesh.app.features.sign_in.presentation.StateFlow.AuthViewModel
import com.gowayki.nesh.app.features.sign_in.presentation.screens.LoginScreen
import com.gowayki.nesh.app.features.sign_in.presentation.screens.PinScreen
import com.gowayki.nesh.app.features.sign_in.presentation.screens.RegisterScreen
import com.gowayki.nesh.app.features.welcome.presentation.screens.WelcomeScreen
import com.gowayki.nesh.data.repository.FakeAuthRepository

// FASE JSON/BACKEND: cambia FakeAuthRepository() por el repositorio real.
// Solo se toca esta línea.
@Composable
fun WaykiNav(
    nav: NavHostController = rememberNavController(),
    vm: AuthViewModel = viewModel { AuthViewModel(FakeAuthRepository()) },
) {
    val state = vm.state

    NavHost(navController = nav, startDestination = Routes.WELCOME) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                // Tu flujo: tu LoginScreen ES el sign-in.
                onEnterClick = { nav.navigate(Routes.LOGIN) },
                onSupportClick = { /* TODO: abrir soporte técnico */ },
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                isLoading = state.isLoading,
                error = state.error,
                onLoginClick = { vm.login(it) },
                onRegisterClick = { nav.navigate(Routes.REGISTER) },
                onForgotClick = { /* TODO: recuperar PIN */ },
                onBack = { nav.popBackStack() },
            )
            // El PIN entra directo al home.
            LaunchedEffect(state.user) {
                if (state.user != null) {
                    vm.consumeUser()
                    /* TODO: nav.navigate("home") */
                }
            }
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                isLoading = state.isLoading,
                error = state.error,
                onRegisterClick = { name, dni, phone -> vm.register(name, dni, phone) },
                // TODO Google real: Credential Manager + SHA-1 + Web Client ID + google-services.json
                onGoogleClick = { vm.google("demo-token") },
                onLoginClick = { nav.popBackStack() },
                onBack = { nav.popBackStack() },
            )
            LaunchedEffect(state.user) {
                if (state.user != null) {
                    vm.consumeUser()
                    nav.navigate(Routes.PIN)
                }
            }
        }
        composable(Routes.PIN) {
            PinScreen(
                isLoading = state.isLoading,
                error = state.error,
                onConfirm = { vm.savePin("", it) },
                onBack = { nav.popBackStack() },
            )
            LaunchedEffect(state.user) {
                if (state.user != null) {
                    vm.consumeUser()
                    /* TODO: nav.navigate("home") */
                }
            }
        }
    }
}