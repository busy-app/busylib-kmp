package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusySnapshotInfinite(
    @SerialName("type")
    val type: Type,
    @SerialName("card_id")
    val cardId: kotlin.String,
    @SerialName("is_paused")
    val isPaused: kotlin.Boolean
) {

    @Serializable
    enum class Type(val value: kotlin.String) {
        @SerialName("INFINITE")
        INFINITE("INFINITE")
    }
}
