package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusySnapshotSimple(
    @SerialName("type")
    val type: Type,
    @SerialName("card_id")
    val cardId: kotlin.String,
    @SerialName("time_left_ms")
    val timeLeftMs: kotlin.Int,
    @SerialName("is_paused")
    val isPaused: kotlin.Boolean
) {

    @Serializable
    enum class Type(val value: kotlin.String) {
        @SerialName("SIMPLE")
        SIMPLE("SIMPLE")
    }
}
