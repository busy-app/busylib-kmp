package net.flipper.bridge.connection.feature.ble.api.model

import net.flipper.bridge.connection.feature.rpc.api.model.BleStatusResponse

sealed interface FBleStatus {
    data object Reset : FBleStatus
    data object Initialization : FBleStatus
    data object InternalError : FBleStatus
    data object Disabled : FBleStatus
    data object Enabled : FBleStatus
    data class Connected(
        val address: String,
        val pairing: BleStatusResponse.Pairing
    ) : FBleStatus
}
