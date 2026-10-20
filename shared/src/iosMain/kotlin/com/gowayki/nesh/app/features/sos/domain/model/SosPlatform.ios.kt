// Actual iOS — stubs para no romper la compilación iOS.
// TODO iOS real: CoreBluetooth (CBCentralManager) + CoreLocation (CLLocationManager).
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.sos.domain.model

actual fun createBleSosClient(): BleSosClient = NoopBleSosClient()

actual fun createGpsProvider(): GpsProvider = NoopGpsProvider()
