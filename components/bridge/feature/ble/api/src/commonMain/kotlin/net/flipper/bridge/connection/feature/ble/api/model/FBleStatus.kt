package net.flipper.bridge.connection.feature.ble.api.model

sealed interface FBleStatus {
    data object Reset : FBleStatus
    data object Initialization : FBleStatus
    data object InternalError : FBleStatus
    data object Disabled : FBleStatus
    data object Enabled : FBleStatus
    data object Connectable : FBleStatus
    data class Connected(val address: String) : FBleStatus
}
