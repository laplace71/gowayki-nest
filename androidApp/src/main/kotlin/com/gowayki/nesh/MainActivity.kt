package com.gowayki.nesh

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.gowayki.nesh.app.di.AppDi
import com.gowayki.nesh.infra.auth.handleAndroidAuthDeepLink

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        AppDi.start()
        handleAndroidAuthDeepLink(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (AppDi.started) {
            handleAndroidAuthDeepLink(intent)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
