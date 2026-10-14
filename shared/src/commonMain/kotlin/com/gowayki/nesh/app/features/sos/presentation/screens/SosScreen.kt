package com.gowayki.nesh.app.features.sos.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.MapPin

@Composable
fun SosScreen(
    onBack: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFEFEF)) // Placeholder map background
    ) {
        // Map Placeholder elements
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Mapa (Implementado por otro equipo)", color = AppColors.slate)
        }

        // Top Layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            SosHeader(onBack = onBack)
            Spacer(modifier = Modifier.weight(1f))
            SosCard()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SosHeader(onBack: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WaykiBackButton(onClick = onBack)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "S.O.S.",
                        style = AppTextStyles.waykiHeader,
                        color = AppColors.ink
                    )
                    Text(
                        text = "Emergencias cercanas",
                        style = AppTextStyles.waykiLabelMedium,
                        color = AppColors.ink
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(AppColors.cream, CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FeatherIcons.MoreVertical,
                    contentDescription = "Opciones",
                    tint = AppColors.ink,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emergencias activas pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(AppColors.cream, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFC64049), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "2 emergencias activas",
                    style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.ink
                )
            }
            
            // Ultima actualizacion
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = FeatherIcons.RefreshCw,
                    contentDescription = null,
                    tint = AppColors.ink,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Actualizado 08:41",
                    style = AppTextStyles.waykiMini,
                    color = AppColors.ink
                )
            }
        }
    }
}

@Composable
private fun SosCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = AppColors.ink.copy(alpha = 0.2f))
            .background(AppColors.cream, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFC64049), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Emergencia activa",
                        style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFC64049)
                    )
                }
                
                Text(
                    text = "Unidad AQP-017",
                    style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.ink
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(AppColors.mauve, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LQ",
                        style = AppTextStyles.waykiButtonLarge,
                        color = AppColors.ink
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Luis Quispe",
                        style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = AppColors.ink
                    )
                    Text(
                        text = "Transportes El Mirador",
                        style = AppTextStyles.waykiMini,
                        color = AppColors.slate
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = FeatherIcons.MapPin,
                    contentDescription = null,
                    tint = AppColors.slate,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "C. Santa Catalina, cerca de C. Zela\nCercado - Arequipa",
                    style = AppTextStyles.waykiLabelMedium,
                    color = AppColors.slate,
                    lineHeight = 20.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SosStat(label = "Activada 08:37", value = "Hace 4 min")
                SosStat(label = "Desde ti", value = "270 m aprox.")
                SosStat(label = "Radio alerta", value = "250 m")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Datos de muestra - No son emergencias reales",
                style = AppTextStyles.waykiMini.copy(fontSize = 10.sp),
                color = AppColors.slate,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun SosStat(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = AppTextStyles.waykiMini,
            color = AppColors.slate
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold),
            color = AppColors.ink
        )
    }
}
