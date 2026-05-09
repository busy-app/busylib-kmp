package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusyProfileTimerSettings(
    @SerialName("type")
    val type: Type,
    @SerialName("total_time_ms")
    val totalTimeMs: kotlin.Int,
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
        @SerialName("INFINITE")
        INFINITE("INFINITE"),

        @SerialName("SIMPLE")
        SIMPLE("SIMPLE"),

        @SerialName("INTERVAL")
        INTERVAL("INTERVAL")
    }
}
