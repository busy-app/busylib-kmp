package net.flipper.bridge.connection.feature.rpc.api.model
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RpcTimezoneListItem(
    @SerialName("name")
    val name: String,

    @SerialName("offset")
    val offset: String,

    @SerialName("abbr")
    val abbr: String
)

@Serializable
data class RpcTimezoneListResponse(
    @Serializable
    val list: List<RpcTimezoneListItem>
)
