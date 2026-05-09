package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TimezoneInfo(
    @SerialName("name")
    val name: kotlin.String,
    @SerialName("offset")
    val offset: kotlin.String,
    @SerialName("abbr")
    val abbr: kotlin.String
)
