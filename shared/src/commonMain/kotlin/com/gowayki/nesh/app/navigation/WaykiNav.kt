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
import androidx.compose.animation.scaleIn
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
import com.gowayki.nesh.core.session.SesionExpiradaState
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
import com.gowayki.nesh.app.features.map.presentation.SelectedRouteHolder
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
import com.gowayki.nesh.domain.repository.AuthRepository
import com.gowayki.nesh.app.features.sign_in.presentation.screens.RegisterScreen
import com.gowayki.nesh.app.features.welcome.presentation.screens.WelcomeScreen
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

// Movimiento único y sobrio: la pantalla entra con un desplazamiento corto y
// una escala casi imperceptible; al volver solo hace fade, sin slide ni zoom,
// para que no se superponga con el gesto del sistema.
private val WaykiEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val WaykiExitEasing = FastOutLinearInEasing

private fun forwardEnter(): EnterTransition =
    fadeIn(tween(180, easing = WaykiEasing)) +
        scaleIn(initialScale = 0.985f, animationSpec = tween(220, easing = WaykiEasing)) +
        slideInHorizontally(tween(220, easing = WaykiEasing)) { width -> width / 20 }

private fun forwardExit(): ExitTransition =
    fadeOut(tween(130, easing = WaykiExitEasing))

private fun backEnter(): EnterTransition =
    fadeIn(tween(160, easing = WaykiEasing))

private fun backExit(): ExitTransition =
    fadeOut(tween(130, easing = WaykiExitEasing))

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
    val sesionRepo = remember { AppDi.get<ChatRepository>() }
    val scope = rememberCoroutineScope()

    /** Cierra sesión de verdad: token, socket y todo el historial del back stack. */
    fun cerrarSesion() {
        // Primero el repositorio guarda el metadata por cuenta y cierra el
        // socket; después se limpia la sesión en memoria.
        (sesionRepo as? KtorChatRepository)?.cerrarSesion()
        // La ruta del chofer anterior no debe heredarla el siguiente (UserSession.clear()
        // también lo hace: resetForLogout() es idempotente y no llama a clear()).
        SelectedRouteHolder.resetForLogout()
        UserSession.clear()
        nav.navigate(Routes.WELCOME) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    // Si esta cuenta abrió sesión en otro dispositivo, el backend revoca el
    // token y el siguiente 401 enciende la señal: cerramos y volvemos al inicio.
    LaunchedEffect(Unit) {
        SesionExpiradaState.expirada.collect { expirada ->
            if (expirada) {
                ToastManager.show("Esta cuenta abrió sesión en otro dispositivo.")
                cerrarSesion()
                SesionExpiradaState.consumir()
            }
        }
    }

    /** Elimina la cuenta (borrado lógico) y vuelve al inicio. */
    fun eliminarCuenta() {
        scope.launch {
            val resultado = AppDi.get<AuthRepository>().deleteAccount()
            if (resultado.isSuccess) {
                ToastManager.show("Cuenta eliminada. Vuelve a registrarte cuando quieras.")
                cerrarSesion()
            } else {
                ToastManager.show(
                    "No se pudo eliminar la cuenta: ${resultado.exceptionOrNull()?.message ?: "intenta de nuevo"}"
                )
            }
        }
    }

    // Triple clic desde cualquier módulo lleva al SOS. Se observa en su propio
    // alcance: si se leyera directo aquí, cada cambio de estado del SOS
    // recompondría el NavHost completo (scroll y cambios de módulo con lag).
    SosTripWatcher(sosVm) { secuencia ->
        // Sin sesión iniciada el ESP32 no debe sacarnos del Welcome/Login.
        if (secuencia > 0 && UserSession.currentUserId != null &&
            nav.currentBackStackEntry?.destination?.route != Routes.SOS
        ) {
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
                onRegisterClick = { name, dni, phone, email, pin, subrutaId ->
                    vm.registrarSolicitud(name, dni, phone, email, pin, subrutaId)
                },
                onClearError = { vm.clearAuthError() },
                onLoginClick = { nav.popBackStack() },
                onBack = { nav.popBackStack() },
            )
            LaunchedEffect(state.mensaje) {
                state.mensaje?.let {
                    ToastManager.show(it)
                    vm.consumeMensaje()
                    nav.popBackStack()
                }
            }
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
                onLogoutClick = { cerrarSesion() },
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
                onLogoutClick = { cerrarSesion() },
                onSolicitarCambioSubruta = { subrutaId ->
                    scope.launch {
                        val resultado = sesionRepo.solicitarCambioSubruta(subrutaId)
                        ToastManager.show(
                            if (resultado.isSuccess) "Solicitud enviada. El jefe de la subruta debe aprobarla."
                            else resultado.exceptionOrNull()?.message ?: "No se pudo enviar la solicitud"
                        )
                    }
                }
            )
        }
        
        composable(Routes.COMMUNICATION) {
            val chatRepo = remember { AppDi.get<ChatRepository>() }
            val scope = rememberCoroutineScope()
            val pendingDeletions by chatRepo.eliminacionesPendientes().collectAsState(initial = emptyList())
            com.gowayki.nesh.app.features.communication.presentation.screens.CommunicationScreen(
                pendingDeletions = pendingDeletions,
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
                onLogoutClick = { cerrarSesion() }
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
                onLogoutClick = { cerrarSesion() },
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
            // Los chats abiertos desde "Ocultos" también deben conservar su
            // identidad de grupo, rol y estado de salida.
            val hiddenConversations by remember { chatRepo.hiddenConversations() }
                .collectAsState(initial = emptyList())
            val conversation = conversations.firstOrNull { it.id == conversationId }
                ?: archivedConversations.firstOrNull { it.id == conversationId }
                ?: hiddenConversations.firstOrNull { it.id == conversationId }
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
                    // Salta directo al mapa, descartando Settings y RoutePicker del
                    // backstack. popBackStack(MAP) era un no-op si el mapa no estaba
                    // apilado (entrada desde Home): popUpTo(RoutePicker) es siempre
                    // determinista y deja una sola copia del mapa.
                    nav.navigate(Routes.MAP) {
                        popUpTo(Routes.ROUTE_PICKER) { inclusive = true }
                        launchSingleTop = true
                    }
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
                onLogoutClick = { cerrarSesion() }
            )
        }

        composable(Routes.SETTINGS) {
            com.gowayki.nesh.app.features.settings.presentation.screens.SettingsScreen(
                onBack = { if (!nav.popBackStack()) nav.navigate(Routes.HOME) },
                onNotificationsClick = { ToastManager.show("No tienes notificaciones nuevas por ahora") },
                onMapaClick = { nav.navigate(Routes.MAP) },
                onRoutePickerClick = { nav.navigate(Routes.ROUTE_PICKER) },
                onProfileClick = { nav.navigate(Routes.PROFILE) },
                onDeleteAccount = { eliminarCuenta() },
                onChangePin = { actual, nueva -> vm.cambiarPin(actual, nueva) },
                onCambiarSubruta = { subrutaId ->
                    scope.launch {
                        val resultado = sesionRepo.solicitarCambioSubruta(subrutaId)
                        ToastManager.show(
                            if (resultado.isSuccess) "Solicitud enviada al jefe de la subruta."
                            else resultado.exceptionOrNull()?.message ?: "No se pudo enviar la solicitud"
                        )
                    }
                },
                sosVm = sosVm,
            )
            LaunchedEffect(state.mensaje) {
                state.mensaje?.let {
                    ToastManager.show(it)
                    vm.consumeMensaje()
                }
            }
            LaunchedEffect(state.error) {
                if (state.error != null && state.mensaje == null) {
                    ToastManager.show(state.error)
                    vm.clearAuthError()
                }
            }
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
