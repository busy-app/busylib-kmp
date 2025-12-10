package net.flipper.bridge.connection.config.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class FDeviceBaseModel {
    @SerialName("human_readable_name")
    abstract val humanReadableName: String
    @SerialName("unique_id")
    abstract val uniqueId: String

    @Serializable
    class FDeviceBSBModelBLE(
        @SerialName("address")
        val address: String,
        override val uniqueId: String = address,
        override val humanReadableName: String,
    ) : FDeviceBaseModel()

    @Serializable
    class FDeviceBSBModelBLEiOS(
        override val uniqueId: String,
        override val humanReadableName: String,
    ) : FDeviceBaseModel()

    @Serializable
    class FDeviceBSBModelMock(
        override val uniqueId: String,
        override val humanReadableName: String = "BUSY Bar Mock"
    ) : FDeviceBaseModel()

    @Serializable
    class FDeviceBSBModelLan(
        val host: String = "10.0.4.20",
    ) : FDeviceBaseModel() {
        override val uniqueId: String = host
        override val humanReadableName: String = "BUSY Bar LAN"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FDeviceBaseModel) return false
        return uniqueId == other.uniqueId
    }

    override fun hashCode(): Int {
        return uniqueId.hashCode()
    }
}
