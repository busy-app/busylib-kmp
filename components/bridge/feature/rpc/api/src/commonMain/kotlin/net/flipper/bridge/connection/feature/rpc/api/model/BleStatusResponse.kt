package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BleStatusResponse(
    @SerialName("state")
    val state: BleState,
    @SerialName("connected_device_ssid")
    val connectedDeviceBssid: String? = null,
    @SerialName("connected_device_name")
    val connectedDeviceName: String? = null
)
