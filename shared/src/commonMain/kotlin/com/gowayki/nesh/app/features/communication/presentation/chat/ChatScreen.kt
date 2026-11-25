// Pantalla de chat de una conversación: burbujas, lista de mensajes y
// campo de escritura. Estilo Wayki (ink, mauve, cream, slate).
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.communication.presentation.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.app.common.ui.shared.BackHandler
import com.gowayki.nesh.app.common.ui.shared.WaykiAvatar
import com.gowayki.nesh.app.common.ui.shared.ToastManager
import com.gowayki.nesh.app.common.util.formatTime
import com.gowayki.nesh.app.features.sos.domain.model.createAudioRecorder
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import com.gowayki.nesh.app.features.communication.domain.model.GroupMember
import com.gowayki.nesh.app.features.communication.domain.model.Message
import com.gowayki.nesh.app.features.communication.domain.model.MessageKind
import com.gowayki.nesh.app.features.communication.domain.model.MessageStatus
import com.gowayki.nesh.app.features.communication.presentation.components.PlaybackWaveform
import com.gowayki.nesh.app.features.communication.presentation.components.VoiceNoteMode
import com.gowayki.nesh.app.features.communication.presentation.components.VoiceNotePanel
import com.gowayki.nesh.app.features.communication.presentation.components.formatVoiceTime
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.CheckSquare
import compose.icons.feathericons.Mic
import compose.icons.feathericons.Archive
import compose.icons.feathericons.Inbox
import compose.icons.feathericons.LogOut
import compose.icons.feathericons.MessageCircle
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.Pause
import compose.icons.feathericons.Play
import compose.icons.feathericons.Send
import compose.icons.feathericons.Trash2
import compose.icons.feathericons.ArrowUp
import compose.icons.feathericons.EyeOff
import compose.icons.feathericons.XCircle
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    title: String,
    isGroup: Boolean,
    memberCount: Int,
    members: List<GroupMember> = emptyList(),
    colleagues: List<GroupMember> = emptyList(),
    messages: List<Message>,
    /** Proveedor del borrador: se lee dentro del composer para no recompensar el chat. */
    draftProvider: () -> String,
    sendingProvider: () -> Boolean = { false },
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    conversationId: String,
    voiceVm: CommunicationVoiceViewModel,
    onBack: () -> Unit,
    photoPath: String? = null,
    onChangePhoto: (String) -> Unit = {},
    onDeleteMessage: (String) -> Unit = {},
    onDeleteMessageForAll: (String) -> Unit = {},
    archived: Boolean = false,
    onArchive: () -> Unit = {},
    onUnarchive: () -> Unit = {},
    onClearChat: () -> Unit = {},
    onHideChat: () -> Unit = {},
    onLeaveGroup: () -> Unit = {},
    onRequestGroupDeletion: () -> Unit = {},
    onCancelGroupDeletion: () -> Unit = {},
    onAddMembers: (List<GroupMember>) -> Unit = {},
    onRefreshColleagues: () -> Unit = {},
    onAssignRole: (String, String) -> Unit = { _, _ -> },
    miRol: String? = null,
    createdBy: String? = null,
    hasLeft: Boolean = false,
    eliminacionPendiente: Boolean = false,
    subrouteLabel: String? = null,
) {
    var showInfo by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var longPressedMessage by remember { mutableStateOf<Message?>(null) }
    var confirmLeaveGroup by remember { mutableStateOf(false) }
    var confirmRequestDeleteGroup by remember { mutableStateOf(false) }
    var confirmCancelDeleteGroup by remember { mutableStateOf(false) }
    var confirmHideChat by remember { mutableStateOf(false) }
    var confirmClearChat by remember { mutableStateOf(false) }
    BackHandler(true) { if (showInfo) showInfo = false else onBack() }
    val listState = rememberLazyListState()
    val audioScope = rememberCoroutineScope()
    val audioPlayer = remember { runCatching { createAudioRecorder() }.getOrNull() }
    var playingPath by remember { mutableStateOf<String?>(null) }
    var playerIsPlaying by remember { mutableStateOf(false) }

    // Aviso de mensajes nuevos mientras el usuario está scrolleando arriba.
    var ultimoVistoId by remember { mutableStateOf<String?>(null) }
    var mensajesNuevos by remember { mutableStateOf(0) }
    var previewNueva by remember { mutableStateOf("") }
    val cercaDelFinal by remember(messages) {
        derivedStateOf {
            val ultimoVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            messages.isEmpty() || ultimoVisible >= messages.lastIndex - 1
        }
    }

    val ultimoMensaje = messages.lastOrNull()
    LaunchedEffect(ultimoMensaje?.id) {
        val id = ultimoMensaje?.id ?: return@LaunchedEffect
        if (ultimoVistoId == null) {
            ultimoVistoId = id
            return@LaunchedEffect
        }
        if (id == ultimoVistoId) return@LaunchedEffect

        val desde = messages.indexOfLast { it.id == ultimoVistoId }
        val cantidad = (messages.lastIndex - desde).coerceAtLeast(1)
        val ultimo = messages.last()
        val texto = when (ultimo.kind) {
            MessageKind.AUDIO -> "Audio · ${formatVoiceTime(ultimo.audioDurationSeconds * 1000L)}"
            else -> ultimo.text
        }
        val autor = if (ultimo.isMine) "Tú" else ultimo.senderName

        if (cercaDelFinal) {
            listState.animateScrollToItem(messages.lastIndex)
            mensajesNuevos = 0
            previewNueva = ""
        } else {
            mensajesNuevos += cantidad
            // Evita "Ana: Ana" cuando el texto alternativo ya es el nombre.
            previewNueva = if (texto == autor) texto else "$autor: $texto"
        }
        ultimoVistoId = id
    }

    LaunchedEffect(cercaDelFinal) {
        if (cercaDelFinal) {
            mensajesNuevos = 0
            previewNueva = ""
            ultimoVistoId = messages.lastOrNull()?.id
        }
    }

    // La reproducción se observa directamente. Antes había un efecto que
    // limpiaba `playingPath` 120 ms después de pulsar play; como MediaPlayer
    // tarda un poco en arrancar, ese retardo apagaba el estado antes de que
    // empezara y la línea de tiempo nunca se movía.
    LaunchedEffect(audioPlayer) {
        audioPlayer?.isPlaying?.collect { playerIsPlaying = it }
    }

    DisposableEffect(audioPlayer) {
        onDispose {
            audioScope.launch {
                runCatching { audioPlayer?.stopPlayback() }
            }
        }
    }

    // Baja al último mensaje solo si el usuario ya está al final (o si es
    // la primera carga). Antes cualquier mensaje nuevo arrastraba la lista
    // y se perdía el punto de lectura del historial.
    var firstScrollDone by remember { mutableStateOf(false) }
    LaunchedEffect(messages.lastOrNull()?.id, messages.size) {
        if (messages.isEmpty()) return@LaunchedEffect
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        val nearBottom = lastVisible >= messages.lastIndex - 1
        if (!firstScrollDone) {
            listState.scrollToItem(messages.lastIndex)
            firstScrollDone = true
        } else if (nearBottom) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    if (showInfo) {
        ChatInfoScreen(
            title = title,
            isGroup = isGroup,
            memberCount = if (members.isNotEmpty()) members.size else memberCount,
            members = members,
            photoPath = photoPath,
            miRol = miRol,
            createdBy = createdBy,
            hasLeft = hasLeft,
            subrouteLabel = subrouteLabel,
            colleagues = colleagues,
            onBack = { showInfo = false },
            onChangePhoto = onChangePhoto,
            onAddMembers = onAddMembers,
            onRefreshColleagues = onRefreshColleagues,
            onAssignRole = onAssignRole,
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.cream)
    ) {
        // ── Cabecera ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.primary)
                .statusBarsPadding()
                .padding(horizontal = AppSpacing.pageHorizontal)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WaykiBackButton(onClick = onBack)

            Spacer(modifier = Modifier.width(AppSpacing.md))

            if (isGroup) {
                WaykiAvatar(
                    text = title.take(2).uppercase(),
                    size = 40.dp,
                    dark = false,
                    photoPath = photoPath,
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f).clickable { showInfo = true }) {
                Text(
                    text = title,
                    style = AppTextStyles.waykiLabelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = AppColors.cream,
                    maxLines = 1
                )
                if (isGroup) {
                    val subtitle = when {
                        hasLeft -> "Ya no participas · $memberCount miembros"
                        miRol == "ADMIN" -> "$memberCount miembros · Eres admin"
                        else -> "$memberCount miembros"
                    }
                    Text(
                        text = subtitle,
                        style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                        color = AppColors.cream.copy(alpha = 0.7f)
                    )
                }
                val location = when {
                    !subrouteLabel.isNullOrBlank() -> "Subruta: $subrouteLabel"
                    isGroup -> "Grupo global · todas las subrutas"
                    else -> null
                }
                if (location != null) {
                    Text(
                        text = location,
                        style = AppTextStyles.waykiMini.copy(fontSize = 10.sp),
                        color = AppColors.cream.copy(alpha = 0.65f),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))
            val menuSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(AppColors.cream.copy(alpha = 0.15f), CircleShape)
                    .clickable(
                        interactionSource = menuSource,
                        indication = ripple(color = AppColors.cream.copy(alpha = 0.2f)),
                        onClick = { showMenu = true }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = compose.icons.FeatherIcons.MoreVertical,
                    contentDescription = "Más opciones",
                    tint = AppColors.cream,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ── Mensajes ──────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = AppSpacing.pageHorizontal, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                // Agrupa mensajes seguidos del mismo emisor (solo en grupos).
                val previous = messages.getOrNull(index - 1)
                val showSender = isGroup && previous?.senderId != message.senderId

                MessageBubble(
                    message = message,
                    showSender = showSender,
                    showAvatar = !message.isMine && showSender,
                    onLongPress = { longPressedMessage = message },
                    audioPlayer = audioPlayer,
                    playingPath = playingPath,
                    onPlayingPathChange = { playingPath = it },
                    playerIsPlaying = playerIsPlaying,
                )
            }
        }

        // ── Campo de escritura (o barra de voz como WhatsApp) ────────────
        if (hasLeft) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.cream)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ya no participas en este grupo. Puedes leer los mensajes y, si quieres, eliminar el chat de tu dispositivo.",
                    style = AppTextStyles.waykiMini.copy(fontSize = 13.sp),
                    color = AppColors.slate,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            // El popup global muestra la grabación bloqueada, la reproducción,
            // la vista previa y los errores. La barra del chat solo se usa mientras
            // se mantiene pulsado el micrófono (sin bloquear), para no pintar dos
            // líneas de tiempo a la vez.
            val hasVoiceState = ShowInlineComposer(voiceVm)
            if (hasVoiceState) {
                val voiceUi = voiceVm.ui
                RecordingComposer(
                    voiceVm = voiceVm,
                    voiceUi = voiceUi,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.cream)
                        .navigationBarsPadding()
                        .imePadding(),
                )
            } else {
                ChatComposerBar(
                    draftProvider = draftProvider,
                    sendingProvider = sendingProvider,
                    onDraftChange = onDraftChange,
                    onSend = onSend,
                    voiceVm = voiceVm,
                    conversationId = conversationId,
                )
            }
        }
    }

    // ── Menú de la conversación ──────────────────────────────────────────
    if (showMenu) {
        val sheetState = androidx.compose.material3.rememberBottomSheetState(
            initialValue = androidx.compose.material3.SheetValue.Expanded,
            enabledValues = setOf(
                androidx.compose.material3.SheetValue.Hidden,
                androidx.compose.material3.SheetValue.Expanded,
            ),
        )
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            sheetState = sheetState,
            containerColor = AppColors.cream,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = title,
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    color = AppColors.ink,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                )
                ChatMenuRow(
                    icon = if (archived) compose.icons.FeatherIcons.Inbox else compose.icons.FeatherIcons.Archive,
                    label = if (archived) "Desarchivar" else "Archivar",
                    onClick = {
                        showMenu = false
                        if (archived) onUnarchive() else onArchive()
                    }
                )
                ChatMenuRow(
                    icon = compose.icons.FeatherIcons.MessageCircle,
                    label = "Limpiar chat (solo para ti)",
                    onClick = { showMenu = false; confirmClearChat = true }
                )
                if (isGroup && !hasLeft) {
                    ChatMenuRow(
                        icon = compose.icons.FeatherIcons.LogOut,
                        label = "Salir del grupo",
                        onClick = { showMenu = false; confirmLeaveGroup = true }
                    )
                    val puedeGestionar = miRol == "ADMIN" || createdBy == com.gowayki.nesh.core.session.UserSession.currentUserId
                    if (puedeGestionar && !eliminacionPendiente) {
                        ChatMenuRow(
                            icon = compose.icons.FeatherIcons.Trash2,
                            label = "Eliminar grupo (definitivo)",
                            destructive = true,
                            onClick = { showMenu = false; confirmRequestDeleteGroup = true }
                        )
                    }
                    if (puedeGestionar && eliminacionPendiente) {
                        ChatMenuRow(
                            icon = compose.icons.FeatherIcons.XCircle,
                            label = "Cancelar eliminación del grupo",
                            onClick = { showMenu = false; confirmCancelDeleteGroup = true }
                        )
                    }
                }
                ChatMenuRow(
                    icon = compose.icons.FeatherIcons.EyeOff,
                    label = "Ocultar chat",
                    destructive = true,
                    onClick = { showMenu = false; confirmHideChat = true }
                )
            }
        }
    }

    // Aviso flotante: puntito + contador + vista previa del último mensaje.
    if (mensajesNuevos > 0) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 104.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(AppColors.primary)
                .clickable {
                    audioScope.launch { listState.animateScrollToItem(messages.lastIndex) }
                    mensajesNuevos = 0
                    previewNueva = ""
                    ultimoVistoId = messages.lastOrNull()?.id
                }
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AppColors.cream),
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "$mensajesNuevos mensaje${if (mensajesNuevos == 1) "" else "s"} nuevo${if (mensajesNuevos == 1) "" else "s"}",
                        style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.cream,
                        maxLines = 1,
                    )
                    if (previewNueva.isNotBlank()) {
                        Text(
                            text = previewNueva,
                            style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                            color = AppColors.cream.copy(alpha = 0.85f),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
    }

    // ── Acciones de un mensaje (mantener presionado) ─────────────────────
    longPressedMessage?.let { msg ->
        val sheetState = androidx.compose.material3.rememberBottomSheetState(
            initialValue = androidx.compose.material3.SheetValue.Expanded,
            enabledValues = setOf(
                androidx.compose.material3.SheetValue.Hidden,
                androidx.compose.material3.SheetValue.Expanded,
            ),
        )
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { longPressedMessage = null },
            sheetState = sheetState,
            containerColor = AppColors.cream,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Mensaje",
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    color = AppColors.ink,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                )
                ChatMenuRow(
                    icon = compose.icons.FeatherIcons.Trash2,
                    label = "Eliminar para mí",
                    onClick = {
                        longPressedMessage = null
                        onDeleteMessage(msg.id)
                    }
                )
                // Solo quien envió el mensaje puede borrarlo para todos; el
                // backend lo valida de nuevo con la identidad del JWT.
                if (msg.isMine) {
                    ChatMenuRow(
                        icon = compose.icons.FeatherIcons.Trash2,
                        label = "Eliminar para todos",
                        destructive = true,
                        onClick = {
                            longPressedMessage = null
                            onDeleteMessageForAll(msg.id)
                        }
                    )
                }
            }
        }
    }

    if (confirmLeaveGroup) {
        val puedeGestionarGrupo = isGroup && (miRol == "ADMIN" || createdBy == com.gowayki.nesh.core.session.UserSession.currentUserId)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmLeaveGroup = false },
            containerColor = AppColors.cream,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = if (puedeGestionarGrupo) "¿Salir y eliminar el grupo?" else "¿Salir del grupo?",
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = AppColors.ink
                )
            },
            text = {
                Text(
                    text = if (puedeGestionarGrupo) {
                        "Si sales del grupo \"$title\", el grupo se eliminará una vez que salgas. Se programará su eliminación para todos los participantes."
                    } else {
                        "Dejarás de participar en \"$title\". El grupo y sus mensajes siguen existiendo para los demás y podrás seguir viendo este chat hasta que lo ocultes."
                    },
                    style = AppTextStyles.waykiMini,
                    color = AppColors.slate
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirmLeaveGroup = false
                    if (puedeGestionarGrupo) onRequestGroupDeletion() else onLeaveGroup()
                }) {
                    Text(if (puedeGestionarGrupo) "Salir y eliminar" else "Salir", color = AppColors.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmLeaveGroup = false }) {
                    Text("Cancelar", color = AppColors.primary)
                }
            }
        )
    }

    if (confirmRequestDeleteGroup) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmRequestDeleteGroup = false },
            containerColor = AppColors.cream,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "¿Eliminar grupo definitivamente?",
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = AppColors.ink
                )
            },
            text = {
                Text(
                    text = "\"$title\" desaparecerá inmediatamente para todos los participantes. Durante las próximas 72 horas podrás cancelar la eliminación; pasado ese plazo se borrarán definitivamente el grupo, sus mensajes y sus archivos.",
                    style = AppTextStyles.waykiMini,
                    color = AppColors.slate
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirmRequestDeleteGroup = false
                    onRequestGroupDeletion()
                }) {
                    Text("Programar eliminación", color = AppColors.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmRequestDeleteGroup = false }) {
                    Text("Cancelar", color = AppColors.primary)
                }
            }
        )
    }

    if (confirmCancelDeleteGroup) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmCancelDeleteGroup = false },
            containerColor = AppColors.cream,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "¿Cancelar la eliminación?",
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = AppColors.ink
                )
            },
            text = {
                Text(
                    text = "El grupo \"$title\" dejará de estar pendiente de eliminación y seguirá disponible para sus participantes.",
                    style = AppTextStyles.waykiMini,
                    color = AppColors.slate
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirmCancelDeleteGroup = false
                    onCancelGroupDeletion()
                }) {
                    Text("Cancelar eliminación", color = AppColors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmCancelDeleteGroup = false }) {
                    Text("Volver", color = AppColors.primary)
                }
            }
        )
    }

    if (confirmHideChat) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmHideChat = false },
            containerColor = AppColors.cream,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "¿Ocultar este chat?",
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = AppColors.ink
                )
            },
            text = {
                Text(
                    text = "Se quitará \"$title\" de tu lista. Solo lo ocultas para ti: no se borran mensajes ni audios y los demás participantes no notan nada. Podrás restaurarlo desde Ocultos.",
                    style = AppTextStyles.waykiMini,
                    color = AppColors.slate
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirmHideChat = false
                    onHideChat()
                }) {
                    Text("Ocultar", color = AppColors.ink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmHideChat = false }) {
                    Text("Cancelar", color = AppColors.primary)
                }
            }
        )
    }

    if (confirmClearChat) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmClearChat = false },
            containerColor = AppColors.cream,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "¿Limpiar este chat?",
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = AppColors.ink
                )
            },
            text = {
                Text(
                    text = "Solo afecta a tu cuenta: ocultarás los mensajes anteriores y el chat saldrá de tu lista, también al reinstalar la app. Los mensajes NO se borran y los demás participantes no notan nada. Si llega un mensaje nuevo, el chat vuelve a aparecer mostrando solo lo nuevo.",
                    style = AppTextStyles.waykiMini,
                    color = AppColors.slate
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirmClearChat = false
                    onClearChat()
                }) {
                    Text("Limpiar chat", color = AppColors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmClearChat = false }) {
                    Text("Cancelar", color = AppColors.primary)
                }
            }
        )
    }
}

@Composable
private fun ChatMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (destructive) AppColors.error else AppColors.ink
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(if (destructive) AppColors.error.copy(alpha = 0.12f) else AppColors.mauve.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
            color = tint
        )
    }
}

/** Burbuja de un mensaje. */
@Composable
private fun MessageBubble(
    message: Message,
    showSender: Boolean,
    showAvatar: Boolean,
    onLongPress: () -> Unit = {},
    audioPlayer: com.gowayki.nesh.app.features.sos.domain.model.AudioRecorder? = null,
    playingPath: String? = null,
    onPlayingPathChange: (String?) -> Unit = {},
    playerIsPlaying: Boolean = false,
) {
    val isMine = message.isMine

    // Avisos de sistema ("salió / se unió"): chip centrado, sin globo ni autor.
    if (message.kind == MessageKind.SYSTEM) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.text.ifBlank { message.senderName },
                style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                color = AppColors.slate,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AppColors.slate.copy(alpha = 0.10f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            ),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine && showAvatar) {
            WaykiAvatar(
                text = message.senderName.take(1).uppercase(),
                size = 28.dp,
                dark = false
            )
            Spacer(modifier = Modifier.width(6.dp))
        } else if (!isMine) {
            Spacer(modifier = Modifier.width(34.dp))
        }

        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
            if (showSender) {
                Text(
                    text = message.senderName,
                    style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                    color = AppColors.slate,
                    modifier = Modifier.padding(start = 4.dp, bottom = 3.dp)
                )
            }

            Box(
                modifier = Modifier
                    // En tablets/landscape un mensaje no debe ocupar todo el
                    // ancho: se limita a una medida de lectura cómoda.
                    .widthIn(max = 520.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (isMine) 20.dp else 6.dp,
                            bottomEnd = if (isMine) 6.dp else 20.dp
                        )
                    )
                    .background(if (isMine) AppColors.primary else AppColors.mauve.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (message.kind == MessageKind.AUDIO) {
                    AudioBubbleContent(
                        message = message,
                        isMine = isMine,
                        audioPlayer = audioPlayer,
                        playingPath = playingPath,
                        onPlayingPathChange = onPlayingPathChange,
                        playerIsPlaying = playerIsPlaying,
                    )
                } else {
                    Text(
                        text = message.text,
                        style = AppTextStyles.waykiLabel.copy(fontSize = 14.sp),
                        color = if (isMine) AppColors.cream else AppColors.ink
                    )
                }
            }

            // Hora + ticks de estado (solo mensajes propios).
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            ) {
                Text(
                    text = formatTime(message.sentAtMillis),
                    style = AppTextStyles.waykiMini.copy(fontSize = 10.sp),
                    color = AppColors.slate
                )
                if (isMine) {
                    Spacer(modifier = Modifier.width(4.dp))
                    when (message.status) {
                        MessageStatus.SENDING -> Icon(
                            imageVector = compose.icons.FeatherIcons.Check,
                            contentDescription = "Enviando",
                            tint = AppColors.slate,
                            modifier = Modifier.size(12.dp)
                        )
                        MessageStatus.DELIVERED -> Icon(
                            imageVector = compose.icons.FeatherIcons.CheckSquare,
                            contentDescription = "Entregado",
                            tint = AppColors.slate,
                            modifier = Modifier.size(12.dp)
                        )
                        MessageStatus.READ -> Icon(
                            imageVector = compose.icons.FeatherIcons.CheckSquare,
                            contentDescription = "Leído",
                            tint = AppColors.ink,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Contenido de una burbuja de audio: botón play real + barra + duración. */
@Composable
private fun AudioBubbleContent(
    message: Message,
    isMine: Boolean,
    audioPlayer: com.gowayki.nesh.app.features.sos.domain.model.AudioRecorder?,
    playingPath: String?,
    onPlayingPathChange: (String?) -> Unit,
    playerIsPlaying: Boolean,
) {
    val contentColor = if (isMine) AppColors.cream else AppColors.ink
    val muted = if (isMine) {
        AppColors.cream.copy(alpha = 0.35f)
    } else {
        AppColors.slate.copy(alpha = 0.35f)
    }
    val accent = if (isMine) AppColors.cream else AppColors.primary
    val audioPath = message.audioFilePath.orEmpty()
    val scope = rememberCoroutineScope()

    // Solo la burbuja activa recolecta la posición. Si todas lo hicieran, cada
    // actualización del reproductor (cada 60 ms) recompondría todos los audios
    // del historial y la lista se pondría lenta.
    val isActiveBubble = playingPath == audioPath && audioPath.isNotBlank()
    var playerPosition by remember { mutableStateOf(0L) }
    var playerDuration by remember { mutableStateOf(0L) }
    LaunchedEffect(audioPlayer, isActiveBubble) {
        if (!isActiveBubble) return@LaunchedEffect
        audioPlayer?.playbackPosition?.collect { playerPosition = it }
    }
    LaunchedEffect(audioPlayer, isActiveBubble) {
        if (!isActiveBubble) return@LaunchedEffect
        audioPlayer?.playbackDuration?.collect { playerDuration = it }
    }

    val isThisPlaying = playerIsPlaying && playingPath == audioPath && audioPath.isNotBlank()
    val totalMillis = if (playingPath == audioPath && playerDuration > 0L) {
        playerDuration
    } else {
        message.audioDurationSeconds * 1000L
    }
    val positionMillis = if (playingPath == audioPath) {
        playerPosition.coerceAtMost(totalMillis)
    } else {
        0L
    }
    val isThisPaused = !playerIsPlaying && audioPath.isNotBlank() &&
        playingPath == audioPath && positionMillis > 0L && positionMillis < totalMillis
    val progress = if (totalMillis > 0L) {
        (positionMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Row(
        modifier = Modifier.widthIn(min = 180.dp, max = 260.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isMine) AppColors.secondary else AppColors.ink)
                .clickable {
                    if (audioPath.isBlank()) {
                        ToastManager.show("Audio de muestra: sin archivo grabado")
                        return@clickable
                    }
                    when {
                        isThisPlaying -> scope.launch { audioPlayer?.pausePlayback() }
                        isThisPaused -> scope.launch { audioPlayer?.resumePlayback() }
                        else -> {
                            onPlayingPathChange(audioPath)
                            scope.launch { audioPlayer?.playAudio(audioPath) }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isThisPlaying) FeatherIcons.Pause else FeatherIcons.Play,
                contentDescription = if (isThisPlaying) "Pausar audio" else "Reproducir audio",
                tint = if (isMine) AppColors.ink else AppColors.cream,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            PlaybackWaveform(
                progress = progress,
                active = isThisPlaying,
                color = accent,
                trackColor = muted,
                height = 30.dp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatVoiceTime(positionMillis),
                    style = AppTextStyles.waykiMini.copy(fontSize = 10.sp),
                    color = contentColor.copy(alpha = 0.8f)
                )
                Text(
                    text = formatVoiceTime(totalMillis),
                    style = AppTextStyles.waykiMini.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = FeatherIcons.Mic,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )
    }
}

/** "14:32" a partir del timestamp. */
/** "18" segundos -> "0:18". */
private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

// ── Grabación tipo WhatsApp para Comunicación ────────────────────────────────
// Pulsa el micrófono para iniciar la grabación y pulsa detener para
// previsualizarla. Desde la barra puedes reproducir, descartar y enviar.

@Composable
private fun VoiceRecordingButton(
    voiceVm: CommunicationVoiceViewModel,
    conversationId: String,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(AppColors.mauve, CircleShape)
            .clickable { voiceVm.toggleRecording(conversationId) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = compose.icons.FeatherIcons.Mic,
            contentDescription = "Grabar nota de voz",
            tint = AppColors.cream,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Lee el estado de voz en su propio alcance: si se leyera directo en el cuerpo
 * de ChatScreen, cada tick del reproductor (cada 60 ms) recompondría todo el
 * chat. Con esta función solo se recompone cuando el valor cambia de verdad.
 */
@Composable
private fun ShowInlineComposer(voiceVm: CommunicationVoiceViewModel): Boolean {
    val ui = voiceVm.ui
    return ui.isRecording && !ui.isLocked
}

/**
 * Campo de texto y botón de envío en su propio alcance. El borrador se lee a
 * través del proveedor para que, al teclear, no se recomponga toda la pantalla
 * ni la lista de mensajes (cada tecla cambiaba `draft` y arrastraba el chat).
 */
@Composable
private fun ChatComposerBar(
    draftProvider: () -> String,
    sendingProvider: () -> Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    voiceVm: CommunicationVoiceViewModel,
    conversationId: String,
) {
    val draft = draftProvider()
    val sending = sendingProvider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.cream)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        BasicTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.mauve.copy(alpha = 0.35f))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = AppColors.ink
            ),
            cursorBrush = SolidColor(AppColors.ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            maxLines = 4,
            decorationBox = { inner ->
                if (draft.isEmpty()) {
                    Text(
                        text = "Escribe un mensaje",
                        style = AppTextStyles.waykiMini.copy(fontSize = 14.sp),
                        color = AppColors.slate,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                inner()
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Botón de envío o de nota de voz.
        val hasText = draft.isNotBlank()
        if (hasText) {
            val sendSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(AppColors.primary, CircleShape)
                    .clickable(
                        interactionSource = sendSource,
                        indication = ripple(color = AppColors.cream.copy(alpha = 0.2f)),
                        enabled = !sending,
                        onClick = onSend,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = compose.icons.FeatherIcons.Send,
                    contentDescription = "Enviar",
                    tint = AppColors.cream,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            VoiceRecordingButton(
                voiceVm = voiceVm,
                conversationId = conversationId,
            )
        }
    }
}

@Composable
private fun RecordingComposer(
    voiceVm: CommunicationVoiceViewModel,
    voiceUi: CommunicationVoiceUiState,
    modifier: Modifier = Modifier,
) {
    val hasRecording = voiceUi.lastRecordedFilePath != null
    val mode = when {
        voiceUi.isRecording -> VoiceNoteMode.RECORDING
        voiceUi.isPlaying -> VoiceNoteMode.PLAYING
        hasRecording -> VoiceNoteMode.PREVIEW
        else -> VoiceNoteMode.ERROR
    }
    val totalMillis = if (hasRecording) {
        maxOf(voiceUi.recordedDurationMillis, voiceUi.playbackDuration)
    } else {
        0L
    }

    VoiceNotePanel(
        mode = mode,
        statusText = when {
            voiceUi.isRecording -> "Grabando nota de voz"
            voiceUi.isPlaying -> "Reproduciendo nota de voz"
            hasRecording -> "Nota de voz lista"
            else -> "No se pudo grabar"
        },
        elapsedMillis = voiceUi.duration,
        totalMillis = totalMillis,
        positionMillis = voiceUi.playbackPosition,
        error = voiceUi.audioError,
        onCancel = { voiceVm.cancel() },
        onStop = { voiceVm.stopAndSave() },
        onPlayPause = { voiceVm.togglePlayback() },
        onSeek = { voiceVm.seekTo(it) },
        onSend = { voiceVm.sendRecorded() },
        onDiscard = { voiceVm.discardRecorded() },
        onDismissError = { voiceVm.clearError() },
        compact = true,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

