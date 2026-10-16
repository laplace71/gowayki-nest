package com.gowayki.nesh.app.features.communication.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.ui.shared.WaykiBottomNav
import com.gowayki.nesh.app.features.home.presentation.screens.WaykiSidebarContent
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowDown
import compose.icons.feathericons.Bell
import compose.icons.feathericons.Menu
import compose.icons.feathericons.Search
import kotlinx.coroutines.launch

data class ConversationItemModel(
    val id: String,
    val isGroup: Boolean,
    val avatarText: String,
    val avatarBgDark: Boolean,
    val title: String,
    val time: String,
    val unreadCount: Int,
    val audioDuration: String,
    val audioPreview: String,
    val isHighlighted: Boolean // true: mauve bg, false: cream bg with mauve border
)

@Composable
fun CommunicationScreen(
    onMenuClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onTabSelected: (Int) -> Unit = {},
    onConversationClick: (String) -> Unit = {},
    onSosClick: () -> Unit = {},
    onInicioClick: () -> Unit = {},
    onRecorridoClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf(0) } // 0: Todos, 1: Grupos, 2: Privados
    var searchQuery by remember { mutableStateOf("") }

    val conversations = remember {
        listOf(
            ConversationItemModel(
                id = "1",
                isGroup = true,
                avatarText = "L7",
                avatarBgDark = true,
                title = "GRUPO · Línea 7 · Central",
                time = "08:39",
                unreadCount = 3,
                audioDuration = "00:18",
                audioPreview = "“Tránsito fluido en Av. Dolores. Mantengan la distancia entre unidades.”",
                isHighlighted = true
            ),
            ConversationItemModel(
                id = "2",
                isGroup = false,
                avatarText = "UO8",
                avatarBgDark = false,
                title = "Privado · Unidad 08 · Carlos",
                time = "08:36",
                unreadCount = 1,
                audioDuration = "00:12",
                audioPreview = "“Confirmo paso por Plaza de Armas.”",
                isHighlighted = false
            ),
            ConversationItemModel(
                id = "3",
                isGroup = false,
                avatarText = "LO3",
                avatarBgDark = false,
                title = "Privado · Unidad 03 · Marco",
                time = "08:31",
                unreadCount = 2,
                audioDuration = "00:09",
                audioPreview = "“Llegando a Mercado San Camilo.”",
                isHighlighted = false
            ),
            ConversationItemModel(
                id = "4",
                isGroup = true,
                avatarText = "OP",
                avatarBgDark = false,
                title = "GRUPO · Operativos · Línea 7",
                time = "08:24",
                unreadCount = 4,
                audioDuration = "00:21",
                audioPreview = "“Hay operativo por la av...”",
                isHighlighted = true
            ),
            ConversationItemModel(
                id = "5",
                isGroup = false,
                avatarText = "PR",
                avatarBgDark = false,
                title = "Privado · Jefe de línea",
                time = "08:18",
                unreadCount = 1,
                audioDuration = "00:07",
                audioPreview = "“Revisa el cumplimiento de la unidad 08.”",
                isHighlighted = false
            )
        )
    }

    val filteredConversations = remember(selectedCategory, searchQuery) {
        conversations.filter { item ->
            val matchesCategory = when (selectedCategory) {
                1 -> item.isGroup
                2 -> !item.isGroup
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.audioPreview.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

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
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                            .background(Color(0xFFC4BDDE), CircleShape)
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

                // Barra de Buscar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(AppColors.mauve, RoundedCornerShape(20.dp))
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
                        Text(
                            text = "Buscar",
                            style = AppTextStyles.waykiMini.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = AppColors.slate
                        )
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
                        val bgColor = if (isSelected) AppColors.ink else AppColors.mauve
                        val textColor = if (isSelected) AppColors.cream else AppColors.ink

                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(bgColor)
                                .clickable { selectedCategory = index }
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
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(filteredConversations, key = { it.id }) { item ->
                        ConversationCard(
                            item = item,
                            onClick = { onConversationClick(item.id) }
                        )
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
}

@Composable
private fun ConversationCard(
    item: ConversationItemModel,
    onClick: () -> Unit
) {
    val cardBg = if (item.isHighlighted) AppColors.mauve else AppColors.cream
    val cardBorder = if (item.isHighlighted) {
        Modifier
    } else {
        Modifier.border(1.dp, AppColors.mauve, RoundedCornerShape(24.dp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(cardBorder)
            .background(cardBg, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar
            val avatarBg = if (item.avatarBgDark) AppColors.ink else AppColors.mauve
            val avatarTextColor = if (item.avatarBgDark) AppColors.cream else AppColors.ink

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(avatarBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.avatarText,
                    style = AppTextStyles.waykiButtonSmall.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = avatarTextColor
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Contenido central y derecho
            Column(modifier = Modifier.weight(1f)) {
                // Cabecera: Nombre + Hora + Contador
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
                        color = AppColors.ink,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.time,
                            style = AppTextStyles.waykiMini.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = AppColors.slate
                        )

                        if (item.unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(AppColors.ink, CircleShape),
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

                // Mensaje con icono de audio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(AppColors.ink, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FeatherIcons.ArrowDown,
                            contentDescription = "Audio",
                            tint = AppColors.cream,
                            modifier = Modifier.size(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Audio · ${item.audioDuration} · ${item.audioPreview}",
                        style = AppTextStyles.waykiMini.copy(
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = AppColors.ink
                    )
                }
            }
        }
    }
}
