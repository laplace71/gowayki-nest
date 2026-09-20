package com.gowayki.nesh

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.gowayki.nesh.app.router.AppRootHost
import com.gowayki.nesh.core.theme.NeshTheme

@Composable
@Preview
fun App() {
    NeshTheme {
        AppRootHost()
    }
}
