package net.flipper.bridge.connection.feature.rpc.api.model
import kotlinx.serialization.Serializable

@Serializable
data class RpcTimezoneListResponse(
    @Serializable
    val list: List<RpcTimezoneInfo>
)
