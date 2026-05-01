package net.flipper.bridge.connection.feature.about.model

import kotlin.time.Instant

data class BusyBarAboutDevice(
    val serialNumber: String,
    val macAddressBluetooth: String?,
    val macAddressWifi: String?,
    val macAddressUsb: String,
    val hardwareVersion: String?,
    val productionDate: Instant?,
    val frontDisplayResolution: String,
    val frontDisplayRefreshRate: String,
    val backDisplayResolution: String,
    val centralMcu: String,
    val ramSize: String,
)
