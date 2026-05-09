package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusResponseIpConfig(
    @SerialName("ip_method")
    val ipMethod: WifiIpMethod? = null,
    @SerialName("ip_type")
    val ipType: WifiIpType? = null,
    @SerialName("address")
    val address: kotlin.String? = null
)
