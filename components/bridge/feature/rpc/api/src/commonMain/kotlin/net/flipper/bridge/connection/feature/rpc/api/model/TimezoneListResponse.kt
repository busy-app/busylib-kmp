package net.flipper.bridge.connection.feature.rpc.api.model
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TimezoneListItem(
    @SerialName("name")
    val name: String,

    @SerialName("offset")
    val offset: String,

    @SerialName("abbr")
    val abbr: String
)

typealias TimezoneListResponse = List<TimezoneListItem>
