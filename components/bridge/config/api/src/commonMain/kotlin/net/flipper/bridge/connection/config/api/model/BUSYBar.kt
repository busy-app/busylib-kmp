package net.flipper.bridge.connection.config.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class BUSYBar(
    @SerialName("human_readable_name")
    val humanReadableName: String,
    @SerialName("unique_id")
    val uniqueId: String = Uuid.random().toString(),
    @SerialName("models")
    val models: List<ConnectionWay>
) {
    @Serializable
    sealed interface ConnectionWay {
        @Serializable
        data class BLE(
            @SerialName("address")
            val address: String
        ) : ConnectionWay

        @Serializable
        data object Mock : ConnectionWay

        @Serializable
        data class Lan(
            @SerialName("host")
            val host: String = "10.0.4.20",
        ) : ConnectionWay

        @Serializable
        data class Cloud(
            @SerialName("auth_token")
            val authToken: String,
            @SerialName("host")
            val host: String,
            @SerialName("device_id")
            val deviceId: String
        ) : ConnectionWay
    }
}
