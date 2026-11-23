// Pantalla de lista de conversaciones estilo WhatsApp.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.communication.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gowayki.nesh.app.common.ui.shared.BackHandler
import com.gowayki.nesh.app.common.ui.shared.WaykiAvatar
import com.gowayki.nesh.app.common.ui.shared.WaykiBottomNav
import com.gowayki.nesh.app.common.ui.shared.WaykiSidebarContent
import com.gowayki.nesh.app.common.ui.shared.WaykiSkeleton
import com.gowayki.nesh.app.common.ui.shared.isScrollingUp
import com.gowayki.nesh.app.common.util.formatTime
import com.gowayki.nesh.app.di.AppDi
import com.gowayki.nesh.app.features.communication.domain.model.Conversation
import com.gowayki.nesh.app.features.communication.domain.repository.ChatRepository
import com.gowayki.nesh.app.features.map.presentation.SubrouteLabels
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bell
import compose.icons.feathericons.Check
import compose.icons.feathericons.CheckSquare
import compose.icons.feathericons.Menu
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Search
import compose.icons.feathericons.Trash2
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CommunicationScreen(
    onMenuClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onTabSelected: (Int) -> Unit = {},
    onConversationClick: (String) -> Unit = {},
    onNewGroupClick: () -> Unit = {},
    onClearChat: (String) -> Unit = {},
    onHideChat: (String) -> Unit = {},
    onRestoreChat: (String) -> Unit = {},
    onRequestGroupDeletion: (String) -> Unit = {},
    onCancelGroupDeletion: (String) -> Unit = {},
    pendingDeletions: List<Conversation> = emptyList(),
    onLeaveGroup: (String) -> Unit = {},
    onSosClick: () -> Unit = {},
    onInicioClick: () -> Unit = {},
    onRecorridoClick: () -> Unit = {},
    onMapaClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val vm = viewModel { com.gowayki.nesh.app.features.communication.presentation.chat.ConversationsViewModel(AppDi.get<ChatRepository>()) }
    val conversations = vm.conversations
    val isLoading = vm.loading

    var query by rememberSaveable { mutableStateOf("") }
    var longPressedConv by remember { mutableStateOf<Conversation?>(null) }
    var hideTarget by remember { mutableStateOf<Conversation?>(null) }
    var requestDeleteTarget by remember { mutableStateOf<Conversation?>(null) }
    var cancelDeleteTarget by remember { mutableStateOf<Conversation?>(null) }
    var leaveGroupTarget by remember { mutableStateOf<Conversation?>(null) }
    var clearTarget by remember { mutableStateOf<Conversation?>(null) }
    var showPendingDeletions by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(0) } // 0 Todos, 1 No leídos, 2 Grupos, 3 Archivados, 4 Ocultos

    // Modo selección múltiple: se eligen varios chats y se ocultan de una vez.
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmBulkHide by remember { mutableStateOf(false) }

    fun exitSelection() {
        selectionMode = false
        selectedIds = emptySet()
    }

    fun toggleSelection(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    // Atrás: primero sale del modo selección, después navega al inicio.
    BackHandler(true) { if (selectionMode) exitSelection() else onInicioClick() }

    // Al cambiar de pestaña se vuelve arriba y el FAB reaparece.
    LaunchedEffect(selectedTab) {
        if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
            listState.scrollToItem(0)
        }
    }

    fun conversationsFor(
        tab: Int,
        q: String,
        all: List<Conversation>,
        archivedList: List<Conversation>,
        hiddenList: List<Conversation>
    ): List<Conversation> {
        val base = when (tab) {
            3 -> archivedList
            4 -> hiddenList
            else -> all
        }
        return base.filter { conv ->
            val matchesQuery = q.isBlank() ||
                conv.title.contains(q, ignoreCase = true) ||
                conv.preview.contains(q, ignoreCase = true)
            val matchesTab = when (tab) {
                1 -> conv.unreadCount > 0
                2 -> conv.isGroup
                3, 4 -> true
                else -> true
            }
            matchesQuery && matchesTab
        }.sortedByDescending { it.lastMessageAtMillis }
    }

    val visibleLists = androidx.compose.runtime.remember(conversations, vm.archived, vm.hidden, query) {
        listOf(0, 1, 2, 3, 4).associateWith { tab -> conversationsFor(tab, query, conversations, vm.archived, vm.hidden) }
    }

    androidx.compose.material3.ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            androidx.compose.material3.ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                drawerContainerColor = AppColors.cream,
                modifier = Modifier
                    .widthIn(max = 328.dp)
                    .fillMaxWidth(0.88f)
                    .fillMaxSize()
            ) {
                WaykiSidebarContent(
                    selectedTab = 1,
                    onClose = { coroutineScope.launch { drawerState.close() } },
                    onSosClick = { coroutineScope.launch { drawerState.close() }; onSosClick() },
                    onInicioClick = { coroutineScope.launch { drawerState.close() }; onInicioClick() },
                    onCommunicationClick = { coroutineScope.launch { drawerState.close() } },
                    onRecorridoClick = { coroutineScope.launch { drawerState.close() }; onRecorridoClick() },
                    onMapaClick = { coroutineScope.launch { drawerState.close() }; onMapaClick() },
                    onSettingsClick = { coroutineScope.launch { drawerState.close() }; onSettingsClick() },
                    onSupportClick = { coroutineScope.launch { drawerState.close() }; onSupportClick() },
                    onLogoutClick = { coroutineScope.launch { drawerState.close() }; onLogoutClick() }
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(AppColors.cream)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ---------- Top bar estilo WhatsApp ----------
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.primary
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(60.dp)
                            .padding(start = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TopAction(icon = FeatherIcons.Menu, description = "Menú", onClick = { coroutineScope.launch { drawerState.open() } })
                        if (selectionMode) {
                            Text(
                                text = if (selectedIds.isEmpty()) "Selecciona chats" else "${selectedIds.size} seleccionado${if (selectedIds.size == 1) "" else "s"}",
                                style = AppTextStyles.waykiHeader.copy(fontSize = 20.sp),
                                color = AppColors.cream,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            TopAction(
                                icon = FeatherIcons.Trash2,
                                description = "Ocultar seleccionados",
                                onClick = { if (selectedIds.isNotEmpty()) confirmBulkHide = true }
                            )
                        } else {
                            Text(
                                text = "Comunicación",
                                style = AppTextStyles.waykiHeader.copy(fontSize = 20.sp),
                                color = AppColors.cream,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            TopAction(icon = FeatherIcons.CheckSquare, description = "Seleccionar chats", onClick = { selectionMode = true })
                            if (pendingDeletions.isNotEmpty()) {
                                TopAction(
                                    icon = FeatherIcons.Trash2,
                                    description = "Eliminaciones pendientes",
                                    onClick = { showPendingDeletions = true }
                                )
                            }
                            TopAction(icon = FeatherIcons.Bell, description = "Notificaciones", onClick = onNotificationsClick)
                        }
                    }
                }

                // ---------- Buscador ----------
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(AppColors.mauve.copy(alpha = 0.35f))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(FeatherIcons.Search, contentDescription = "Buscar", tint = AppColors.slate, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = AppTextStyles.waykiLabel.copy(color = AppColors.ink),
                            cursorBrush = SolidColor(AppColors.ink),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (query.isEmpty()) {
                                        Text("Buscar", style = AppTextStyles.waykiMini, color = AppColors.slate)
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                }

                // ---------- Tabs ----------
                val tabs = listOf("Todos", "No leídos", "Grupos", "Archivados", "Ocultos")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEachIndexed { index, label ->
                        val selected = selectedTab == index
                        val indicator by animateFloatAsState(
                            targetValue = if (selected) 1f else 0f,
                            animationSpec = tween(
                                durationMillis = 260,
                                easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
                            ),
                            label = "tabIndicator$index"
                        )
                        Column(
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = index }
                                .padding(horizontal = 12.dp)
                                .width(IntrinsicSize.Max),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = label,
                                style = AppTextStyles.waykiLabelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
                                color = if (selected) AppColors.primary else AppColors.slate,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .height(3.dp)
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = indicator
                                        alpha = indicator
                                    }
                                    .clip(CircleShape)
                                    .background(AppColors.primary)
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = AppColors.slate.copy(alpha = 0.3f))

                // ---------- Lista (transición animada entre pestañas) ----------
                AnimatedContent(
                    targetState = selectedTab,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        val forward = targetState > initialState
                        val easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
                        (fadeIn(tween(240, easing = easing)) +
                            slideInHorizontally(tween(320, easing = easing)) { width ->
                                if (forward) width / 10 else -width / 10
                            }) togetherWith
                            (fadeOut(tween(160)) +
                                slideOutHorizontally(tween(240, easing = easing)) { width ->
                                    if (forward) -width / 16 else width / 16
                                })
                    },
                    label = "communicationTab"
                ) { tab ->
                    val list = visibleLists[tab].orEmpty()
                    when {
                        isLoading -> ConversationsSkeleton()
                        list.isEmpty() -> ConversationsEmpty(onNewChat = onNewGroupClick)
                        else -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 150.dp)
                            ) {
                                items(list, key = { it.id }) { conv ->
                                    Column(Modifier.animateItem()) {
                                        ConversationRow(
                                            conv = conv,
                                            selectionMode = selectionMode,
                                            selected = conv.id in selectedIds,
                                            onClick = {
                                                if (selectionMode) toggleSelection(conv.id) else onConversationClick(conv.id)
                                            },
                                            onLongClick = {
                                                if (selectionMode) toggleSelection(conv.id) else longPressedConv = conv
                                            }
                                        )
                                        HorizontalDivider(
                                            thickness = 0.5.dp,
                                            color = AppColors.slate.copy(alpha = 0.2f),
                                            modifier = Modifier.padding(start = 80.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showPendingDeletions) {
                AlertDialog(
                    onDismissRequest = { showPendingDeletions = false },
                    title = {
                        Text(
                            text = "Eliminaciones pendientes",
                            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.ink,
                        )
                    },
                    text = {
                        LazyColumn {
                            items(pendingDeletions, key = { it.id }) { conv ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = conv.title,
                                            style = AppTextStyles.waykiLabelMedium,
                                            color = AppColors.ink,
                                            maxLines = 1,
                                        )
                                        conv.eliminacionProgramadaPara?.let { fecha ->
                                            Text(
                                                text = "Se elimina: $fecha",
                                                style = AppTextStyles.waykiMini,
                                                color = AppColors.slate,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                    TextButton(onClick = {
                                        onCancelGroupDeletion(conv.id)
                                        showPendingDeletions = false
                                    }) {
                                        Text("Cancelar", color = AppColors.primary)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPendingDeletions = false }) {
                            Text("Cerrar", color = AppColors.primary)
                        }
                    },
                )
            }

            longPressedConv?.let { conv ->
                val sheetState = androidx.compose.material3.rememberModalBottomSheetState()
                androidx.compose.material3.ModalBottomSheet(
                    onDismissRequest = { longPressedConv = null },
                    sheetState = sheetState,
                    containerColor = AppColors.cream
                ) {
                    Text(conv.title, style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold), color = AppColors.ink, modifier = Modifier.padding(16.dp))
                    if (selectedTab == 4) {
                        SheetActionRow(
                            label = "Restaurar chat",
                            onClick = { onRestoreChat(conv.id); longPressedConv = null }
                        )
                    } else {
                        SheetActionRow(
                            label = "Seleccionar chats",
                            onClick = { selectionMode = true; selectedIds = setOf(conv.id); longPressedConv = null }
                        )
                        SheetActionRow(
                            label = if (selectedTab == 3) "Desarchivar" else "Archivar",
                            onClick = {
                                if (selectedTab == 3) vm.unarchiveConversation(conv.id)
                                else vm.archiveConversation(conv.id)
                                longPressedConv = null
                            }
                        )
                        SheetActionRow("Limpiar chat (solo para ti)", onClick = { clearTarget = conv; longPressedConv = null })

                        if (conv.isGroup && !conv.hasLeft) {
                            SheetActionRow(
                                label = "Salir del grupo",
                                onClick = { leaveGroupTarget = conv; longPressedConv = null }
                            )
                            val puedeGestionarEliminacion =
                                conv.miRol == "ADMIN" || conv.createdBy == com.gowayki.nesh.core.session.UserSession.currentUserId
                            if (puedeGestionarEliminacion && !conv.eliminacionPendiente) {
                                SheetActionRow(
                                    label = "Eliminar grupo (definitivo)",
                                    onClick = { requestDeleteTarget = conv; longPressedConv = null },
                                    color = AppColors.error
                                )
                            }
                            if (puedeGestionarEliminacion && conv.eliminacionPendiente) {
                                SheetActionRow(
                                    label = "Cancelar eliminación del grupo",
                                    onClick = { cancelDeleteTarget = conv; longPressedConv = null }
                                )
                            }
                        }

                        SheetActionRow(
                            label = "Ocultar chat",
                            onClick = { hideTarget = conv; longPressedConv = null },
                            color = AppColors.error
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            hideTarget?.let { target ->
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { hideTarget = null },
                    title = { Text("¿Ocultar este chat?") },
                    text = {
                        Text(
                            "Se quitará \"${target.title}\" de tu lista. Solo lo ocultas para ti: " +
                                "no se borran mensajes ni audios y los demás participantes no notan nada. " +
                                "Podrás restaurarlo desde Ocultos."
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = { onHideChat(target.id); hideTarget = null }) {
                            Text("Ocultar", color = AppColors.ink)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { hideTarget = null }) { Text("Cancelar") }
                    }
                )
            }

            requestDeleteTarget?.let { target ->
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { requestDeleteTarget = null },
                    title = { Text("¿Eliminar grupo definitivamente?") },
                    text = {
                        Text(
                            "\"${target.title}\" desaparecerá inmediatamente para todos los participantes. " +
                                "Durante las próximas 72 horas podrás cancelar la eliminación; pasado ese plazo " +
                                "se borrarán definitivamente el grupo, sus mensajes y sus archivos."
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = { onRequestGroupDeletion(target.id); requestDeleteTarget = null }) {
                            Text("Programar eliminación", color = AppColors.error)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { requestDeleteTarget = null }) { Text("Cancelar") }
                    }
                )
            }

            cancelDeleteTarget?.let { target ->
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { cancelDeleteTarget = null },
                    title = { Text("¿Cancelar la eliminación?") },
                    text = {
                        Text(
                            "El grupo \"${target.title}\" dejará de estar pendiente de eliminación " +
                                "y seguirá disponible para sus participantes."
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = { onCancelGroupDeletion(target.id); cancelDeleteTarget = null }) {
                            Text("Cancelar eliminación", color = AppColors.ink)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { cancelDeleteTarget = null }) { Text("Volver") }
                    }
                )
            }

            clearTarget?.let { target ->
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { clearTarget = null },
                    title = { Text("¿Limpiar este chat?") },
                    text = {
                        Text("Solo afecta a tu cuenta: ocultarás los mensajes anteriores y el chat saldrá de tu lista, también al reinstalar la app. Los mensajes NO se borran y los demás participantes no notan nada. Si llega un mensaje nuevo, el chat vuelve a aparecer mostrando solo lo nuevo.")
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = { onClearChat(target.id); clearTarget = null }) {
                            Text("Limpiar chat", color = AppColors.ink)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { clearTarget = null }) { Text("Cancelar") }
                    }
                )
            }

            leaveGroupTarget?.let { target ->
                val esAdminGrupo = target.miRol == "ADMIN" || target.createdBy == com.gowayki.nesh.core.session.UserSession.currentUserId
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { leaveGroupTarget = null },
                    title = { Text(if (esAdminGrupo) "¿Salir y eliminar el grupo?" else "¿Salir del grupo?") },
                    text = {
                        Text(
                            if (esAdminGrupo) {
                                "Si sales del grupo \"${target.title}\", el grupo se eliminará una vez que salgas. " +
                                    "Se programará su eliminación para todos los participantes."
                            } else {
                                "Dejarás de participar en \"${target.title}\". El grupo y sus mensajes siguen existiendo para los demás y podrás seguir viendo este chat hasta que lo ocultes."
                            }
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            if (esAdminGrupo) {
                                onRequestGroupDeletion(target.id)
                            } else {
                                onLeaveGroup(target.id)
                            }
                            leaveGroupTarget = null
                        }) {
                            Text(if (esAdminGrupo) "Salir y eliminar" else "Salir", color = AppColors.error)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { leaveGroupTarget = null }) { Text("Cancelar") }
                    }
                )
            }

            if (confirmBulkHide) {
                val base = when (selectedTab) {
                    3 -> vm.archived
                    4 -> vm.hidden
                    else -> conversations
                }
                val elegidos = base.filter { it.id in selectedIds }
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { confirmBulkHide = false },
                    title = {
                        Text(if (elegidos.size == 1) "¿Ocultar 1 chat?" else "¿Ocultar ${elegidos.size} chats?")
                    },
                    text = {
                        Text(
                            "Se quitarán de tu lista solo para ti. No se borran mensajes ni audios y " +
                                "los demás participantes no notan nada. Podrás restaurarlos desde Ocultos."
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            elegidos.forEach { conv -> onHideChat(conv.id) }
                            confirmBulkHide = false
                            exitSelection()
                        }) {
                            Text("Ocultar", color = AppColors.ink)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { confirmBulkHide = false }) { Text("Cancelar") }
                    }
                )
            }

            // ---------- FAB ----------
            // Se eleva por encima de la barra flotante (76dp + 12dp de margen)
            // más el inset de la barra de navegación del sistema. Se oculta al
            // bajar la lista y reaparece al subir.
            val navInset = androidx.compose.foundation.layout.WindowInsets.navigationBars
                .asPaddingValues()
                .calculateBottomPadding()
            if (!selectionMode) {
                NewChatFab(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    listState = listState,
                    bottomPadding = 76.dp + 12.dp + navInset + 16.dp,
                    onClick = onNewGroupClick
                )
            }

            // ---------- Bottom nav ----------
            WaykiBottomNav(
                selectedTab = 1,
                onTabSelected = { tab ->
                    if (tab == 0) onInicioClick()
                    else if (tab == 2) onRecorridoClick()
                    else if (tab == 3) onMapaClick()
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun NewChatFab(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    // Se oculta al bajar y reaparece al subir. El estado de scroll se lee
    // aquí dentro para que solo este FAB se recomponga, no toda la lista.
    val visible = listState.isScrollingUp()
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            animationSpec = tween(220, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
        ) { fullHeight -> fullHeight / 2 } + fadeIn(tween(160)),
        exit = slideOutVertically(
            animationSpec = tween(180, easing = CubicBezierEasing(0.4f, 0f, 1f, 1f))
        ) { fullHeight -> fullHeight / 2 } + fadeOut(tween(120))
    ) {
        androidx.compose.material3.FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.padding(end = 16.dp, bottom = bottomPadding),
            shape = CircleShape,
            containerColor = AppColors.primary,
            contentColor = AppColors.cream
        ) {
            Icon(FeatherIcons.Plus, contentDescription = "Nuevo chat")
        }
    }
}

@Composable
private fun TopAction(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = AppColors.cream, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ConversationRow(
    conv: Conversation,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) AppColors.primary.copy(alpha = 0.10f) else androidx.compose.ui.graphics.Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) AppColors.primary else androidx.compose.ui.graphics.Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (selected) AppColors.primary else AppColors.slate,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        FeatherIcons.Check,
                        contentDescription = "Seleccionado",
                        tint = AppColors.cream,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
        }
        WaykiAvatar(text = conv.avatarText, size = 52.dp, dark = conv.avatarDark, photoPath = conv.photoPath)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (conv.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(AppColors.primary),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    conv.title,
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (conv.eliminacionPendiente) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(AppColors.error.copy(alpha = 0.12f), CircleShape)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Se eliminará",
                            style = AppTextStyles.waykiMini.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = AppColors.error
                        )
                    }
                } else if (conv.isGroup && conv.hasLeft) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(AppColors.slate.copy(alpha = 0.12f), CircleShape)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text("Saliste", style = AppTextStyles.waykiMini.copy(fontSize = 10.sp), color = AppColors.slate)
                    }
                } else if (conv.isGroup && conv.miRol == "ADMIN") {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(AppColors.primary.copy(alpha = 0.12f), CircleShape)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text("Admin", style = AppTextStyles.waykiMini.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = AppColors.primary)
                    }
                } else if (conv.tipoConversacion == "GRUPO_RUTA") {
                    // Tipo del grupo: este chat pertenece a una subruta concreta.
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(AppColors.primary.copy(alpha = 0.12f), CircleShape)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text("RUTA", style = AppTextStyles.waykiMini.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = AppColors.primary)
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
            val subruta = SubrouteLabels.nameOf(conv.claveRuta)
            when {
                conv.eliminacionPendiente -> Text(
                    "Pendiente de eliminación en 72 horas",
                    style = AppTextStyles.waykiMini,
                    color = AppColors.error,
                    maxLines = 1,
                )
                conv.isGroup && conv.hasLeft -> Text(
                    "Ya no participas en este grupo",
                    style = AppTextStyles.waykiMini,
                    color = AppColors.slate,
                    maxLines = 1,
                )
                else -> {
                    // Tipo del grupo, con detalle del primero (Ciudad de Dios → Terminal).
                    if (conv.tipoConversacion == "GRUPO_RUTA") {
                        Text(
                            text = "Tipo: GRUPO_RUTA" +
                                if (conv.claveRuta == "SB-17620010") " · ruta principal" else "",
                            style = AppTextStyles.waykiMini.copy(fontSize = 10.sp),
                            color = AppColors.slate,
                            maxLines = 1,
                        )
                    }
                    // Indicador de subruta arriba y mensaje reciente debajo.
                    if (subruta != null) {
                        Text(
                            text = "Subruta: $subruta",
                            style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.primary,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = conv.preview,
                        style = AppTextStyles.waykiMini,
                        color = AppColors.slate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatTime(conv.lastMessageAtMillis),
                    style = AppTextStyles.labelSmall,
                    color = if (conv.unreadCount > 0) AppColors.primary else AppColors.slate,
                )
                if (conv.unreadCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .defaultMinSize(20.dp, 20.dp)
                            .background(AppColors.primary, CircleShape)
                            .padding(horizontal = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (conv.unreadCount > 99) "99+" else "${conv.unreadCount}",
                            style = AppTextStyles.labelSmall.copy(fontSize = 11.sp),
                            color = AppColors.cream,
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            if (conv.unreadCount <= 0) {
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SheetActionRow(label: String, onClick: () -> Unit, color: androidx.compose.ui.graphics.Color = AppColors.ink) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = AppTextStyles.waykiLabelMedium, color = color)
    }
}

/**
 * Esqueletos con la forma real de una fila de conversación.
 * Reemplazan al spinner centrado: la lista "ya está ahí" mientras carga y
 * el cambio a contenido real no da un salto visual.
 */
@Composable
private fun ConversationsSkeleton() {
    Column(Modifier.fillMaxSize()) {
        repeat(7) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WaykiSkeleton(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = AppColors.slate.copy(alpha = 0.18f)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    WaykiSkeleton(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(13.dp),
                        color = AppColors.slate.copy(alpha = 0.18f)
                    )
                    Spacer(Modifier.height(8.dp))
                    WaykiSkeleton(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(10.dp),
                        color = AppColors.slate.copy(alpha = 0.12f)
                    )
                }
                Spacer(Modifier.width(16.dp))
                WaykiSkeleton(
                    modifier = Modifier
                        .width(38.dp)
                        .height(10.dp),
                    color = AppColors.slate.copy(alpha = 0.12f)
                )
            }
            if (index < 6) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = AppColors.slate.copy(alpha = 0.12f),
                    modifier = Modifier.padding(start = 80.dp)
                )
            }
        }
    }
}

@Composable
private fun ConversationsEmpty(onNewChat: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 72.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(AppColors.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = FeatherIcons.Menu,
                contentDescription = null,
                tint = AppColors.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(38.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Sin conversaciones",
            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
            color = AppColors.ink
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Crea un grupo o inicia una conversación con tu línea.",
            style = AppTextStyles.waykiMini,
            color = AppColors.slate,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onNewChat)
                .background(AppColors.primary)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = FeatherIcons.Plus,
                contentDescription = null,
                tint = AppColors.cream,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Nuevo chat",
                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                color = AppColors.cream
            )
        }
    }
}
