// Pantalla de información de chat/contacto o grupo (estilo WhatsApp).
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.communication.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.platform.pickImageFromGallery
import com.gowayki.nesh.app.common.ui.shared.WaykiAvatar
import com.gowayki.nesh.app.features.communication.domain.model.GroupMember
import com.gowayki.nesh.core.session.UserSession
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Camera
import compose.icons.feathericons.UserPlus
import kotlinx.coroutines.launch

@Composable
fun ChatInfoScreen(
    title: String,
    isGroup: Boolean,
    memberCount: Int,
    members: List<GroupMember> = emptyList(),
    photoPath: String? = null,
    miRol: String? = null,
    createdBy: String? = null,
    hasLeft: Boolean = false,
    subrouteLabel: String? = null,
    colleagues: List<GroupMember> = emptyList(),
    onBack: () -> Unit = {},
    onChangePhoto: (String) -> Unit = {},
    onAddMembers: (List<GroupMember>) -> Unit = {},
    onRefreshColleagues: () -> Unit = {},
    onAssignRole: (String, String) -> Unit = { _, _ -> },
) {
    var pickingPhoto by remember { mutableStateOf(false) }
    var showAddMembers by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val effectiveCount = if (memberCount > 0) memberCount else members.size
    val miId = UserSession.currentUserId
    // El creador y cualquier ADMIN pueden invitar y asignar roles. Un ADMIN
    // heredado hereda también este permiso, y puede pasarlo a otros.
    val puedeGestionar = !hasLeft && (miRol == "ADMIN" || (createdBy != null && createdBy == miId))

    fun pickPhoto() {
        if (pickingPhoto) return
        pickingPhoto = true
        scope.launch {
            val path = runCatching { pickImageFromGallery() }.getOrNull()
            pickingPhoto = false
            if (path != null) onChangePhoto(path)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.cream)
    ) {
        Surface(modifier = Modifier.fillMaxWidth(), color = AppColors.primary) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(60.dp)
                    .padding(start = 4.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(FeatherIcons.ArrowLeft, contentDescription = "Volver", tint = AppColors.cream, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (isGroup) "Info. del grupo" else "Info. del contacto",
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                    color = AppColors.cream,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(AppColors.mauve.copy(alpha = 0.35f))
                        .clickable(enabled = !pickingPhoto) { pickPhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    WaykiAvatar(
                        text = title.take(2).uppercase(),
                        size = 96.dp,
                        dark = false,
                        photoPath = photoPath,
                    )
                    if (pickingPhoto) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(AppColors.ink.copy(alpha = 0.45f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = AppColors.cream,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = title,
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                    color = AppColors.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(4.dp))
                val subtitle = when {
                    !isGroup -> "Contacto"
                    hasLeft -> "Grupo · ya no participas"
                    miRol == "ADMIN" -> "Grupo · ${if (memberCount > 0) memberCount else members.size} miembros · eres administrador"
                    else -> "Grupo · ${if (memberCount > 0) memberCount else members.size} miembros"
                }
                Text(
                    text = subtitle,
                    style = AppTextStyles.waykiMini.copy(fontSize = 13.sp),
                    color = AppColors.slate,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                val location = when {
                    !subrouteLabel.isNullOrBlank() -> "Subruta: $subrouteLabel"
                    isGroup -> "Grupo global · disponible en todas las subrutas"
                    else -> null
                }
                if (location != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = location,
                        style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                        color = AppColors.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { pickPhoto() }, enabled = !pickingPhoto) {
                    Icon(FeatherIcons.Camera, contentDescription = null, tint = AppColors.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (pickingPhoto) "Abriendo galería…" else "Cambiar foto",
                        style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.primary
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = AppColors.slate.copy(alpha = 0.3f))

            if (isGroup) {
                if (hasLeft) {
                    Text(
                        text = "Saliste de este grupo. Solo puedes ver el historial o eliminar el chat de tu dispositivo.",
                        style = AppTextStyles.waykiMini.copy(fontSize = 12.sp),
                        color = AppColors.slate,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
                Text(
                    text = "Miembros del grupo",
                    style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    color = AppColors.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp)
                )
                if (members.isNotEmpty()) {
                    val myId = UserSession.currentUserId
                    members.forEach { member ->
                        val isYou = myId != null && member.userId == myId
                        MemberRow(
                            name = member.name,
                            initials = member.initials,
                            isYou = isYou,
                            isAdmin = member.rol == "ADMIN",
                            subruta = member.subruta ?: subrouteLabel,
                            onToggleAdmin = if (puedeGestionar && !isYou && member.userId != createdBy) {
                                {
                                    onAssignRole(
                                        member.userId,
                                        if (member.rol == "ADMIN") "MIEMBRO" else "ADMIN"
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                } else {
                    Text(
                        text = "No se pudieron cargar los miembros.",
                        style = AppTextStyles.waykiMini.copy(fontSize = 12.sp),
                        color = AppColors.slate,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                // Invitar es una acción de administración: lo puede el creador
                // o cualquier ADMIN (incluidos los que heredaron el rol).
                if (puedeGestionar) {
                    TextButton(
                        onClick = { showAddMembers = true },
                        modifier = Modifier.padding(start = 10.dp, top = 4.dp)
                    ) {
                        Icon(
                            imageVector = FeatherIcons.UserPlus,
                            contentDescription = null,
                            tint = AppColors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Añadir miembros",
                            style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showAddMembers) {
        AddMembersDialog(
            colleagues = colleagues,
            alreadyIn = members.map { it.userId }.toSet() + listOfNotNull(UserSession.currentUserId),
            onRefresh = onRefreshColleagues,
            onDismiss = { showAddMembers = false },
            onConfirm = { nuevos ->
                showAddMembers = false
                onAddMembers(nuevos)
            },
        )
    }
}

/** Selección de conductores para un grupo que quedó con un solo miembro. */
@Composable
private fun AddMembersDialog(
    colleagues: List<GroupMember>,
    alreadyIn: Set<String>,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (List<GroupMember>) -> Unit,
) {
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    val candidates = colleagues.filterNot { it.userId in alreadyIn }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Añadir miembros",
                style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                color = AppColors.ink,
            )
        },
        text = {
            Column {
                TextButton(onClick = onRefresh) {
                    Text(
                        text = "Actualizar lista de conductores",
                        style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.primary,
                    )
                }
                if (candidates.isEmpty()) {
                    Text(
                        text = "No hay conductores disponibles para añadir.",
                        style = AppTextStyles.waykiMini,
                        color = AppColors.slate,
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(candidates, key = { it.userId }) { member ->
                        val checked = member.userId in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (checked) selected - member.userId else selected + member.userId
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WaykiAvatar(
                                text = member.initials,
                                size = 40.dp,
                                dark = false,
                                online = member.isOnline
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = member.name,
                                style = AppTextStyles.waykiLabelMedium,
                                color = AppColors.ink,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    selected = if (checked) selected - member.userId else selected + member.userId
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AppColors.primary,
                                    checkmarkColor = AppColors.cream,
                                    uncheckedColor = AppColors.slate.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }
        }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(candidates.filter { it.userId in selected }) },
                enabled = selected.isNotEmpty()
            ) {
                Text("Añadir", color = AppColors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = AppColors.slate)
            }
        },
    )
}

@Composable
private fun MemberRow(
    name: String,
    initials: String,
    isYou: Boolean,
    isAdmin: Boolean = false,
    subruta: String? = null,
    onToggleAdmin: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WaykiAvatar(text = initials.ifBlank { name.take(2).uppercase() }, size = 44.dp, dark = false)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.ink,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isAdmin) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(AppColors.primary.copy(alpha = 0.12f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Admin",
                            style = AppTextStyles.waykiMini.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = AppColors.primary
                        )
                    }
                }
            }
            val where = when {
                !subruta.isNullOrBlank() -> if (isYou) "Tú · En $subruta" else "En $subruta"
                isYou -> "Tú"
                else -> null
            }
            if (where != null) {
                Text(where, style = AppTextStyles.waykiMini, color = AppColors.slate, maxLines = 1)
            }
        }
        if (onToggleAdmin != null) {
            TextButton(onClick = onToggleAdmin) {
                Text(
                    text = if (isAdmin) "Quitar admin" else "Hacer admin",
                    style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.primary,
                )
            }
        }
    }
}
