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
    val models: List<ConnectionWayModel>
) {
    @Serializable
    sealed interface ConnectionWayModel {
        @Serializable
        data class FConnectionWayBSBModelBLE(
            @SerialName("address")
            val address: String
        ) : ConnectionWayModel

        @Serializable
        data object FConnectionWayBSBModelMock : ConnectionWayModel

        @Serializable
        data class FConnectionWayBSBModelLan(
            @SerialName("host")
            val host: String = "10.0.4.20",
        ) : ConnectionWayModel

        @Serializable
        data class FConnectionWayBSBModelCloud(
            @SerialName("auth_token")
            val authToken: String,
            @SerialName("host")
            val host: String,
            @SerialName("device_id")
            val deviceId: String
        ) : ConnectionWayModel
    }
}
