package net.flipper.bridge.connection.config.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
sealed interface FDeviceBaseModel {
    @SerialName("human_readable_name")
    val humanReadableName: String

    @SerialName("unique_id")
    val uniqueId: String

    @Serializable
    data class FDeviceBSBModelBLE(
        @SerialName("address")
        val address: String,
        override val uniqueId: String = address,
        override val humanReadableName: String,
    ) : FDeviceBaseModel

    @Serializable
    data class FDeviceBSBModelBLEiOS(
        @SerialName("address")
        val uuid: String,
        override val uniqueId: String = uuid,
        override val humanReadableName: String,
    ) : FDeviceBaseModel

    @Serializable
    data class FDeviceBSBModelMock(
        override val uniqueId: String = Uuid.random().toString(),
        override val humanReadableName: String = "BUSY Bar Mock"
    ) : FDeviceBaseModel
}
