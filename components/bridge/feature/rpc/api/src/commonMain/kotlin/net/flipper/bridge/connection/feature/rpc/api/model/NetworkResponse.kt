package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkResponse(
    @SerialName("count")
    val count: Int,
    @SerialName("networks")
    val networks: List<Network> = emptyList()
)
