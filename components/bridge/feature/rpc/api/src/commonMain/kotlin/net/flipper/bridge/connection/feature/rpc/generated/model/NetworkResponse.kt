package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkResponse(
    @SerialName("count")
    val count: kotlin.Int? = null,
    @SerialName("networks")
    val networks: kotlin.collections.List<Network>? = null
)
