package com.gowayki.nesh.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gowayki.nesh.app.di.AppDi
import com.gowayki.nesh.app.features.reports.presentation.screens.ReportsScreen
import com.gowayki.nesh.app.features.sign_in.presentation.StateFlow.AuthViewModel
import com.gowayki.nesh.app.features.sign_in.presentation.screens.LoginScreen
import com.gowayki.nesh.app.features.sign_in.presentation.screens.PinScreen
import com.gowayki.nesh.app.features.sign_in.presentation.screens.RegisterScreen
import com.gowayki.nesh.app.features.welcome.presentation.screens.WelcomeScreen

@Composable
fun WaykiNav(
    nav: NavHostController = rememberNavController(),
    // Utilizamos el contenedor de dependencias AppDi para obtener el ViewModel
    vm: AuthViewModel = viewModel { AppDi.get<AuthViewModel>() }
) {
    val state = vm.state
    val loginState = vm.loginState

    NavHost(navController = nav, startDestination = Routes.WELCOME) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onEnterClick = { nav.navigate(Routes.LOGIN) },
                onSupportClick = { /* TODO: abrir soporte tcnico */ },
            )
        }
        
        composable(Routes.LOGIN) {
            LoginScreen(
                state = loginState,
                onNumberClick = { vm.onNumberClick(it) },
                onDeleteClick = { vm.onDeleteClick() },
                onForgotPinClick = { /* TODO: Pantalla de Recuperacin de PIN */ },
                onBackClick = { nav.popBackStack() },
                onSupportClick = { /* TODO: Soporte tcnico */ }
            )
            
            // Cuando el mock de login es exitoso, el User se llena en el state principal
            LaunchedEffect(state.user) {
                if (state.user != null) {
                    vm.consumeUser()
                    // Si te logueas con exito, te vas de frente al mapa/reportes
                    nav.navigate(Routes.REPORTS) { popUpTo(Routes.WELCOME) { inclusive = false } }
                }
            }
        }
        
        composable(Routes.REGISTER) {
            RegisterScreen(
                isLoading = state.isLoading,
                error = state.error,
                onRegisterClick = { name, dni, phone -> vm.register(name, dni, phone) },
                onGoogleClick = { vm.google("demo-token") },
                onLoginClick = { nav.popBackStack() },
                onBack = { nav.popBackStack() },
            )
            LaunchedEffect(state.user) {
                if (state.user != null) {
                    vm.consumeUser()
                    nav.navigate(Routes.REPORTS) { popUpTo(Routes.WELCOME) { inclusive = false } }
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
                    nav.navigate(Routes.REPORTS) { popUpTo(Routes.WELCOME) { inclusive = false } }
                }
            }
        }
        
        composable(Routes.REPORTS) {
            ReportsScreen(
                onBack = { nav.popBackStack() },
                onHomeClick = { /* TODO: home conductor */ },
                onSosClick = { /* TODO: SOS */ },
                onMapClick = { /* ya ests en reportes */ },
                onSettingsClick = { /* TODO: ajustes */ },
                onLogoutClick = { 
                    nav.navigate(Routes.WELCOME) { popUpTo(Routes.WELCOME) { inclusive = true } } 
                },
            )
        }
    }
}