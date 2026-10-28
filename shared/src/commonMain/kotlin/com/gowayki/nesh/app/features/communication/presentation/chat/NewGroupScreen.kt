// Pantalla "Nuevo grupo": nombre + selección de conductores.
// Estilo Wayki (ink, mauve, cream, slate).
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.ui.shared.WaykiAvatar
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.app.common.ui.shared.BackHandler
import com.gowayki.nesh.app.common.ui.shared.WaykiPrimaryButton
import com.gowayki.nesh.app.features.communication.domain.model.GroupMember
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.feathericons.Check

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.cream)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Cabecera
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.slate)
                .statusBarsPadding()
                .padding(horizontal = AppSpacing.pageHorizontal)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WaykiBackButton(onClick = onBack)
            Spacer(modifier = Modifier.width(AppSpacing.md))
            Text(
                text = "Nuevo grupo",
                style = AppTextStyles.waykiLabelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = AppColors.cream
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = AppSpacing.pageHorizontal)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Nombre del grupo",
                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                color = AppColors.slate
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo de nombre
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppColors.mauve.copy(alpha = 0.35f))
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                BasicTextField(
                    value = name,
                    onValueChange = onNameChange,
                    textStyle = TextStyle(fontSize = 15.sp, color = AppColors.ink),
                    cursorBrush = SolidColor(AppColors.ink),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (name.isEmpty()) {
                            Text(
                                text = "Ej: Operativos Línea 7",
                                style = AppTextStyles.waykiMini.copy(fontSize = 14.sp),
                                color = AppColors.slate
                            )
                        }
                        inner()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (selected.isEmpty()) "CONDUCTORES" else "CONDUCTORES · ${selected.size} ELEGIDOS",
                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                color = AppColors.slate
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(colleagues, key = { it.userId }) { member ->
                    val isSelected = selected.contains(member.userId)
                    val rowSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isSelected) AppColors.mauve.copy(alpha = 0.45f)
                                else androidx.compose.ui.graphics.Color.Transparent
                            )
                            .clickable(
                                interactionSource = rowSource,
                                indication = ripple(color = AppColors.primary.copy(alpha = 0.12f)),
                                onClick = { onToggle(member.userId) }
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                color = AppColors.ink
                            )
                            Text(
                                text = if (member.isOnline) "En línea" else "Desconectado",
                                style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                                color = AppColors.slate
                            )
                        }
                        // Check
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(
                                    if (isSelected) AppColors.primary else androidx.compose.ui.graphics.Color.Transparent,
                                    CircleShape
                                )
                                .then(
                                    if (isSelected) Modifier.clip(CircleShape)
                                    else Modifier.background(
                                        AppColors.slate.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = compose.icons.FeatherIcons.Check,
                                    contentDescription = null,
                                    tint = AppColors.ink,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Botón crear
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.pageHorizontal)
                .padding(bottom = AppSpacing.md)
        ) {
            WaykiPrimaryButton(
                text = if (creating) "Creando..." else "Crear grupo",
                onClick = onCreate,
                fullWidth = true,
                enabled = name.isNotBlank() && selected.isNotEmpty() && !creating,
                showArrow = false,
                isLoading = creating
            )
        }
    }
}
