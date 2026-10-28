package com.gowayki.nesh.app.features.communication.presentation.screens

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ripple
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.gowayki.nesh.app.common.ui.shared.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gowayki.nesh.app.di.AppDi
import com.gowayki.nesh.app.common.ui.shared.WaykiAvatar
import com.gowayki.nesh.app.common.ui.shared.WaykiConfirmDialog
import com.gowayki.nesh.app.common.ui.shared.WaykiBottomNav
import com.gowayki.nesh.app.common.ui.shared.WaykiSkeleton
import com.gowayki.nesh.app.features.communication.domain.model.Conversation
import com.gowayki.nesh.app.features.communication.domain.repository.ChatRepository
import com.gowayki.nesh.app.features.communication.presentation.chat.ConversationAction
import com.gowayki.nesh.app.features.communication.presentation.chat.ConversationMenuSheet
import com.gowayki.nesh.app.features.communication.presentation.chat.ConversationsViewModel
import com.gowayki.nesh.app.features.home.presentation.screens.WaykiSidebarContent
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowDown
import compose.icons.feathericons.Bell
import compose.icons.feathericons.Menu
import compose.icons.feathericons.MessageCircle
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Search
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CommunicationScreen(
    onMenuClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onTabSelected: (Int) -> Unit = {},
    onConversationClick: (String) -> Unit = {},
    onNewGroupClick: () -> Unit = {},
    onClearChat: (String) -> Unit = {},
    onDeleteChat: (String) -> Unit = {},
    onSosClick: () -> Unit = {},
    onInicioClick: () -> Unit = {},
    onRecorridoClick: () -> Unit = {},
    onMapaClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    BackHandler(true) { onInicioClick() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Menú contextual: conversación sobre la que se está actuando.
    var menuFor by remember { mutableStateOf<Conversation?>(null) }
    var confirmDelete by remember { mutableStateOf<Conversation?>(null) }

    // Estado desde el ViewModel (repositorio fake por ahora).
    // Se crea fresco cada vez: si fuera singleton, su viewModelScope
    // quedaría cancelado al salir y las acciones no se ejecutarían.
    val vm = viewModel { ConversationsViewModel(AppDi.get<ChatRepository>()) }
    val selectedCategory = vm.category
    val searchQuery = vm.query
    val filteredConversations = vm.visible
    val isLoading = vm.loading

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                drawerContainerColor = AppColors.cream,
                modifier = Modifier.width(328.dp).fillMaxHeight()
            ) {
                WaykiSidebarContent(
                    onClose = {
                        coroutineScope.launch { drawerState.close() }
                    },
                    onSosClick = {
                        coroutineScope.launch { drawerState.close() }
                        onSosClick()
                    },
                    onInicioClick = {
                        coroutineScope.launch { drawerState.close() }
                        onInicioClick()
                    },
                    onCommunicationClick = {
                        coroutineScope.launch { drawerState.close() }
                    },
                    onRecorridoClick = {
                        coroutineScope.launch { drawerState.close() }
                        onRecorridoClick()
                    },
                    onMapaClick = {
                        coroutineScope.launch { drawerState.close() }
                        onMapaClick()
                    },
                    onSettingsClick = {
                        coroutineScope.launch { drawerState.close() }
                        onSettingsClick()
                    },
                    onSupportClick = {
                        coroutineScope.launch { drawerState.close() }
                        onSupportClick()
                    },
                    onLogoutClick = {
                        coroutineScope.launch { drawerState.close() }
                        onLogoutClick()
                    }
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.cream)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(
                        start = AppSpacing.pageHorizontal,
                        end = AppSpacing.pageHorizontal,
                        top = 16.dp,
                        bottom = AppSpacing.sm
                    )
            ) {
                // Encabezado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comunicación",
                        style = AppTextStyles.waykiHeader,
                        color = AppColors.ink
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón "nuevo grupo"
                        val newGroupSource = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(AppColors.primary, CircleShape)
                                .clickable(
                                    interactionSource = newGroupSource,
                                    indication = ripple(color = AppColors.cream.copy(alpha = 0.12f))
                                ) { onNewGroupClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FeatherIcons.Plus,
                                contentDescription = "Nuevo grupo",
                                tint = AppColors.cream,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Icon(
                            imageVector = FeatherIcons.Menu,
                            contentDescription = "Menu",
                            tint = AppColors.ink,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { coroutineScope.launch { drawerState.open() } }
                        )
                        Icon(
                            imageVector = FeatherIcons.Bell,
                            contentDescription = "Notificaciones",
                            tint = AppColors.ink,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onNotificationsClick() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Estado de conectados
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(AppColors.secondary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "12 conductores conectados · 4 conversaciones activas",
                        style = AppTextStyles.waykiMini.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = AppColors.slate
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Barra de Buscar (funcional)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(AppColors.secondary, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = FeatherIcons.Search,
                            contentDescription = "Buscar",
                            tint = AppColors.slate,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = vm::onQueryChange,
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 13.sp,
                                    color = AppColors.ink
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(AppColors.ink),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Buscar",
                                            style = AppTextStyles.waykiMini.copy(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Normal
                                            ),
                                            color = AppColors.slate
                                        )
                                    }
                                    inner()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Categorías (Todos, Grupos, Privados)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val categories = listOf("Todos", "Grupos", "Privados")
                    categories.forEachIndexed { index, category ->
                        val isSelected = selectedCategory == index
                        val bgColor = if (isSelected) AppColors.primary else AppColors.secondary
                        val textColor = if (isSelected) AppColors.cream else AppColors.ink

                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(bgColor)
                                .clickable { vm.onCategoryChange(index) }
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                style = AppTextStyles.labelSmall.copy(
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                ),
                                color = textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lista de conversaciones
                if (isLoading) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(3) {
                            WaykiSkeleton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp),
                                shape = RoundedCornerShape(24.dp)
                            )
                        }
                    }
                } else if (filteredConversations.isEmpty()) {
                    // Estado vacío
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "No hay conversaciones",
                                style = AppTextStyles.waykiLabelMedium,
                                color = AppColors.slate
                            )
                            Text(
                                text = "Cuando recibas mensajes, aparecerán aquí.",
                                style = AppTextStyles.waykiMini,
                                color = AppColors.slate,
                                modifier = Modifier.padding(horizontal = 32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        itemsIndexed(filteredConversations, key = { _, it -> it.id }) { index, item ->
                            ConversationCard(
                                item = item,
                                index = index,
                                onLongClick = { menuFor = item },
                                onClick = { onConversationClick(item.id) }
                            )
                        }
                    }
                }
            }

            // Barra de Navegación Inferior fija
            WaykiBottomNav(
                selectedTab = 1, // "Comunicación" activo
                onTabSelected = onTabSelected,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // ── Menú contextual (mantener presionada una conversación) ──────────
    menuFor?.let { conversation ->
        ConversationMenuSheet(
            conversation = conversation,
            onDismiss = { menuFor = null },
            onAction = { action ->
                when (action) {
                    ConversationAction.CLEAR -> {
                        onClearChat(conversation.id)
                        menuFor = null
                    }
                    ConversationAction.DELETE_GROUP,
                    ConversationAction.DELETE_CHAT -> {
                        // Pide confirmación antes de borrar.
                        menuFor = null
                        confirmDelete = conversation
                    }
                }
            }
        )
    }

    // ── Confirmación al eliminar ────────────────────────────────────────
    confirmDelete?.let { conversation ->
        WaykiConfirmDialog(
            title = if (conversation.isGroup) "Eliminar grupo" else "Eliminar chat",
            message = if (conversation.isGroup) {
                "Se eliminará \"${conversation.title}\" para todos los conductores."
            } else {
                "Se eliminará la conversación con \"${conversation.title}\" de tu lista."
            },
            confirmLabel = "Eliminar",
            onConfirm = {
                onDeleteChat(conversation.id)
                confirmDelete = null
            },
            onDismiss = { confirmDelete = null }
        )
    }
}

@Composable
private fun ConversationCard(
    item: Conversation,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    /** Índice en la lista: escalona la entrada de cada tarjeta. */
    index: Int = 0
) {
    // Entrada escalonada: cada tarjeta aparece un poco después que la anterior.
    val appear = remember(index) {
        Animatable(0f)
    }
    LaunchedEffect(index) {
        // 60 ms de retraso por tarjeta: se ve "en cascada", no de golpe.
        delay(index * 60L)
        appear.animateTo(
            targetValue = 1f,
            animationSpec = tween(320, easing = LinearOutSlowInEasing)
        )
    }

    val interactionSource = remember { MutableInteractionSource() }
    val highlighted = item.unreadCount > 0
    val cardBg = if (highlighted) AppColors.mauve else AppColors.cream
    val cardBorder = if (highlighted) {
        Modifier
    } else {
        Modifier.border(1.dp, AppColors.mauve, RoundedCornerShape(24.dp))
    }
    val titleColor = if (highlighted) AppColors.cream else AppColors.ink
    val previewColor = if (highlighted) AppColors.cream.copy(alpha = 0.85f) else AppColors.ink

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value
                translationY = 24.dp.toPx() * (1f - appear.value)
            }
            .clip(RoundedCornerShape(24.dp))
            .then(cardBorder)
            .background(cardBg, RoundedCornerShape(24.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(color = AppColors.primary.copy(alpha = 0.12f)),
                onLongClick = onLongClick,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WaykiAvatar(
                text = item.avatarText,
                size = 48.dp,
                dark = item.avatarDark,
                online = item.members.firstOrNull { it.userId != "me" }?.isOnline
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Cabecera: nombre + hora + contador
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = AppTextStyles.waykiLabelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = titleColor,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formatListTime(item.lastMessageAtMillis),
                            style = AppTextStyles.waykiMini.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = if (highlighted) AppColors.cream.copy(alpha = 0.8f) else AppColors.slate
                        )

                        if (item.unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(AppColors.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.unreadCount.toString(),
                                    style = AppTextStyles.waykiMini.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = AppColors.cream
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Vista previa del último mensaje
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(AppColors.secondary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FeatherIcons.MessageCircle,
                            contentDescription = null,
                            tint = if (highlighted) AppColors.cream else AppColors.ink,
                            modifier = Modifier.size(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = item.preview,
                        style = AppTextStyles.waykiMini.copy(
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = previewColor,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

/** Hora del listado: "08:39". */
private fun formatListTime(millis: Long): String {
    val totalMinutes = (millis / 60000) % (24 * 60)
    val hour = (totalMinutes / 60).toInt()
    val minute = (totalMinutes % 60).toInt()
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}
