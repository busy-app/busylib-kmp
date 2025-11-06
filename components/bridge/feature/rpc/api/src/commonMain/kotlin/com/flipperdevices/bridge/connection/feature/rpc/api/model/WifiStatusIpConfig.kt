package com.flipperdevices.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WifiStatusIpConfig(
    @SerialName("ip_method")
    val ipMethod: WifiIpMethod,
    @SerialName("ip_type")
    val ipType: WifiIpType,
    @SerialName("address")
    val address: String
)
