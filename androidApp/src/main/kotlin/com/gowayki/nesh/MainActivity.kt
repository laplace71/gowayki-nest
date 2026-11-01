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
import com.gowayki.nesh.app.common.platform.AndroidImagePicker
import com.gowayki.nesh.app.common.platform.REQUEST_CODE_PICK_IMAGE
import com.gowayki.nesh.app.common.platform.handleImagePickResult
import com.gowayki.nesh.app.di.AppDi
import com.gowayki.nesh.app.features.sos.domain.model.AndroidSosContext
import com.gowayki.nesh.app.features.sos.domain.model.AndroidAudioContext
import com.gowayki.nesh.app.features.sos.domain.model.REQUEST_CODE_RECORD_AUDIO
import com.gowayki.nesh.app.features.sos.domain.model.REQUEST_CODE_SCREEN_CAPTURE
import com.gowayki.nesh.app.features.sos.domain.model.handleRecordPermissionResult
import com.gowayki.nesh.app.features.sos.domain.model.handleScreenCaptureResult
import java.lang.ref.WeakReference

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        AppDi.start()
        // SOS BLE+GPS: registra contexto para los actual Android (sin tocar AppDi).
        AndroidSosContext.appContext = applicationContext
        // Audio recording: registra contexto para grabación y beeps.
        AndroidAudioContext.appContext = applicationContext
        // El micrófono y la galería se piden desde la Activity (runtime).
        AndroidAudioContext.activity = WeakReference(this)
        AndroidImagePicker.activity = WeakReference(this)
        requestSosPermissions()
        requestAudioPermissions()

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

    // Permiso runtime para grabación de audio (Android 6+).
    private fun requestAudioPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.RECORD_AUDIO), 9002)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    // Completa la solicitud de permiso MediaProjection desde el resultado de la Activity.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            handleScreenCaptureResult(resultCode, data)
        }
        if (requestCode == REQUEST_CODE_PICK_IMAGE) {
            handleImagePickResult(resultCode, data)
        }
    }

    // Permiso de micrófono pedido desde la UI (grabar notas de voz).
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_RECORD_AUDIO) {
            handleRecordPermissionResult(requestCode, grantResults)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}