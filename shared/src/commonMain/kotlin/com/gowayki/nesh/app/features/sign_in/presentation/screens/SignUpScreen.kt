package com.gowayki.nesh.app.features.sign_in.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gowayki.nesh.app.common.modules.auth.AuthCopy
import com.gowayki.nesh.app.common.ui.layouts.AuthLayout
import com.gowayki.nesh.app.features.sign_in.domain.models.ProfileGender
import com.gowayki.nesh.app.features.sign_in.presentation.StateFlow.SignInViewModel
import com.gowayki.nesh.app.features.sign_in.presentation.components.AuthCard
import com.gowayki.nesh.core.theme.NeshTheme

@Composable
fun SignUpScreen(
    viewModel: SignInViewModel = viewModel { SignInViewModel() },
) {
    val uiState by viewModel.uiState.collectAsState()

    AuthLayout {
        AuthCard(
            title = AuthCopy.registerTitle,
            subtitle = AuthCopy.registerSubtitle,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!uiState.userEmail.isNullOrBlank()) {
                    Text(
                        text = uiState.userEmail!!,
                        style = NeshTheme.typography.bodySmall,
                        color = NeshTheme.colors.onSurfaceMuted,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(NeshTheme.spacing.md))
                }

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        style = NeshTheme.typography.bodySmall,
                        color = NeshTheme.colors.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                NeshTheme.colors.errorContainer,
                                NeshTheme.radius.sm,
                            )
                            .padding(NeshTheme.spacing.md),
                    )
                    Spacer(modifier = Modifier.height(NeshTheme.spacing.md))
                }

                Text(
                    text = AuthCopy.nameLabel,
                    style = NeshTheme.typography.labelLarge,
                    color = NeshTheme.colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(NeshTheme.spacing.xs))
                BasicTextField(
                    value = uiState.registerName,
                    onValueChange = viewModel::onRegisterNameChange,
                    singleLine = true,
                    textStyle = NeshTheme.typography.bodyMedium.copy(
                        color = NeshTheme.colors.onSurface,
                    ),
                    cursorBrush = SolidColor(NeshTheme.colors.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeshTheme.colors.outline, NeshTheme.radius.input)
                        .background(NeshTheme.colors.surface, NeshTheme.radius.input)
                        .padding(horizontal = NeshTheme.spacing.md, vertical = NeshTheme.spacing.md),
                    decorationBox = { inner ->
                        if (uiState.registerName.isBlank()) {
                            Text(
                                text = AuthCopy.namePlaceholder,
                                style = NeshTheme.typography.bodyMedium,
                                color = NeshTheme.colors.onSurfaceMuted,
                            )
                        }
                        inner()
                    },
                )

                Spacer(modifier = Modifier.height(NeshTheme.spacing.lg))

                Text(
                    text = AuthCopy.genderLabel,
                    style = NeshTheme.typography.labelLarge,
                    color = NeshTheme.colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(NeshTheme.spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NeshTheme.spacing.sm),
                ) {
                    GenderChip(
                        label = AuthCopy.genderMale,
                        selected = uiState.registerGender == ProfileGender.Male,
                        onClick = { viewModel.onRegisterGenderChange(ProfileGender.Male) },
                        modifier = Modifier.weight(1f),
                    )
                    GenderChip(
                        label = AuthCopy.genderFemale,
                        selected = uiState.registerGender == ProfileGender.Female,
                        onClick = { viewModel.onRegisterGenderChange(ProfileGender.Female) },
                        modifier = Modifier.weight(1f),
                    )
                    GenderChip(
                        label = AuthCopy.genderOther,
                        selected = uiState.registerGender == ProfileGender.Other,
                        onClick = { viewModel.onRegisterGenderChange(ProfileGender.Other) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(NeshTheme.spacing.lg))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onAcceptTermsChange(!uiState.acceptTerms) },
                ) {
                    Checkbox(
                        checked = uiState.acceptTerms,
                        onCheckedChange = viewModel::onAcceptTermsChange,
                    )
                    Text(
                        text = AuthCopy.acceptTerms,
                        style = NeshTheme.typography.bodySmall,
                        color = NeshTheme.colors.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(NeshTheme.spacing.xl))

                Button(
                    onClick = viewModel::submitRegistration,
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = NeshTheme.radius.button,
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = NeshTheme.colors.onPrimary,
                        )
                    } else {
                        Text(AuthCopy.saveAndContinue)
                    }
                }

                Spacer(modifier = Modifier.height(NeshTheme.spacing.md))
                Text(
                    text = AuthCopy.signOut,
                    style = NeshTheme.typography.labelMedium,
                    color = NeshTheme.colors.onSurfaceMuted,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable(onClick = viewModel::signOut)
                        .padding(NeshTheme.spacing.sm),
                )
            }
        }
    }
}

@Composable
private fun GenderChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) NeshTheme.colors.primaryContainer else NeshTheme.colors.surface
    val border = if (selected) NeshTheme.colors.primary else NeshTheme.colors.outline
    val fg = if (selected) NeshTheme.colors.onPrimaryContainer else NeshTheme.colors.onSurface

    Box(
        modifier = modifier
            .clip(NeshTheme.radius.sm)
            .border(1.dp, border, NeshTheme.radius.sm)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = NeshTheme.spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = NeshTheme.typography.labelMedium,
            color = fg,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
