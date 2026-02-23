package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AutoUpdate(
    @SerialName("is_enabled")
    val isEnabled: Boolean,
    @SerialName("interval_start")
    val intervalStart: String,
    @SerialName("interval_end")
    val intervalEnd: String
)
