package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BusyProfileSlot(val value: kotlin.String) {
    @SerialName("busy")
    BUSY("busy"),

    @SerialName("custom")
    CUSTOM("custom")
}
