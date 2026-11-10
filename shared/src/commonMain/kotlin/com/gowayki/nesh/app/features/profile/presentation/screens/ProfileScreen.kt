// Perfil del conductor: foto (galería), datos y acción para cambiarla.
// La foto se guarda en el almacenamiento interno de la app y se refleja en
// Configuración y en el resto de la interfaz.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.profile.presentation.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.platform.pickImageFromGallery
import com.gowayki.nesh.app.common.ui.shared.WaykiAvatar
import com.gowayki.nesh.app.common.ui.shared.ToastManager
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.session.TurnoSession
import com.gowayki.nesh.core.session.UserSession
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import com.gowayki.nesh.data.repository.DisplayNameState
import com.gowayki.nesh.data.repository.ProfilePhotoState
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Camera
import compose.icons.feathericons.Trash2
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
) {
    val photoPath by ProfilePhotoState.path
    var pickingPhoto by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(DisplayNameState.value.value ?: "") }

    LaunchedEffect(Unit) { TurnoSession.refresh() }

    fun pickPhoto() {
        if (pickingPhoto) return
        pickingPhoto = true
        scope.launch {
            val path = runCatching { pickImageFromGallery() }.getOrNull()
            pickingPhoto = false
            if (path != null) {
                ProfilePhotoState.update(path)
                ToastManager.show("Foto de perfil actualizada")
            }
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
                    Icon(
                        imageVector = FeatherIcons.ArrowLeft,
                        contentDescription = "Volver",
                        tint = AppColors.cream,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Perfil",
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                    color = AppColors.cream
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(AppColors.mauve.copy(alpha = 0.35f))
                        .clickable(enabled = !pickingPhoto) { pickPhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    WaykiAvatar(
                        text = UserSession.displayInitials,
                        size = 104.dp,
                        dark = false,
                        photoPath = photoPath
                    )
                    if (pickingPhoto) {
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .background(AppColors.ink.copy(alpha = 0.45f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = AppColors.cream,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = UserSession.displayName,
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                    color = AppColors.ink,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (TurnoSession.hasTurno) {
                        "${TurnoSession.lineaNombre} · ${TurnoSession.unidadTexto}"
                    } else {
                        "Sin unidad asignada"
                    },
                    style = AppTextStyles.waykiMini.copy(fontSize = 13.sp),
                    color = AppColors.slate,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                TextButton(onClick = {
                    nameInput = DisplayNameState.value.value ?: UserSession.currentUser?.name?.takeIf { !it.contains("@") } ?: ""
                    showNameDialog = true
                }) {
                    Text(
                        text = "Cambiar nombre",
                        style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = { pickPhoto() }, enabled = !pickingPhoto) {
                        Icon(
                            imageVector = FeatherIcons.Camera,
                            contentDescription = null,
                            tint = AppColors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (pickingPhoto) "Abriendo galería…" else "Cambiar foto",
                            style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.primary
                        )
                    }
                    if (photoPath != null) {
                        TextButton(onClick = {
                            ProfilePhotoState.update(null)
                            ToastManager.show("Foto de perfil eliminada")
                        }) {
                            Icon(
                                imageVector = FeatherIcons.Trash2,
                                contentDescription = null,
                                tint = AppColors.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Quitar",
                                style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                                color = AppColors.error
                            )
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = AppColors.slate.copy(alpha = 0.3f))

            ProfileInfoRow(label = "Nombre", value = UserSession.displayName)
            ProfileInfoRow(label = "Rol", value = "Conductor de combi")
            ProfileInfoRow(
                label = "Línea",
                value = if (TurnoSession.hasTurno) TurnoSession.lineaNombre else "Sin asignar"
            )
            ProfileInfoRow(
                label = "Unidad",
                value = if (TurnoSession.hasTurno) TurnoSession.unidadTexto else "Sin asignar"
            )
            ProfileInfoRow(
                label = "Placa",
                value = TurnoSession.placa ?: "Sin asignar"
            )
            ProfileInfoRow(
                label = "Teléfono",
                value = UserSession.currentUser?.phone
                    ?.takeIf { it.isNotBlank() }
                    ?.let { "+51 $it" }
                    ?: "+51 987 654 321"
            )

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.pageHorizontal)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.softSage)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Tu foto se muestra en el chat y en la lista de conversaciones. " +
                        "Se guarda solo en este dispositivo.",
                    style = AppTextStyles.waykiMini.copy(fontSize = 12.sp),
                    color = AppColors.slate
                )
            }

            Spacer(Modifier.height(32.dp))
        }

        if (showNameDialog) {
            AlertDialog(
                onDismissRequest = { showNameDialog = false },
                title = { Text("Cambiar nombre", color = AppColors.ink) },
                text = {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Tu nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        DisplayNameState.update(nameInput)
                        showNameDialog = false
                        ToastManager.show("Nombre actualizado")
                    }) { Text("Guardar", color = AppColors.primary, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showNameDialog = false }) { Text("Cancelar", color = AppColors.slate) }
                }
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.pageHorizontal, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = AppTextStyles.waykiMini.copy(fontSize = 13.sp),
            color = AppColors.slate
        )
        Text(
            text = value,
            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
            color = AppColors.ink,
            textAlign = TextAlign.End
        )
    }
}
