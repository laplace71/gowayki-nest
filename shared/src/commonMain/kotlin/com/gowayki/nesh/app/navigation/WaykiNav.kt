package com.gowayki.nesh.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.savedstate.read
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gowayki.nesh.app.di.AppDi
import com.gowayki.nesh.app.di.ChatDi
import com.gowayki.nesh.app.features.communication.domain.repository.ChatRepository
import com.gowayki.nesh.app.features.communication.presentation.chat.ChatScreen
import com.gowayki.nesh.app.features.communication.presentation.chat.ConversationsViewModel
import com.gowayki.nesh.app.features.communication.presentation.chat.NewGroupViewModel
import com.gowayki.nesh.app.features.communication.presentation.chat.NewGroupScreen
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

    NavHost(
        navController = nav,
        startDestination = Routes.WELCOME,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(200)
            ) + fadeIn(animationSpec = tween(200))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(200)
            ) + fadeOut(animationSpec = tween(200))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(200)
            ) + fadeIn(animationSpec = tween(200))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(200)
            ) + fadeOut(animationSpec = tween(200))
        }
    ) {
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
                onForgotPinClick = { nav.navigate(Routes.RECOVER_ACCESS) },
                onBackClick = { nav.popBackStack() },
                onSupportClick = { /* TODO: Soporte técnico */ }
            )
            
            // Cuando el mock de login es exitoso, el User se llena en el state principal
            LaunchedEffect(state.user) {
                if (state.user != null) {
                    vm.consumeUser()
                    // Si te logueas con exito, te vas de frente al mapa/reportes
                    nav.navigate(Routes.HOME) { popUpTo(Routes.WELCOME) { inclusive = false } }
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
                    nav.navigate(Routes.HOME) { popUpTo(Routes.WELCOME) { inclusive = false } }
                }
            }
        }
        
        composable(Routes.RECOVER_ACCESS) {
            com.gowayki.nesh.app.features.sign_in.presentation.screens.RecoverAccessScreen(
                isLoading = state.isLoading,
                onSendCodeClick = { dni -> 
                    // TODO: call view model to send code and then navigate to verification
                    // For now, we simulate a small delay or just go to PIN as a dummy step?
                    // According to user: "que el chofer lo ponga, y de ahi que recien se envie el sms"
                    // Let's add sendRecoveryCode in ViewModel later, for now just call it
                    vm.sendRecoveryCode(dni) 
                },
                onBack = { nav.popBackStack() },
                onSupportClick = { /* TODO: Soporte técnico */ }
            )
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
                    nav.navigate(Routes.HOME) { popUpTo(Routes.WELCOME) { inclusive = false } }
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
        composable(Routes.HOME) {
            com.gowayki.nesh.app.features.home.presentation.screens.HomeScreen(
                onMenuClick = { /* TODO: abrir menu */ },
                onNotificationsClick = { /* TODO: notificaciones */ },
                onBack = { nav.popBackStack() },
                onSosClick = { nav.navigate(Routes.SOS) },
                onCommunicationClick = { nav.navigate(Routes.COMMUNICATION) },
                onRecorridoClick = { nav.navigate(Routes.ROUTE_DETAIL) },
                onMapaClick = { nav.navigate(Routes.MAP) },
                onSettingsClick = { nav.navigate(Routes.SETTINGS) },
                onLogoutClick = {
                    nav.navigate(Routes.WELCOME) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Routes.COMMUNICATION) {
            com.gowayki.nesh.app.features.communication.presentation.screens.CommunicationScreen(
                onConversationClick = { conversationId ->
                    AppDi.get<ConversationsViewModel>().open(conversationId)
                    nav.navigate(Routes.chat(conversationId))
                },
                onNewGroupClick = { nav.navigate(Routes.NEW_GROUP) },
                onClearChat = { id -> AppDi.get<ConversationsViewModel>().clearChat(id) },
                onDeleteChat = { id -> AppDi.get<ConversationsViewModel>().deleteConversation(id) },
                onTabSelected = { tab ->
                    if (tab == 0) {
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    } else if (tab == 2) {
                        nav.navigate(Routes.ROUTE_DETAIL)
                    } else if (tab == 3) {
                        nav.navigate(Routes.MAP)
                    }
                },
                onSosClick = { nav.navigate(Routes.SOS) },
                onInicioClick = {
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onRecorridoClick = { nav.navigate(Routes.ROUTE_DETAIL) },
                onMapaClick = { nav.navigate(Routes.MAP) },
                onSettingsClick = { nav.navigate(Routes.SETTINGS) },
                onLogoutClick = {
                    nav.navigate(Routes.WELCOME) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Routes.ROUTE_DETAIL) {
            com.gowayki.nesh.app.features.route_detail.presentation.screens.RouteDetailScreen(
                onBack = { nav.popBackStack() },
                onSosClick = { nav.navigate(Routes.SOS) },
                onInicioClick = {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                },
                onCommunicationClick = { nav.navigate(Routes.COMMUNICATION) },
                // Ya estamos en Recorrido: no hace falta navegar.
                onRecorridoClick = {},
                onSettingsClick = { nav.navigate(Routes.SETTINGS) },
                onMapaClick = { nav.navigate(Routes.MAP) },
                onLogoutClick = {
                    nav.navigate(Routes.WELCOME) { popUpTo(Routes.WELCOME) { inclusive = true } }
                },
                onTabSelected = { tab ->
                    if (tab == 0) {
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    } else if (tab == 1) {
                        nav.navigate(Routes.COMMUNICATION)
                    } else if (tab == 3) {
                        nav.navigate(Routes.MAP)
                    }
                }
            )
        }
        
        composable(
            // El destino se declara con el placeholder: "chat/{conversationId}".
            // Usar Routes.CHAT ("chat") aquí revienta con IllegalArgumentException.
            Routes.CHAT_PATTERN,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
            enterTransition = { fadeIn(animationSpec = tween(150)) },
            exitTransition = { fadeOut(animationSpec = tween(150)) },
            popEnterTransition = { fadeIn(animationSpec = tween(150)) },
            popExitTransition = { fadeOut(animationSpec = tween(150)) }
        ) { entry ->
            val conversationId = entry.arguments?.read { getString("conversationId") }.orEmpty()
            val chatVm = remember(conversationId) { ChatDi.chatViewModel(conversationId) }

            // La conversación puede ser nueva (recién creada), así que la
            // buscamos viva en el repositorio en vez de leer un argumento fijo.
            val conversations by remember { AppDi.get<ChatRepository>().conversations() }
                .collectAsState(initial = emptyList())
            val conversation = conversations.firstOrNull { it.id == conversationId }

            ChatScreen(
                title = conversation?.title ?: "Conversación",
                isGroup = conversation?.isGroup ?: false,
                memberCount = conversation?.members?.size ?: 0,
                messages = chatVm.messages,
                draft = chatVm.draft,
                sending = chatVm.sending,
                onDraftChange = chatVm::onDraftChange,
                onSend = chatVm::send,
                onVoiceNote = { chatVm.sendVoiceNote(18) },
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            Routes.NEW_GROUP,
            enterTransition = { fadeIn(animationSpec = tween(150)) },
            exitTransition = { fadeOut(animationSpec = tween(150)) },
            popEnterTransition = { fadeIn(animationSpec = tween(150)) },
            popExitTransition = { fadeOut(animationSpec = tween(150)) }
        ) {
            val groupVm = viewModel { AppDi.get<NewGroupViewModel>() }
            NewGroupScreen(
                name = groupVm.name,
                colleagues = groupVm.colleagues,
                selected = groupVm.selected,
                creating = groupVm.creating,
                onNameChange = groupVm::onNameChange,
                onToggle = groupVm::toggle,
                onCreate = {
                    groupVm.create { newId ->
                        // Abre el grupo recién creado y limpia el formulario.
                        nav.navigate(Routes.chat(newId)) {
                            popUpTo(Routes.NEW_GROUP) { inclusive = true }
                        }
                    }
                },
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.MAP) {
            com.gowayki.nesh.app.features.map.presentation.screens.MapScreen(
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            com.gowayki.nesh.app.features.settings.presentation.screens.SettingsScreen(
                onBack = { nav.popBackStack() },
                onMapaClick = { nav.navigate(Routes.MAP) }
            )
        }
        
        composable(Routes.SOS) {
            com.gowayki.nesh.app.features.sos.presentation.screens.SosScreen(
                onBack = { nav.popBackStack() },
                onMapaClick = { nav.navigate(Routes.MAP) }
            )
        }
    }
}
