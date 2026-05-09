package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusyTimerIntervalSettings(
    @SerialName("type")
    val type: Type,
    @SerialName("interval_work_ms")
    val intervalWorkMs: kotlin.Int,
    @SerialName("interval_rest_ms")
    val intervalRestMs: kotlin.Int,
    @SerialName("interval_work_cycles_count")
    val intervalWorkCyclesCount: kotlin.Int,
    @SerialName("is_autostart_enabled")
    val isAutostartEnabled: kotlin.Boolean
) {

    @Serializable
    enum class Type(val value: kotlin.String) {
        @SerialName("INTERVAL")
        INTERVAL("INTERVAL")
    }
}
