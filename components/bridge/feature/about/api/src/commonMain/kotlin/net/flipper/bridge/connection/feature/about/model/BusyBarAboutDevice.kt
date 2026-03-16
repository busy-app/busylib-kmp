package net.flipper.bridge.connection.feature.about.model

data class BusyBarAboutDevice(
    val serialNumber: String,
    val macAddressBluetooth: String,
    val macAddressWifi: String,
    val macAddressUsb: String,
    val hardwareVersion: String,
    val productionDate: String,
    val frontDisplayResolution: String,
    val frontDisplayRefreshRate: String,
    val backDisplayResolution: String,
    val centralMcu: String,
    val ramSize: String,
)
