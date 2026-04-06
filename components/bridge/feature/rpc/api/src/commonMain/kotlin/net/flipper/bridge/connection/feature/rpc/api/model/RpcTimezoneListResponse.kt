package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RpcTimezoneListResponse(
    @SerialName("list")
    val list: List<RpcTimezoneInfo>
)
