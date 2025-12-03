package net.flipper.bridge.connection.feature.ble.api.model

sealed interface FBleStatus {
    data object Disabled : FBleStatus
    data object Enabled : FBleStatus
    data class Connected(
        val connectedDeviceBssid: String,
        val connectedDeviceName: String
    ) : FBleStatus
}
