package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusyTimerSimpleSettings(
    @SerialName("type")
    val type: Type,
    @SerialName("total_time_ms")
    val totalTimeMs: kotlin.Int
) {

    @Serializable
    enum class Type(val value: kotlin.String) {
        @SerialName("SIMPLE")
        SIMPLE("SIMPLE")
    }
}
