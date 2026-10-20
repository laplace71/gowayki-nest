package com.gowayki.nesh

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gowayki.nesh.app.di.AppDi
import com.gowayki.nesh.app.features.sos.domain.model.AndroidSosContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        AppDi.start()
        // SOS BLE+GPS: registra contexto para los actual Android (sin tocar AppDi).
        AndroidSosContext.appContext = applicationContext
        requestSosPermissions()

        setContent {
            App()
        }
    }

    // Permisos runtime BLE + ubicación (necesarios para scan BLE en Android 6+ y 12+).
    private fun requestSosPermissions() {
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= 31) {
                add(android.Manifest.permission.BLUETOOTH_SCAN)
                add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
            add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 9001)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}