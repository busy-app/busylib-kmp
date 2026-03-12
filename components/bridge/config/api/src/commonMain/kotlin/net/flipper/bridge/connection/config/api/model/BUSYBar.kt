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
    @SerialName("connection_way_ble")
    val ble: ConnectionWay.BLE? = null,
    @SerialName("connection_way_cloud")
    val cloud: ConnectionWay.Cloud? = null,
    @SerialName("connection_way_lan")
    val lan: ConnectionWay.Lan? = null,
    @SerialName("connection_way_mock")
    val mock: ConnectionWay.Mock? = null,
) {
    /**
     * Returns all non-null connection ways ordered by priority (highest first):
     * Lan(100) > Cloud(10) > BLE(0) > Mock(-1)
     */
    val connectionWays: List<ConnectionWay>
        get() = listOfNotNull(lan, cloud, ble, mock)

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
            @SerialName("device_id")
            val deviceId: Uuid
        ) : ConnectionWay
    }
}
