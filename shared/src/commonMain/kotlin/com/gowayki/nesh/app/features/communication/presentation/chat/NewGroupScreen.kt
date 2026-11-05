// Pantalla "Nuevo grupo": nombre + selección de conductores.
// Construida con componentes Material Design 3 (Scaffold, OutlinedTextField,
// Checkbox y Button) sobre la paleta Wayki.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.communication.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.ui.shared.BackHandler
import com.gowayki.nesh.app.common.ui.shared.WaykiAvatar
import com.gowayki.nesh.app.features.communication.domain.model.GroupMember
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.X

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupScreen(
    name: String,
    colleagues: List<GroupMember>,
    selected: Set<String>,
    creating: Boolean,
    onNameChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    onCreate: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(true) { onBack() }
    val selectedMembers = colleagues.filter { selected.contains(it.userId) }
    val canCreate = name.isNotBlank() && selected.isNotEmpty() && !creating

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppColors.cream,
        topBar = {
            Surface(modifier = Modifier.fillMaxWidth(), color = AppColors.primary) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(64.dp)
                        .padding(start = 4.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = FeatherIcons.ArrowLeft,
                            contentDescription = "Volver",
                            tint = AppColors.cream,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nuevo grupo",
                            style = AppTextStyles.waykiLabelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = AppColors.cream,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (selected.isEmpty()) "Añade participantes"
                            else "${selected.size} seleccionado${if (selected.size == 1) "" else "s"}",
                            style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                            color = AppColors.cream.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = AppColors.cream, shadowElevation = 8.dp) {
                Button(
                    onClick = onCreate,
                    enabled = canCreate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = AppSpacing.pageHorizontal, vertical = 12.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.primary,
                        contentColor = AppColors.cream,
                        disabledContainerColor = AppColors.primary.copy(alpha = 0.35f),
                        disabledContentColor = AppColors.cream.copy(alpha = 0.7f)
                    )
                ) {
                    if (creating) {
                        CircularProgressIndicator(
                            color = AppColors.cream,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = if (creating) "Creando…" else "Crear grupo",
                        style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = AppSpacing.pageHorizontal)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ---------- Nombre del grupo (Material Design) ----------
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre del grupo") },
                placeholder = { Text("Ej: Operativos Línea 7") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = AppTextStyles.waykiLabelMedium.copy(color = AppColors.ink),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.ink,
                    unfocusedTextColor = AppColors.ink,
                    focusedContainerColor = AppColors.cream,
                    unfocusedContainerColor = AppColors.cream,
                    cursorColor = AppColors.primary,
                    focusedBorderColor = AppColors.primary,
                    unfocusedBorderColor = AppColors.slate.copy(alpha = 0.5f),
                    focusedLabelColor = AppColors.primary,
                    unfocusedLabelColor = AppColors.slate,
                    focusedPlaceholderColor = AppColors.slate.copy(alpha = 0.7f),
                    unfocusedPlaceholderColor = AppColors.slate.copy(alpha = 0.7f)
                )
            )

            // ---------- Participantes elegidos ----------
            if (selectedMembers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedMembers.forEach { member ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(AppColors.primary.copy(alpha = 0.12f))
                                .padding(start = 6.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(AppColors.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = member.initials.take(1),
                                    style = AppTextStyles.waykiMini.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = AppColors.cream
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = member.name,
                                style = AppTextStyles.waykiMini.copy(fontSize = 12.sp),
                                color = AppColors.ink,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = FeatherIcons.X,
                                contentDescription = "Quitar ${member.name}",
                                tint = AppColors.slate,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onToggle(member.userId) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Contactos",
                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                color = AppColors.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            HorizontalDivider(
                thickness = 0.5.dp,
                color = AppColors.slate.copy(alpha = 0.3f)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
            ) {
                items(colleagues, key = { it.userId }) { member ->
                    val isSelected = selected.contains(member.userId)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(member.userId) }
                            .heightIn(min = 64.dp)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WaykiAvatar(
                            text = member.initials,
                            size = 44.dp,
                            dark = false,
                            online = member.isOnline
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member.name,
                                style = AppTextStyles.waykiLabelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = AppColors.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (member.isOnline) "En línea" else "Desconectado",
                                style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                                color = AppColors.slate
                            )
                        }
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggle(member.userId) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AppColors.primary,
                                checkmarkColor = AppColors.cream,
                                uncheckedColor = AppColors.slate.copy(alpha = 0.6f)
                            )
                        )
                    }
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = AppColors.slate.copy(alpha = 0.2f),
                        modifier = Modifier.padding(start = 58.dp)
                    )
                }
            }
        }
    }
}
