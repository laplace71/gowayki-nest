package com.gowayki.nesh.app.features.map.data

// iOS todavía usa el mapa de respaldo: sin seguimiento GPS por ahora.
actual fun createDeviceLocationSource(): DeviceLocationSource = NoopDeviceLocationSource()
