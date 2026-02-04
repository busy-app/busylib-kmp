package net.flipper.bridge.connection.config.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

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
        override val uniqueId: String = Uuid.random().toString(),
        override val humanReadableName: String = "BUSY Bar Mock"
    ) : FDeviceBaseModel()

    @Serializable
    class FDeviceBSBModelLan(
        @SerialName("host")
        val host: String = "10.0.4.20",
    ) : FDeviceBaseModel() {
        override val uniqueId: String = host
        override val humanReadableName: String = "BUSY Bar LAN"
    }

    @Serializable
    class FDeviceBSBModelCloud(
        @SerialName("auth_token")
        val authToken: String,
        @SerialName("host")
        val host: String,
        @SerialName("device_id")
        val deviceId: String
    ) : FDeviceBaseModel() {
        override val uniqueId: String = authToken
        override val humanReadableName: String = "BUSY Bar Cloud"
    }

    @Serializable
    class FDeviceBSBModelCombined(
        override val uniqueId: String = Uuid.random().toString(),
        override val humanReadableName: String = models.first().humanReadableName,
        @SerialName("models")
        val models: List<FDeviceBaseModel>
    ) : FDeviceBaseModel()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FDeviceBaseModel) return false
        return uniqueId == other.uniqueId
    }

    override fun hashCode(): Int {
        return uniqueId.hashCode()
    }

    override fun toString(): String {
        when (this) {
            is FDeviceBSBModelBLE -> {
                return "FDeviceBSBModelBLE(" +
                    "address='$address', " +
                    "uniqueId='$uniqueId', " +
                    "humanReadableName='$humanReadableName'" +
                    ")"
            }

            is FDeviceBSBModelBLEiOS -> {
                return "FDeviceBSBModelBLEiOS(" +
                    "uniqueId='$uniqueId', " +
                    "humanReadableName='$humanReadableName'" +
                    ")"
            }

            is FDeviceBSBModelMock -> {
                return "FDeviceBSBModelMock(" +
                    "uniqueId='$uniqueId', " +
                    "humanReadableName='$humanReadableName'" +
                    ")"
            }

            is FDeviceBSBModelLan -> {
                return "FDeviceBSBModelLan(" +
                    "host='$host', " +
                    "uniqueId='$uniqueId', " +
                    "humanReadableName='$humanReadableName'" +
                    ")"
            }

            is FDeviceBSBModelCloud -> {
                return "FDeviceBSBModelCloud(" +
                    "authToken='$authToken', " +
                    "host='$host', " +
                    "uniqueId='$uniqueId', " +
                    "humanReadableName='$humanReadableName'" +
                    ")"
            }

            is FDeviceBSBModelCombined ->
                return "FDeviceBSBModelCombined(" +
                    "uniqueId='$uniqueId'," +
                    "humanReadableName='$humanReadableName'," +
                    "models=$models" +
                    ")"
        }
    }
}
