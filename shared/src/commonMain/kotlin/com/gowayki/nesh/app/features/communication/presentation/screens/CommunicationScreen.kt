// Pantalla de lista de conversaciones estilo WhatsApp.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.communication.presentation.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.gowayki.nesh.app.common.util.formatTime
import com.gowayki.nesh.app.di.AppDi
import com.gowayki.nesh.app.features.communication.domain.model.Conversation
import com.gowayki.nesh.app.features.communication.domain.repository.ChatRepository
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bell
import compose.icons.feathericons.Menu
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Search
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
    onDeleteChat: (String) -> Unit = {},
    onDeleteGroup: (String) -> Unit = {},
    onSosClick: () -> Unit = {},
    onInicioClick: () -> Unit = {},
    onRecorridoClick: () -> Unit = {},
    onMapaClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    BackHandler(true) { onInicioClick() }
    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val vm = viewModel { com.gowayki.nesh.app.features.communication.presentation.chat.ConversationsViewModel(AppDi.get<ChatRepository>()) }
    val conversations = vm.conversations
    val isLoading = vm.loading

    var query by remember { mutableStateOf("") }
    var longPressedConv by remember { mutableStateOf<Conversation?>(null) }
    var deleteTarget by remember { mutableStateOf<Conversation?>(null) }
    var deleteGroupTarget by remember { mutableStateOf<Conversation?>(null) }
    var clearTarget by remember { mutableStateOf<Conversation?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0 Todos, 1 No leídos, 2 Grupos, 3 Archivados

    fun conversationsFor(tab: Int): List<Conversation> {
        val base = if (tab == 3) vm.archived else conversations
        return base.filter { conv ->
            val matchesQuery = query.isBlank() ||
                conv.title.contains(query, ignoreCase = true) ||
                conv.preview.contains(query, ignoreCase = true)
            val matchesTab = when (tab) {
                1 -> conv.unreadCount > 0
                2 -> conv.isGroup
                3 -> true
                // "Todos": un chat de grupo borrado se oculta de aquí y
                // sigue apareciendo en "Grupos".
                else -> !conv.hiddenFromAll
            }
            matchesQuery && matchesTab
        }.sortedByDescending { it.lastMessageAtMillis }
    }

    androidx.compose.material3.ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            androidx.compose.material3.ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                drawerContainerColor = AppColors.cream,
                modifier = Modifier.width(328.dp).fillMaxSize()
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
                        Text(
                            text = "Comunicación",
                            style = AppTextStyles.waykiHeader.copy(fontSize = 20.sp),
                            color = AppColors.cream,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        TopAction(icon = FeatherIcons.Bell, description = "Notificaciones", onClick = onNotificationsClick)
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
                val tabs = listOf("Todos", "No leídos", "Grupos", "Archivados")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEachIndexed { index, label ->
                        val selected = selectedTab == index
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
                                    .then(if (selected) Modifier.fillMaxWidth() else Modifier.width(0.dp))
                                    .clip(CircleShape)
                                    .background(if (selected) AppColors.primary else Color.Transparent)
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = AppColors.slate.copy(alpha = 0.3f))

                // ---------- Lista (transición animada entre pestañas) ----------
                Crossfade(
                    targetState = selectedTab,
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
                    ),
                    label = "communicationTab"
                ) { tab ->
                    val list = conversationsFor(tab)
                    when {
                        isLoading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                androidx.compose.material3.CircularProgressIndicator(color = AppColors.primary)
                            }
                        }
                        list.isEmpty() -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(top = 80.dp, start = 32.dp, end = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(FeatherIcons.Menu, contentDescription = null, tint = AppColors.slate.copy(alpha = 0.6f), modifier = Modifier.size(64.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("Sin conversaciones", style = AppTextStyles.waykiLabelMedium, color = AppColors.ink)
                                Spacer(Modifier.height(4.dp))
                                Text("Crea un grupo o inicia una conversación.", style = AppTextStyles.waykiMini, color = AppColors.slate, textAlign = TextAlign.Center)
                            }
                        }
                        else -> {
                            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 150.dp)) {
                                items(list, key = { it.id }) { conv ->
                                    ConversationRow(conv = conv, onClick = { onConversationClick(conv.id) }, onLongClick = { longPressedConv = conv })
                                    HorizontalDivider(thickness = 0.5.dp, color = AppColors.slate.copy(alpha = 0.2f), modifier = Modifier.padding(start = 80.dp))
                                }
                            }
                        }
                    }
                }
            }


    longPressedConv?.let { conv ->
        val sheetState = androidx.compose.material3.rememberModalBottomSheetState()
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { longPressedConv = null },
            sheetState = sheetState,
            containerColor = AppColors.cream
        ) {
            Text(conv.title, style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold), color = AppColors.ink, modifier = Modifier.padding(16.dp))
            SheetActionRow(
                label = if (selectedTab == 3) "Desarchivar" else "Archivar",
                onClick = {
                    if (selectedTab == 3) vm.unarchiveConversation(conv.id)
                    else vm.archiveConversation(conv.id)
                    longPressedConv = null
                }
            )
            SheetActionRow("Vaciar chat", onClick = { clearTarget = conv; longPressedConv = null })
            if (conv.isGroup) {
                SheetActionRow(
                    label = "Eliminar grupo",
                    onClick = { deleteGroupTarget = conv; longPressedConv = null },
                    color = AppColors.error
                )
            }
            SheetActionRow("Eliminar chat", onClick = { deleteTarget = conv; longPressedConv = null }, color = AppColors.error)
            Spacer(Modifier.height(8.dp))
        }
    }

    deleteTarget?.let { target ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("¿Eliminar este chat?") },
            text = {
                Text(
                    if (target.isGroup) "Se eliminará esta conversación de tu lista de chats. El grupo seguirá disponible en la sección Grupos y podrás volver a abrirlo."
                    else "Se borrará \"${target.title}\" con sus mensajes y audios."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { onDeleteChat(target.id); deleteTarget = null }) {
                    Text("Eliminar chat", color = AppColors.ink)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") }
            }
        )
    }

    clearTarget?.let { target ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { clearTarget = null },
            title = { Text("¿Vaciar este chat?") },
            text = {
                Text("Se eliminarán todos los mensajes de esta conversación. El chat seguirá disponible en tu lista y podrás seguir enviando y recibiendo mensajes.")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { onClearChat(target.id); clearTarget = null }) {
                    Text("Vaciar chat", color = AppColors.ink)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { clearTarget = null }) { Text("Cancelar") }
            }
        )
    }

    deleteGroupTarget?.let { target ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteGroupTarget = null },
            title = { Text("¿Eliminar grupo?") },
            text = { Text("\"${target.title}\" se eliminará por completo con todos sus mensajes y audios.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { onDeleteGroup(target.id); deleteGroupTarget = null }) {
                    Text("Eliminar", color = AppColors.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleteGroupTarget = null }) { Text("Cancelar") }
            }
        )
    }

            // ---------- FAB ----------
            // Se eleva por encima de la barra flotante (76dp + 12dp de margen)
            // más el inset de la barra de navegación del sistema.
            val navInset = androidx.compose.foundation.layout.WindowInsets.navigationBars
                .asPaddingValues()
                .calculateBottomPadding()
            androidx.compose.material3.FloatingActionButton(
                onClick = onNewGroupClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 76.dp + 12.dp + navInset + 16.dp),
                shape = CircleShape,
                containerColor = AppColors.primary,
                contentColor = AppColors.cream
            ) {
                Icon(FeatherIcons.Plus, contentDescription = "Nuevo chat")
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
private fun ConversationRow(conv: Conversation, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WaykiAvatar(text = conv.avatarText, size = 52.dp, dark = conv.avatarDark, photoPath = conv.photoPath)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(conv.title, style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold), color = AppColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(conv.preview, style = AppTextStyles.waykiMini, color = AppColors.slate, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatTime(conv.lastMessageAtMillis),
                style = AppTextStyles.labelSmall,
                color = if (conv.unreadCount > 0) AppColors.primary else AppColors.slate
            )
            Spacer(Modifier.height(6.dp))
            if (conv.unreadCount > 0) {
                Box(Modifier.size(22.dp).background(AppColors.primary, CircleShape), contentAlignment = Alignment.Center) {
                    Text(conv.unreadCount.toString(), style = AppTextStyles.labelSmall.copy(fontSize = 11.sp), color = AppColors.cream)
                }
            } else {
                Spacer(Modifier.height(22.dp))
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
