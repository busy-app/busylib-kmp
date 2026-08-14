package net.flipper.bridge.connection.feature.firmwareupdate.model

/**
 * [BsbUpdateVersion] together with the id of the device it belongs to
 */
data class DeviceUpdateVersion(
    val deviceId: String,
    val version: BsbUpdateVersion,
)
