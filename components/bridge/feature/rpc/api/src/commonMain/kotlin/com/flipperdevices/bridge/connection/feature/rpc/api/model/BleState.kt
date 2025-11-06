package com.flipperdevices.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BleState {
    @SerialName("disabled")
    DISABLED,

    @SerialName("enabled")
    ENABLED,

    @SerialName("connected")
    CONNECTED
}
