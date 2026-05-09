package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Network(
    @SerialName("ssid")
    val ssid: kotlin.String,
    @SerialName("security")
    val security: WifiSecurityMethod,
    @SerialName("rssi")
    val rssi: kotlin.Int
)
