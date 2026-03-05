package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BleStatusResponse(
    @SerialName("status")
    val state: State,
    @SerialName("address")
    val address: String? = null,
) {
    @Serializable
    enum class State {
        @SerialName("reset")
        RESET,

        @SerialName("initialization")
        INITIALIZATION,

        @SerialName("disabled")
        DISABLED,

        @SerialName("enabled")
        ENABLED,

        @SerialName("connectable")
        CONNECTABLE,

        @SerialName("connected")
        CONNECTED,

        @SerialName("internal error")
        INTERNAL_ERROR
    }
}
