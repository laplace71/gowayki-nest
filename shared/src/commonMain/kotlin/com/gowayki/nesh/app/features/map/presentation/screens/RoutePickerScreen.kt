package com.gowayki.nesh.app.features.map.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.ui.shared.WaykiBackButton
import com.gowayki.nesh.app.features.map.domain.model.ArequipaRoutes
import com.gowayki.nesh.app.features.map.presentation.SelectedRouteHolder
import com.gowayki.nesh.core.theme.src.AppTextStyles
import com.gowayki.nesh.core.theme.src.core.AppColors
import com.gowayki.nesh.core.theme.src.core.AppRadius
import com.gowayki.nesh.core.theme.src.core.AppSpacing
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search

@Composable
fun RoutePickerScreen(
    onBack: () -> Unit = {},
    onRouteSelected: () -> Unit = onBack,
) {
    val selected by SelectedRouteHolder.selected.collectAsState()
    var query by remember { mutableStateOf("") }
    val routes = remember {
        ArequipaRoutes.all.filter { it.isSub }
    }
    val filtered = remember(query, routes) {
        if (query.isBlank()) routes
        else routes.filter { it.name.contains(query, ignoreCase = true) || (it.operator?.contains(query, ignoreCase = true) == true) }
    }
    val grouped = remember(filtered) {
        filtered.groupBy { it.parent ?: it.id }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.cream)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = AppSpacing.pageHorizontal, vertical = AppSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WaykiBackButton(onClick = onBack)
                Text(
                    text = "Escoge tu ruta actual de micro",
                    style = AppTextStyles.waykiHeader,
                    color = AppColors.ink
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar línea o ruta", color = AppColors.slate) },
                leadingIcon = { Icon(FeatherIcons.Search, null, tint = AppColors.slate) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.ink,
                    unfocusedBorderColor = AppColors.outline.copy(alpha = 0.4f),
                    focusedContainerColor = AppColors.surface,
                    unfocusedContainerColor = AppColors.surface,
                    cursorColor = AppColors.ink,
                    focusedTextColor = AppColors.ink,
                    unfocusedTextColor = AppColors.ink,
                ),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                grouped.forEach { (parent, subs) ->
                    item(key = "header_$parent") {
                        Text(
                            text = parent,
                            style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = AppColors.slate,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(subs, key = { it.id }) { route ->
                        val isSelected = selected?.id == route.id
                        Card(
                            onClick = {
                                SelectedRouteHolder.select(route)
                                onRouteSelected()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) AppColors.primary.copy(alpha = 0.18f) else AppColors.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(route.color.copy(alpha = 0.25f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = route.name.take(2),
                                        style = AppTextStyles.waykiMini.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                        color = AppColors.ink
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = route.name,
                                        style = AppTextStyles.waykiLabelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                        color = AppColors.ink
                                    )
                                    Text(
                                        text = route.operator ?: "",
                                        style = AppTextStyles.waykiMini.copy(fontSize = 11.sp),
                                        color = AppColors.slate
                                    )
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(AppColors.primary, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}