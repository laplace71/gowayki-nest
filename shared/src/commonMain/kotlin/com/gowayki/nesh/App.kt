package com.gowayki.nesh

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import com.gowayki.nesh.app.features.map.presentation.SelectedRouteHolder
import com.gowayki.nesh.app.router.AppRootHost
import com.gowayki.nesh.core.theme.NeshTheme

@Composable
@Preview
fun App() {
    LaunchedEffect(Unit) {
        SelectedRouteHolder.restore()
    }
    NeshTheme {
        AppRootHost()
    }
}
