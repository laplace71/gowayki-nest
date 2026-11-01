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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Camera
import kotlinx.coroutines.launch

@Composable
fun ChatInfoScreen(
    title: String,
    isGroup: Boolean,
    memberCount: Int,
    members: List<String> = emptyList(),
    photoPath: String? = null,
    onBack: () -> Unit = {},
    onChangePhoto: (String) -> Unit = {},
) {
    var pickingPhoto by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                Text(
                    text = if (isGroup) "${if (memberCount > 0) memberCount else members.size} miembros" else "Contacto",
                    style = AppTextStyles.waykiMini.copy(fontSize = 13.sp),
                    color = AppColors.slate
                )
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
                Text(
                    text = "Miembros del grupo",
                    style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    color = AppColors.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp)
                )
                val list = if (members.isNotEmpty()) members else (1..memberCount.coerceAtLeast(1)).map { "Miembro $it" }
                list.forEachIndexed { index, name ->
                    MemberRow(name = name, isYou = index == 0)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun MemberRow(name: String, isYou: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WaykiAvatar(text = name.take(2).uppercase(), size = 44.dp, dark = false)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold), color = AppColors.ink, maxLines = 1)
            if (isYou) {
                Text("Tú", style = AppTextStyles.waykiMini, color = AppColors.slate)
            }
        }
    }
}
