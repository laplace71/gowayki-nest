package com.gowayki.nesh

import androidx.compose.ui.window.ComposeUIViewController
import com.gowayki.nesh.app.di.AppDi

fun MainViewController() = ComposeUIViewController {
    AppDi.start()
    App()
}
