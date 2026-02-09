package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TimestampInfo(
    @SerialName("timestamp")
    val timestamp: String
)
