package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusySnapshotInterval(
    @SerialName("type")
    val type: Type,
    @SerialName("card_id")
    val cardId: kotlin.String,
    @SerialName("current_interval")
    val currentInterval: kotlin.Int,
    @SerialName("current_interval_time_total_ms")
    val currentIntervalTimeTotalMs: kotlin.Int,
    @SerialName("current_interval_time_left_ms")
    val currentIntervalTimeLeftMs: kotlin.Int,
    @SerialName("is_paused")
    val isPaused: kotlin.Boolean,
    @SerialName("interval_settings")
    val intervalSettings: BusyTimerIntervalSettings
) {

    @Serializable
    enum class Type(val value: kotlin.String) {
        @SerialName("INTERVAL")
        INTERVAL("INTERVAL")
    }
}
