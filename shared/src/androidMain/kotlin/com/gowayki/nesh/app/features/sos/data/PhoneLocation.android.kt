package com.gowayki.nesh.app.features.sos.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.gowayki.nesh.app.features.reports.domain.model.OsmLatLng

@Composable
actual fun rememberPhoneLocation(enabled: Boolean): PhoneLocationState {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val locationManager = remember {
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    var state by remember { mutableStateOf(PhoneLocationState()) }
    val hasFinePermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarsePermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val hasLocationPermission = hasFinePermission || hasCoarsePermission

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        state = state.copy(
            permissionGranted = granted,
            error = if (granted) null else "Permite la ubicación para compartir el GPS del teléfono.",
        )
    }

    LaunchedEffect(enabled, hasLocationPermission) {
        if (!enabled || hasLocationPermission) return@LaunchedEffect
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    DisposableEffect(enabled, hasLocationPermission, locationManager) {
        if (!enabled || !hasLocationPermission) {
            onDispose { }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    state = state.copy(
                        location = OsmLatLng(location.latitude, location.longitude),
                        permissionGranted = true,
                        error = null,
                        capturedAtEpochMs = location.time,
                        accuracyMeters = location.accuracy,
                    )
                }

                @Deprecated("Deprecated in Android API")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit

                override fun onProviderEnabled(provider: String) {
                    state = state.copy(error = null)
                }

                override fun onProviderDisabled(provider: String) {
                    state = state.copy(error = "Activa la ubicación del teléfono.")
                }
            }
            try {
                val provider = when {
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                        LocationManager.GPS_PROVIDER
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                        LocationManager.NETWORK_PROVIDER
                    else -> null
                }
                if (provider == null) {
                    state = state.copy(
                        permissionGranted = true,
                        error = "Activa la ubicación del teléfono.",
                    )
                } else {
                    locationManager.requestLocationUpdates(provider, 2_000L, 0f, listener, Looper.getMainLooper())
                }
            } catch (security: SecurityException) {
                state = state.copy(
                    permissionGranted = false,
                    error = "No se concedió permiso para leer la ubicación.",
                )
            } catch (unavailable: IllegalArgumentException) {
                state = state.copy(error = "No hay proveedor de ubicación disponible.")
            }
            onDispose {
                locationManager.removeUpdates(listener)
            }
        }
    }

    return state.copy(permissionGranted = hasLocationPermission || state.permissionGranted)
}
