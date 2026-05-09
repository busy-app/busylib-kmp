package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusyProfile(
    @SerialName("sort_order")
    val sortOrder: kotlin.Int,
    @SerialName("title")
    val title: kotlin.String,
    @SerialName("id")
    val id: kotlin.String,
    @SerialName("timer_settings")
    val timerSettings: BusyProfileTimerSettings,
    @SerialName("busy_bar_settings")
    val busyBarSettings: BusyBarSettings,
    @SerialName("profile_timestamp_ms")
    val profileTimestampMs: kotlin.Int
)
