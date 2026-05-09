package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateStatusCheck(
    @SerialName("available_version")
    val availableVersion: kotlin.String,
    @SerialName("event")
    val event: Event? = null,
    @SerialName("status")
    val status: Status
) {

    @Serializable
    enum class Event(val value: kotlin.String) {
        @SerialName("start")
        START("start"),

        @SerialName("stop")
        STOP("stop"),

        @SerialName("none")
        NONE("none")
    }

    @Serializable
    enum class Status(val value: kotlin.String) {
        @SerialName("available")
        AVAILABLE("available"),

        @SerialName("not_available")
        NOT_AVAILABLE("not_available"),

        @SerialName("failure")
        FAILURE("failure"),

        @SerialName("none")
        NONE("none")
    }
}
