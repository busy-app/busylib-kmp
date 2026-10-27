package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TelemetryInfo(
    @SerialName("enabled")
    val enabled: Boolean,
    @SerialName("asked")
    val asked: Boolean
)
