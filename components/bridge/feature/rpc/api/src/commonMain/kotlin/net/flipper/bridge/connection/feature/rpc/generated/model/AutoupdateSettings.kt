package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AutoupdateSettings(
    @SerialName("is_enabled")
    val isEnabled: kotlin.Boolean,
    @SerialName("interval_start")
    val intervalStart: kotlin.String? = null,
    @SerialName("interval_end")
    val intervalEnd: kotlin.String? = null
)
