package net.flipper.bridge.connection.config.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class FDeviceCombined(
    @SerialName("human_readable_name")
    val humanReadableName: String,
    @SerialName("unique_id")
    val uniqueId: String = Uuid.random().toString(),
    @SerialName("models")
    val models: List<DeviceModel>
) {
    @Serializable
    sealed interface DeviceModel {
        @Serializable
        data class FDeviceBSBModelBLE(
            @SerialName("address")
            val address: String
        ) : DeviceModel

        @Serializable
        data object FDeviceBSBModelMock : DeviceModel

        @Serializable
        data class FDeviceBSBModelLan(
            @SerialName("host")
            val host: String = "10.0.4.20",
        ) : DeviceModel

        @Serializable
        data class FDeviceBSBModelCloud(
            @SerialName("auth_token")
            val authToken: String,
            @SerialName("host")
            val host: String,
            @SerialName("device_id")
            val deviceId: String
        ) : DeviceModel
    }
}
