package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BleStatusResponse(
    @SerialName("status")
    val status: Status,
    @SerialName("address")
    val address: kotlin.String? = null
) {

    @Serializable
    enum class Status(val value: kotlin.String) {
        @SerialName("reset")
        RESET("reset"),

        @SerialName("initialization")
        INITIALIZATION("initialization"),

        @SerialName("disabled")
        DISABLED("disabled"),

        @SerialName("enabled")
        ENABLED("enabled"),

        @SerialName("connectable")
        CONNECTABLE("connectable"),

        @SerialName("connected")
        CONNECTED("connected"),

        @SerialName("internal error")
        INTERNAL_ERROR("internal error")
    }
}
