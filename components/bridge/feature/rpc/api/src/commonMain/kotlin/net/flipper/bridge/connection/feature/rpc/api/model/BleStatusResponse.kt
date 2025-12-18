package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BleStatusResponse(
    @SerialName("state")
    val state: State,
    @SerialName("address")
    val address: String? = null,
    @SerialName("pairing")
    val pairing: Pairing? = null
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

        @SerialName("connected")
        CONNECTED,

        @SerialName("internal error")
        INTERNAL_ERROR
    }

    @Serializable
    enum class Pairing {
        @SerialName("unknown")
        UNKNOWN,

        @SerialName("not paired")
        NOT_PAIR,

        @SerialName("paired")
        PAIRED
    }
}
