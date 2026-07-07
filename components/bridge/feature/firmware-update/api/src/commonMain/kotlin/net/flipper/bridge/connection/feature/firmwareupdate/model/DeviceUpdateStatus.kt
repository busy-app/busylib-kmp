package net.flipper.bridge.connection.feature.firmwareupdate.model

/**
 * [BsbUpdateStatus] together with the id of the device it belongs to
 */
data class DeviceUpdateStatus(
    val deviceId: String,
    val status: BsbUpdateStatus,
)
