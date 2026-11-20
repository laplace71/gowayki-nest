package com.gowayki.nesh.app.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import com.gowayki.nesh.data.repository.KtorChatRepository
import com.gowayki.nesh.app.features.communication.presentation.chat.ChatScreen
import com.gowayki.nesh.app.features.communication.presentation.chat.NewGroupViewModel
import com.gowayki.nesh.app.features.communication.presentation.chat.NewGroupScreen
import com.gowayki.nesh.app.features.communication.presentation.chat.CommunicationVoiceViewModel
import com.gowayki.nesh.app.features.communication.presentation.chat.CommunicationVoicePopup
import com.gowayki.nesh.app.features.map.presentation.SubrouteLabels
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

// Transiciones compartidas del NavHost: un desplazamiento corto con fade
// (nunca la pantalla completa) para que el cambio se sienta ligero y rápido.
private val WaykiEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val WaykiExitEasing = FastOutLinearInEasing

private fun forwardEnter(): EnterTransition =
    fadeIn(tween(300, easing = WaykiEasing)) +
        slideInHorizontally(tween(340, easing = WaykiEasing)) { width -> width / 12 }

private fun forwardExit(): ExitTransition =
    fadeOut(tween(200, easing = WaykiExitEasing)) +
        slideOutHorizontally(tween(240, easing = WaykiExitEasing)) { width -> -width / 18 }

private fun backEnter(): EnterTransition =
    fadeIn(tween(300, easing = WaykiEasing)) +
        slideInHorizontally(tween(340, easing = WaykiEasing)) { width -> -width / 12 }

private fun backExit(): ExitTransition =
    fadeOut(tween(200, easing = WaykiExitEasing)) +
        slideOutHorizontally(tween(240, easing = WaykiExitEasing)) { width -> width / 18 }

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
    // Triple clic desde cualquier módulo lleva al SOS. Se observa en su propio
    // alcance: si se leyera directo aquí, cada cambio de estado del SOS
    // recompondría el NavHost completo (scroll y cambios de módulo con lag).
    SosTripWatcher(sosVm) { secuencia ->
        if (secuencia > 0 && nav.currentBackStackEntry?.destination?.route != Routes.SOS) {
            nav.navigate(Routes.SOS)
        }
    }

    // Navegación entre pestañas: deja Inicio como raíz del back stack.
    // Antes cada toque en la barra inferior apilaba una pantalla nueva
    // (Inicio → Comunicación → Recorrido → Mapa → ...) y el botón Atrás
    // tenía que deshacer todo ese historial.
    fun goTab(route: String) {
        if (nav.currentBackStackEntry?.destination?.route == route) return
        if (route == Routes.HOME) {
            if (!nav.popBackStack(Routes.HOME, inclusive = false)) {
                nav.navigate(Routes.HOME) { launchSingleTop = true }
            }
            return
        }
        nav.navigate(route) {
            popUpTo(Routes.HOME) { inclusive = false }
            launchSingleTop = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
        navController = nav,
        startDestination = Routes.WELCOME,
        enterTransition = { forwardEnter() },
        exitTransition = { forwardExit() },
        popEnterTransition = { backEnter() },
        popExitTransition = { backExit() }
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
                    // Si te logueas con exito, te vas al inicio como raíz (sin volver a bienvenida con atrás).
                    nav.navigate(Routes.HOME) { popUpTo(Routes.WELCOME) { inclusive = true } }
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
                    nav.navigate(Routes.HOME) { popUpTo(Routes.WELCOME) { inclusive = true } }
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
                    nav.navigate(Routes.HOME) { popUpTo(Routes.WELCOME) { inclusive = true } }
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
                onCommunicationClick = { goTab(Routes.COMMUNICATION) },
                onRecorridoClick = { goTab(Routes.ROUTE_DETAIL) },
                onMapaClick = { goTab(Routes.MAP) },
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
                onHideChat = { id ->
                    scope.launch {
                        val result = chatRepo.ocultarConversacion(id)
                        if (result.isFailure) ToastManager.show(result.exceptionOrNull()?.message ?: "No se pudo ocultar el chat")
                    }
                },
                onRestoreChat = { id ->
                    scope.launch {
                        val result = chatRepo.restaurarConversacion(id)
                        if (result.isFailure) ToastManager.show(result.exceptionOrNull()?.message ?: "No se pudo restaurar el chat")
                    }
                },
                onRequestGroupDeletion = { id ->
                    scope.launch {
                        val result = chatRepo.solicitarEliminacionGrupo(id)
                        if (result.isFailure) {
                            ToastManager.show(result.exceptionOrNull()?.message ?: "No se pudo solicitar la eliminación")
                        } else {
                            ToastManager.show("El grupo se eliminará en 72 horas")
                        }
                    }
                },
                onCancelGroupDeletion = { id ->
                    scope.launch {
                        val result = chatRepo.cancelarEliminacionGrupo(id)
                        if (result.isFailure) {
                            ToastManager.show(result.exceptionOrNull()?.message ?: "No se pudo cancelar la eliminación")
                        } else {
                            ToastManager.show("Se canceló la eliminación del grupo")
                        }
                    }
                },
                onLeaveGroup = { id ->
                    scope.launch {
                        val result = chatRepo.leaveGroup(id)
                        if (result.isFailure) {
                            ToastManager.show(result.exceptionOrNull()?.message ?: "No se pudo salir del grupo")
                        }
                    }
                },
                onTabSelected = { tab ->
                    when (tab) {
                        0 -> goTab(Routes.HOME)
                        2 -> goTab(Routes.ROUTE_DETAIL)
                        3 -> goTab(Routes.MAP)
                    }
                },
                onSosClick = { nav.navigate(Routes.SOS) },
                onInicioClick = { goTab(Routes.HOME) },
                onRecorridoClick = { goTab(Routes.ROUTE_DETAIL) },
                onMapaClick = { goTab(Routes.MAP) },
                onSettingsClick = { nav.navigate(Routes.SETTINGS) },
                onLogoutClick = {
                    UserSession.clear()
                    nav.navigate(Routes.WELCOME) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Routes.ROUTE_DETAIL) {
            com.gowayki.nesh.app.features.route_detail.presentation.screens.RouteDetailScreen(
                onBack = { nav.popBackStack() },
                onNotificationsClick = { ToastManager.show("No tienes notificaciones nuevas por ahora") },
                onSosClick = { nav.navigate(Routes.SOS) },
                onInicioClick = { goTab(Routes.HOME) },
                onCommunicationClick = { goTab(Routes.COMMUNICATION) },
                // Ya estamos en Recorrido: no hace falta navegar.
                onRecorridoClick = {},
                onSettingsClick = { nav.navigate(Routes.SETTINGS) },
                onMapaClick = { goTab(Routes.MAP) },
                onLogoutClick = {
                    UserSession.clear()
                    nav.navigate(Routes.WELCOME) { popUpTo(Routes.WELCOME) { inclusive = true } }
                },
                onTabSelected = { tab ->
                    when (tab) {
                        0 -> goTab(Routes.HOME)
                        1 -> goTab(Routes.COMMUNICATION)
                        3 -> goTab(Routes.MAP)
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
            // Carga los conductores disponibles para el botón "Añadir miembros".
            val addMembersVm = remember { NewGroupViewModel(chatRepo) }
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
                memberCount = if (chatVm.participants.isNotEmpty()) chatVm.participants.size else (conversation?.members?.size ?: 0),
                members = chatVm.participants,
                colleagues = addMembersVm.colleagues,
                messages = chatVm.messages,
                draftProvider = { chatVm.draft },
                sendingProvider = { chatVm.sending },
                onDraftChange = chatVm::onDraftChange,
                onSend = chatVm::send,
                conversationId = conversationId,
                voiceVm = communicationVoiceVm,
                photoPath = conversation?.photoPath,
                onChangePhoto = { path ->
                    scope.launch { chatRepo.updateConversationPhoto(conversationId, path) }
                },
                onDeleteMessage = { messageId ->
                    scope.launch {
                        val result = chatRepo.deleteMessage(conversationId, messageId, false)
                        ToastManager.show(
                            if (result.isSuccess) "Mensaje eliminado de tu chat"
                            else "No se pudo eliminar el mensaje"
                        )
                    }
                },
                onDeleteMessageForAll = { messageId ->
                    scope.launch {
                        val result = chatRepo.deleteMessage(conversationId, messageId, true)
                        ToastManager.show(
                            if (result.isSuccess) "Mensaje eliminado de tu chat"
                            else "No se pudo eliminar el mensaje"
                        )
                    }
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
                onHideChat = {
                    scope.launch {
                        val result = chatRepo.ocultarConversacion(conversationId)
                        if (result.isSuccess) {
                            nav.popBackStack()
                        } else {
                            ToastManager.show(result.exceptionOrNull()?.message ?: "No se pudo ocultar el chat")
                        }
                    }
                },
                onLeaveGroup = {
                    scope.launch {
                        val result = chatRepo.leaveGroup(conversationId)
                        if (result.isFailure) {
                            ToastManager.show(result.exceptionOrNull()?.message ?: "No se pudo salir del grupo")
                        }
                    }
                },
                onRequestGroupDeletion = {
                    scope.launch {
                        val result = chatRepo.solicitarEliminacionGrupo(conversationId)
                        if (result.isSuccess) {
                            ToastManager.show("El grupo se eliminará en 72 horas")
                        } else {
                            ToastManager.show(result.exceptionOrNull()?.message ?: "No se pudo solicitar la eliminación")
                        }
                    }
                },
                onCancelGroupDeletion = {
                    scope.launch {
                        val result = chatRepo.cancelarEliminacionGrupo(conversationId)
                        if (result.isSuccess) {
                            ToastManager.show("Se canceló la eliminación del grupo")
                        } else {
                            ToastManager.show(result.exceptionOrNull()?.message ?: "No se pudo cancelar la eliminación")
                        }
                    }
                },
                onAddMembers = { nuevos ->
                    scope.launch {
                        val result = chatRepo.addMembers(conversationId, nuevos)
                        if (result.isSuccess) {
                            chatVm.loadParticipants()
                            ToastManager.show("Se añadieron ${nuevos.size} miembro${if (nuevos.size == 1) "" else "s"}")
                        } else {
                            ToastManager.show(result.exceptionOrNull()?.message ?: "No se pudieron añadir los miembros")
                        }
                    }
                },
                onRefreshColleagues = {
                    (chatRepo as? KtorChatRepository)?.fetchColleagues()
                },
                onAssignRole = { userId, rol ->
                    scope.launch {
                        val result = chatRepo.asignarRol(conversationId, userId, rol)
                        if (result.isSuccess) {
                            chatVm.loadParticipants()
                            (chatRepo as? KtorChatRepository)?.refreshConversations()
                            ToastManager.show(
                                if (rol == "ADMIN") "Ahora es administrador" else "Ya no es administrador"
                            )
                        } else {
                            ToastManager.show(result.exceptionOrNull()?.message ?: "No se pudo cambiar el rol")
                        }
                    }
                },
                miRol = conversation?.miRol,
                createdBy = conversation?.createdBy,
                hasLeft = conversation?.hasLeft ?: false,
                eliminacionPendiente = conversation?.eliminacionPendiente ?: false,
                subrouteLabel = SubrouteLabels.nameOf(conversation?.claveRuta),
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
                onBack = { nav.popBackStack() },
                subrouteLabel = groupVm.subrouteLabel
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

        composable(Routes.MAP) {
            com.gowayki.nesh.app.features.map.presentation.screens.MapScreen(
                onBack = { nav.popBackStack() },
                onHomeClick = { goTab(Routes.HOME) },
                onCommunicationClick = { goTab(Routes.COMMUNICATION) },
                onRecorridoClick = { goTab(Routes.ROUTE_DETAIL) },
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

/** Lee solo el contador de SOS en su propio alcance para no recompensar la navegación. */
@Composable
private fun SosTripWatcher(vm: SosViewModel, onTrip: (Int) -> Unit) {
    val secuencia = vm.ui.sosTripRequestSeq
    LaunchedEffect(secuencia) { onTrip(secuencia) }
}
