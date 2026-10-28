package com.gowayki.nesh

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.gowayki.nesh.app.common.ui.SystemBarsAppearance
import com.gowayki.nesh.app.features.map.presentation.SelectedRouteHolder
import com.gowayki.nesh.app.features.settings.data.ThemeStore
import com.gowayki.nesh.app.router.AppRootHost
import com.gowayki.nesh.core.theme.LocalThemeController
import com.gowayki.nesh.core.theme.NeshTheme
import com.gowayki.nesh.core.theme.ThemeController

@Composable
@Preview
fun App() {
    var isDark by remember { mutableStateOf(ThemeStore.isDark()) }

    LaunchedEffect(Unit) {
        SelectedRouteHolder.restore()
    }

    NeshTheme(darkTheme = isDark) {
        SystemBarsAppearance(isDark)
        CompositionLocalProvider(
            LocalThemeController provides ThemeController(isDark = isDark) { value ->
                isDark = value
                ThemeStore.setDark(value)
            }
        ) {
            AppRootHost()
        }
    }
}
