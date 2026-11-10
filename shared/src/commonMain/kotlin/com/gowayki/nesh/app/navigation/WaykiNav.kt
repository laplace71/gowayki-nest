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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.savedstate.read
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gowayki.nesh.app.di.AppDi
import com.gowayki.nesh.core.session.UserSession
import com.gowayki.nesh.app.di.ChatDi
import com.gowayki.nesh.app.features.communication.domain.repository.ChatRepository
import com.gowayki.nesh.app.features.communication.presentation.chat.ChatScreen
import com.gowayki.nesh.app.features.communication.presentation.chat.NewGroupViewModel
import com.gowayki.nesh.app.features.communication.presentation.chat.NewGroupScreen
import com.gowayki.nesh.app.features.communication.presentation.chat.CommunicationVoiceViewModel
import com.gowayki.nesh.app.features.communication.presentation.chat.CommunicationVoicePopup
import com.gowayki.nesh.app.features.reports.presentation.screens.ReportsScreen
import com.gowayki.nesh.app.features.sign_in.presentation.StateFlow.AuthViewModel
import com.gowayki.nesh.app.features.sign_in.presentation.screens.LoginScreen
import com.gowayki.nesh.app.features.sos.presentation.StateFlow.SosViewModel
import com.gowayki.nesh.app.features.sos.presentation.components.SosAudioPopup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.gowayki.nesh.app.features.sign_in.presentation.screens.PinScreen
import com.gowayki.nesh.app.common.ui.shared.ToastManager
import com.gowayki.nesh.app.features.sign_in.presentation.screens.RegisterScreen
import com.gowayki.nesh.app.features.welcome.presentation.screens.WelcomeScreen
import kotlinx.coroutines.launch

@Composable
fun WaykiNav(
    nav: NavHostController = rememberNavController(),
    // Utilizamos el contenedor de dependencias AppDi para obtener el ViewModel
    vm: AuthViewModel = viewModel { AppDi.get<AuthViewModel>() }
) {
    val state = vm.state
    val loginState = vm.loginState
    // Instancia raíz compartida entre módulos: BLE/GPS no se pierden al navegar.
    val sosVm: SosViewModel = viewModel {
        SosViewModel(chatRepository = AppDi.getOrNull<ChatRepository>())
    }
    // Instancia raíz para notas de voz de Comunicación.
    val communicationVoiceVm: CommunicationVoiceViewModel = viewModel {
        CommunicationVoiceViewModel(chatRepository = AppDi.getOrNull<ChatRepository>())
    }
    // Triple clic desde cualquier módulo lleva al SOS.
    LaunchedEffect(sosVm.ui.sosTripRequestSeq) {
        if (sosVm.ui.sosTripRequestSeq > 0 &&
            nav.currentBackStackEntry?.destination?.route != Routes.SOS
        ) {
            nav.navigate(Routes.SOS)
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
        navController = nav,
        startDestination = Routes.WELCOME,
        enterTransition = {
            fadeIn(animationSpec = tween(300, easing = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f))) +
                androidx.compose.animation.slideInHorizontally(
                    initialOffsetX = { it / 10 },
                    animationSpec = tween(300, easing = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f))
                )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(220, easing = androidx.compose.animation.core.FastOutLinearInEasing)) +
                androidx.compose.animation.slideOutHorizontally(
                    targetOffsetX = { -it / 10 },
                    animationSpec = tween(220, easing = androidx.compose.animation.core.FastOutLinearInEasing)
                )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300, easing = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f))) +
                androidx.compose.animation.slideInHorizontally(
                    initialOffsetX = { -it / 10 },
                    animationSpec = tween(300, easing = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f))
                )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(220, easing = androidx.compose.animation.core.FastOutLinearInEasing)) +
                androidx.compose.animation.slideOutHorizontally(
                    targetOffsetX = { it / 10 },
                    animationSpec = tween(220, easing = androidx.compose.animation.core.FastOutLinearInEasing)
                )
        }
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onEnterClick = { nav.navigate(Routes.LOGIN) },
                onSupportClick = { /* TODO: abrir soporte tcnico */ },
            )
        }
        
        composable(Routes.LOGIN) {
            LaunchedEffect(Unit) { vm.resetLogin() }
            LoginScreen(
                state = loginState,
                activeDni = vm.activeDni,
                onNumberClick = { vm.onNumberClick(it) },
                onDeleteClick = { vm.onDeleteClick() },
                onDniChange = { vm.updateDni(it) },
                onRegisterClick = {
                    vm.clearAuthError()
                    nav.navigate(Routes.REGISTER)
                },
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
                onRegisterClick = { name, dni, phone, email, pin ->
                    vm.register(name, dni, phone, email, pin)
                },
                onClearError = { vm.clearAuthError() },
                onLoginClick = { nav.popBackStack() },
                onBack = { nav.popBackStack() },
            )
            LaunchedEffect(state.user) {
                if (state.user != null) {
                    vm.consumeUser()
                    ToastManager.show("Registro exitoso. Se detectaron rutas cercanas usando la app.")
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
                onNotificationsClick = { ToastManager.show("No tienes notificaciones nuevas por ahora") },
                onHomeClick = { /* TODO: home conductor */ },
                onSosClick = { /* TODO: SOS */ },
                onMapClick = { /* ya ests en reportes */ },
                onSettingsClick = { /* TODO: ajustes */ },
                onLogoutClick = {
                    UserSession.clear()
                    nav.navigate(Routes.WELCOME) { popUpTo(Routes.WELCOME) { inclusive = true } }
                },
                sosVm = sosVm,
            )
        }
        composable(Routes.HOME) {
            com.gowayki.nesh.app.features.home.presentation.screens.HomeScreen(
                onMenuClick = { /* TODO: abrir menu */ },
                onNotificationsClick = { ToastManager.show("No tienes notificaciones nuevas por ahora") },
                onBack = { nav.popBackStack() },
                onSosClick = { nav.navigate(Routes.SOS) },
                onCommunicationClick = { nav.navigate(Routes.COMMUNICATION) },
                onRecorridoClick = { nav.navigate(Routes.ROUTE_DETAIL) },
                onMapaClick = { nav.navigate(Routes.MAP) },
                onSettingsClick = { nav.navigate(Routes.SETTINGS) },
                onLogoutClick = {
                    UserSession.clear()
                    nav.navigate(Routes.WELCOME) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Routes.COMMUNICATION) {
            val chatRepo = remember { AppDi.get<ChatRepository>() }
            val scope = rememberCoroutineScope()
            com.gowayki.nesh.app.features.communication.presentation.screens.CommunicationScreen(
                onConversationClick = { conversationId ->
                    scope.launch { chatRepo.markAsRead(conversationId) }
                    nav.navigate(Routes.chat(conversationId))
                },
                onNewGroupClick = { nav.navigate(Routes.NEW_GROUP) },
                onNotificationsClick = { ToastManager.show("No tienes notificaciones nuevas por ahora") },
                onClearChat = { id ->
                    scope.launch { chatRepo.clearMessages(id) }
                },
                onDeleteChat = { id ->
                    scope.launch { chatRepo.deleteConversation(id) }
                },
                onDeleteGroup = { id ->
                    scope.launch { chatRepo.deleteGroup(id) }
                },
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
                    UserSession.clear()
                    nav.navigate(Routes.WELCOME) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            Routes.ROUTE_DETAIL,
            enterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(350, easing = androidx.compose.animation.core.FastOutSlowInEasing)) },
            exitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.FastOutLinearInEasing)) },
            popEnterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(350, easing = androidx.compose.animation.core.FastOutSlowInEasing)) },
            popExitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.FastOutLinearInEasing)) }
        ) {
            com.gowayki.nesh.app.features.route_detail.presentation.screens.RouteDetailScreen(
                onBack = { nav.popBackStack() },
                onNotificationsClick = { ToastManager.show("No tienes notificaciones nuevas por ahora") },
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
                    UserSession.clear()
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
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { entry ->
            val conversationId = entry.arguments?.read { getString("conversationId") }.orEmpty()
            val chatVm = remember(conversationId) { ChatDi.chatViewModel(conversationId) }
            val chatRepo = remember { AppDi.get<ChatRepository>() }
            val scope = rememberCoroutineScope()

            // La conversación puede ser nueva (recién creada), así que la
            // buscamos viva en el repositorio en vez de leer un argumento fijo.
            val conversations by remember { chatRepo.conversations() }
                .collectAsState(initial = emptyList())
            val archivedConversations by remember { chatRepo.archived() }
                .collectAsState(initial = emptyList())
            val conversation = conversations.firstOrNull { it.id == conversationId }
                ?: archivedConversations.firstOrNull { it.id == conversationId }
            val isArchived = archivedConversations.any { it.id == conversationId }

            ChatScreen(
                title = conversation?.title ?: "Conversación",
                isGroup = conversation?.isGroup ?: false,
                memberCount = conversation?.members?.size ?: 0,
                messages = chatVm.messages,
                draft = chatVm.draft,
                sending = chatVm.sending,
                onDraftChange = chatVm::onDraftChange,
                onSend = chatVm::send,
                conversationId = conversationId,
                voiceVm = communicationVoiceVm,
                photoPath = conversation?.photoPath,
                onChangePhoto = { path ->
                    scope.launch { chatRepo.updateConversationPhoto(conversationId, path) }
                },
                onDeleteMessage = { messageId ->
                    scope.launch { chatRepo.deleteMessage(conversationId, messageId, false) }
                },
                onDeleteMessageForAll = { messageId ->
                    scope.launch { chatRepo.deleteMessage(conversationId, messageId, true) }
                },
                archived = isArchived,
                onArchive = {
                    scope.launch { chatRepo.archiveConversation(conversationId) }
                    nav.popBackStack()
                },
                onUnarchive = {
                    scope.launch { chatRepo.unarchiveConversation(conversationId) }
                },
                onClearChat = {
                    scope.launch { chatRepo.clearMessages(conversationId) }
                },
                onDeleteChat = {
                    scope.launch { chatRepo.deleteConversation(conversationId) }
                    nav.popBackStack()
                },
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            Routes.NEW_GROUP
        ) {
            val groupVm = viewModel { NewGroupViewModel(AppDi.get<ChatRepository>()) }
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

        composable(Routes.ROUTE_PICKER) {
            com.gowayki.nesh.app.features.map.presentation.screens.RoutePickerScreen(
                onBack = { nav.popBackStack() },
                onRouteSelected = {
                    // Salta directo al mapa, descartando Settings y RoutePicker del backstack.
                    // Si el mapa no está en el backstack (entrada desde Home), navega a él.
                    val popped = nav.popBackStack(Routes.MAP, inclusive = false)
                    if (!popped) nav.navigate(Routes.MAP)
                }
            )
        }

        composable(
            Routes.MAP,
            enterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(350, easing = androidx.compose.animation.core.FastOutSlowInEasing)) },
            exitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.FastOutLinearInEasing)) },
            popEnterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(350, easing = androidx.compose.animation.core.FastOutSlowInEasing)) },
            popExitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.FastOutLinearInEasing)) }
        ) {
            com.gowayki.nesh.app.features.map.presentation.screens.MapScreen(
                onBack = { nav.popBackStack() },
                onHomeClick = {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                },
                onCommunicationClick = { nav.navigate(Routes.COMMUNICATION) },
                onRecorridoClick = { nav.navigate(Routes.ROUTE_DETAIL) },
                onMapaClick = {},
                onSettingsClick = { nav.navigate(Routes.SETTINGS) },
                onLogoutClick = {
                    nav.navigate(Routes.WELCOME) { popUpTo(Routes.WELCOME) { inclusive = true } }
                }
            )
        }

        composable(Routes.SETTINGS) {
            com.gowayki.nesh.app.features.settings.presentation.screens.SettingsScreen(
                onBack = { if (!nav.popBackStack()) nav.navigate(Routes.HOME) },
                onNotificationsClick = { ToastManager.show("No tienes notificaciones nuevas por ahora") },
                onMapaClick = { nav.navigate(Routes.MAP) },
                onRoutePickerClick = { nav.navigate(Routes.ROUTE_PICKER) },
                onProfileClick = { nav.navigate(Routes.PROFILE) },
                sosVm = sosVm,
            )
        }

        composable(Routes.PROFILE) {
            com.gowayki.nesh.app.features.profile.presentation.screens.ProfileScreen(
                onBack = { nav.popBackStack() },
            )
        }
        
        composable(Routes.SOS) {
            com.gowayki.nesh.app.features.sos.presentation.screens.SosScreen(
                onBack = { nav.popBackStack() },
                onMapaClick = { nav.navigate(Routes.MAP) },
                sosVm = sosVm,
            )
        }
    }
    // Ventanas emergentes globales encima de cualquier módulo (fuera del NavHost).
    SosAudioPopup(sosVm)
    CommunicationVoicePopup(communicationVoiceVm)
    }
}
