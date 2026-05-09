package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusyTimerInfiniteSettings(
    @SerialName("type")
    val type: Type
) {

    @Serializable
    enum class Type(val value: kotlin.String) {
        @SerialName("INFINITE")
        INFINITE("INFINITE")
    }
}
