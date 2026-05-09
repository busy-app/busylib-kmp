package net.flipper.bridge.connection.feature.rpc.generated.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SmartHomePairingInfoLatestPairingStatus(
    @SerialName("value")
    val `value`: Value,
    @SerialName("timestamp")
    val timestamp: kotlin.Int? = null
) {

    @Serializable
    enum class Value(val value: kotlin.String) {
        @SerialName("never_started")
        NEVER_STARTED("never_started"),

        @SerialName("started")
        STARTED("started"),

        @SerialName("completed_successfully")
        COMPLETED_SUCCESSFULLY("completed_successfully"),

        @SerialName("failed")
        FAILED("failed")
    }
}
