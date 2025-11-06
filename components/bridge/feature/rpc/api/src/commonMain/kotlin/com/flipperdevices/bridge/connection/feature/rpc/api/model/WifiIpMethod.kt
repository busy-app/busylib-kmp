package com.flipperdevices.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WifiIpMethod {
    @SerialName("dhcp")
    DHCP,

    @SerialName("static")
    STATIC
}
