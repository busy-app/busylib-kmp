package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TimezoneListResponse(
    @SerialName("list")
    val list: kotlin.collections.List<TimezoneInfo>? = null
)
