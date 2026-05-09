package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusySnapshotNotStarted(
    @SerialName("type")
    val type: Type
) {

    @Serializable
    enum class Type(val value: kotlin.String) {
        @SerialName("NOT_STARTED")
        NOT_STARTED("NOT_STARTED")
    }
}
