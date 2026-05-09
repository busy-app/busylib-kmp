package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountStatus(
    @SerialName("status")
    val status: Status? = null
) {

    @Serializable
    enum class Status(val value: kotlin.String) {
        @SerialName("error")
        ERROR("error"),

        @SerialName("disconnected")
        DISCONNECTED("disconnected"),

        @SerialName("connected")
        CONNECTED("connected")
    }
}
