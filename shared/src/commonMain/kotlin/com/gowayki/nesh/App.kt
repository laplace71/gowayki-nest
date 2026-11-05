package com.gowayki.nesh

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.gowayki.nesh.app.common.ui.shared.ToastHost
import com.gowayki.nesh.app.features.map.presentation.SelectedRouteHolder
import com.gowayki.nesh.app.router.AppRootHost
import com.gowayki.nesh.core.theme.NeshTheme
import com.gowayki.nesh.core.theme.ThemeManager

@Composable
@Preview
fun App() {
    val systemDark = isSystemInDarkTheme()
    LaunchedEffect(Unit) {
        SelectedRouteHolder.restore()
    }
    LaunchedEffect(systemDark) {
        ThemeManager.systemDark.value = systemDark
        ThemeManager.sync()
    }
    NeshTheme {
        Box(Modifier.fillMaxSize()) {
            AppRootHost()
            ToastHost(Modifier.align(Alignment.TopCenter).padding(top = 64.dp))
        }
    }
}
