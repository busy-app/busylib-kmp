package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SmartHomeSwitchState(
    @SerialName("state")
    val state: kotlin.Boolean? = null,
    @SerialName("startup")
    val startup: Startup? = null
) {

    @Serializable
    enum class Startup(val value: kotlin.String) {
        @SerialName("off")
        OFF("off"),

        @SerialName("on")
        ON("on"),

        @SerialName("toggle")
        TOGGLE("toggle"),

        @SerialName("last")
        LAST("last")
    }
}
