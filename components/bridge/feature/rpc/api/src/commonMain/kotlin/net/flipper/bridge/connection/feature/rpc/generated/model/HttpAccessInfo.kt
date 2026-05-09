package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HttpAccessInfo(
    @SerialName("mode")
    val mode: Mode? = null,
    @SerialName("key_valid")
    val keyValid: kotlin.Boolean? = null
) {

    @Serializable
    enum class Mode(val value: kotlin.String) {
        @SerialName("disabled")
        DISABLED("disabled"),

        @SerialName("enabled")
        ENABLED("enabled"),

        @SerialName("key")
        KEY("key")
    }
}
