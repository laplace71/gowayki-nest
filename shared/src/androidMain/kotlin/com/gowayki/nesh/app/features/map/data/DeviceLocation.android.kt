@file:Suppress("MissingPermission")
package com.gowayki.nesh.app.features.map.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.WindowManager
import com.gowayki.nesh.app.features.map.domain.model.UserLocation
import com.gowayki.nesh.app.features.sos.domain.model.AndroidSosContext
import kotlin.math.abs
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

private class AndroidDeviceLocationSource(
    private val context: Context,
) : DeviceLocationSource {

    override fun locations(): Flow<UserLocation> = callbackFlow {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (manager == null) {
            close()
            return@callbackFlow
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        val orientationSensor = if (rotationSensor == null) {
            sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        } else null
        val accelSensor = if (rotationSensor == null && orientationSensor == null) {
            sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } else null
        val magSensor = if (rotationSensor == null && orientationSensor == null) {
            sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        } else null

        var currentAzimuth: Float? = null
        var lastEmittedBearing: Float? = null
        var lastLocation: Location? = null
        var lastLocationTime: Long = 0L
        var lastUserLocation: UserLocation? = null
        var gravityVals: FloatArray? = null
        var geoVals: FloatArray? = null

        fun buildUserLocation(loc: Location): UserLocation {
            val rawSpeed = if (loc.hasSpeed()) loc.speed else null
            val speed: Float = if (rawSpeed != null && rawSpeed >= 0f) {
                if (rawSpeed < 0.3f) 0f else rawSpeed
            } else {
                val prev = lastLocation
                val prevTime = lastLocationTime
                if (prev != null && prevTime > 0L) {
                    val dist = prev.distanceTo(loc)
                    val dt = (loc.time - prevTime) / 1000f
                    if (dist < 2.5f) {
                        0f
                    } else if (dt in 0.5f..10f) {
                        dist / dt
                    } else {
                        0f
                    }
                } else {
                    0f
                }
            }

            val bearing: Float? = if (loc.hasBearing() && speed >= 1.5f) {
                loc.bearing
            } else if (currentAzimuth != null) {
                currentAzimuth
            } else if (loc.hasBearing()) {
                loc.bearing
            } else {
                lastEmittedBearing
            }

            lastLocation = loc
            lastLocationTime = loc.time
            if (bearing != null) {
                lastEmittedBearing = bearing
            }

            val userLoc = UserLocation(
                lat = loc.latitude,
                lon = loc.longitude,
                bearingDeg = bearing,
                speedMps = speed,
                accuracyM = if (loc.hasAccuracy()) loc.accuracy else null,
            )
            lastUserLocation = userLoc
            return userLoc
        }

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                var azimuth: Float? = null
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR ||
                    event.sensor.type == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR
                ) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val remappedMatrix = FloatArray(9)
                    val displayRotation = runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            context.display?.rotation
                        } else {
                            (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.rotation
                        }
                    }.getOrNull() ?: Surface.ROTATION_0

                    when (displayRotation) {
                        Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
                            rotationMatrix,
                            SensorManager.AXIS_Y,
                            SensorManager.AXIS_MINUS_X,
                            remappedMatrix
                        )
                        Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(
                            rotationMatrix,
                            SensorManager.AXIS_MINUS_X,
                            SensorManager.AXIS_MINUS_Y,
                            remappedMatrix
                        )
                        Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
                            rotationMatrix,
                            SensorManager.AXIS_MINUS_Y,
                            SensorManager.AXIS_X,
                            remappedMatrix
                        )
                        else -> System.arraycopy(rotationMatrix, 0, remappedMatrix, 0, 9)
                    }

                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(remappedMatrix, orientation)
                    var deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    if (deg < 0f) deg += 360f
                    deg %= 360f
                    azimuth = deg
                } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
                    var deg = event.values[0]
                    if (deg < 0f) deg += 360f
                    deg %= 360f
                    azimuth = deg
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    gravityVals = event.values.clone()
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    geoVals = event.values.clone()
                }

                if (azimuth == null && gravityVals != null && geoVals != null) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, gravityVals, geoVals)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        var deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        if (deg < 0f) deg += 360f
                        deg %= 360f
                        azimuth = deg
                    }
                }

                if (azimuth != null) {
                    currentAzimuth = azimuth
                    val lastBearing = lastEmittedBearing
                    val delta = if (lastBearing == null) 360f else abs(((azimuth - lastBearing + 180f) % 360f) - 180f)
                    if (delta >= 2.0f) {
                        lastEmittedBearing = azimuth
                        val currentLoc = lastUserLocation
                        if (currentLoc != null) {
                            val updated = currentLoc.copy(bearingDeg = azimuth)
                            lastUserLocation = updated
                            trySend(updated)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val handler = Handler(Looper.getMainLooper())
        val primarySensor = rotationSensor ?: orientationSensor
        if (sensorManager != null) {
            if (primarySensor != null) {
                runCatching {
                    sensorManager.registerListener(
                        sensorListener,
                        primarySensor,
                        SensorManager.SENSOR_DELAY_UI,
                        handler
                    )
                }
            } else if (accelSensor != null && magSensor != null) {
                runCatching {
                    sensorManager.registerListener(
                        sensorListener,
                        accelSensor,
                        SensorManager.SENSOR_DELAY_UI,
                        handler
                    )
                    sensorManager.registerListener(
                        sensorListener,
                        magSensor,
                        SensorManager.SENSOR_DELAY_UI,
                        handler
                    )
                }
            }
        }

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(buildUserLocation(location))
            }

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {}

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        runCatching {
            val gps = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val net = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            (gps ?: net)?.let { trySend(buildUserLocation(it)) }
        }

        val providers = buildList {
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
        }

        providers.forEach { provider ->
            runCatching {
                manager.requestLocationUpdates(
                    provider,
                    1_000L,
                    1f,
                    locationListener,
                    Looper.getMainLooper(),
                )
            }
        }

        awaitClose {
            runCatching { sensorManager?.unregisterListener(sensorListener) }
            runCatching { manager.removeUpdates(locationListener) }
        }
    }.conflate()
}

actual fun createDeviceLocationSource(): DeviceLocationSource {
    val ctx = AndroidSosContext.appContext ?: return NoopDeviceLocationSource()
    return AndroidDeviceLocationSource(ctx)
}
