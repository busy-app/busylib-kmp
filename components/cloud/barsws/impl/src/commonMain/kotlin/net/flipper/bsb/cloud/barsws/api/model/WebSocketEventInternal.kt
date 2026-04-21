package net.flipper.bsb.cloud.barsws.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.bsb.cloud.barsws.api.BUSYBarWebSocket
import net.flipper.bsb.cloud.barsws.api.WebSocketEvent
import kotlin.uuid.Uuid

@Serializable
sealed interface WebSocketEventInternal {
    @Serializable
    @SerialName("device.linked")
    data class LinkDevice(
        @SerialName("device_id")
        val deviceId: Uuid,
        @SerialName("hardware_id")
        val hardwareId: String
    ) : WebSocketEventInternal

    @Serializable
    @SerialName("device.unlinked")
    data class UnlinkDevice(
        @SerialName("device_id")
        val deviceId: Uuid,
        @SerialName("hardware_id")
        val hardwareId: String
    ) : WebSocketEventInternal

    @Serializable
    @SerialName("device.name-updated")
    data class NameUpdated(
        @SerialName("device_id")
        val deviceId: Uuid,
        @SerialName("hardware_id")
        val hardwareId: String,
        @SerialName("name")
        val name: String
    ) : WebSocketEventInternal

    @Serializable
    @SerialName("protobuf")
    data class Protobuf(
        @SerialName("bar_id")
        val cloudId: Uuid,
        @SerialName("state")
        val state: String
    ) : WebSocketEventInternal
}

fun WebSocketEventInternal.toPublic() = when (this) {
    is WebSocketEventInternal.LinkDevice -> WebSocketEvent.LinkEvent(
        BUSYBarWebSocket(
            cloudId = deviceId,
            hardwareId = hardwareId,
            name = null
        )
    )

    is WebSocketEventInternal.NameUpdated -> WebSocketEvent.NameChangeEvent(
        BUSYBarWebSocket(
            cloudId = deviceId,
            hardwareId = hardwareId,
            name = name
        )
    )

    is WebSocketEventInternal.UnlinkDevice -> WebSocketEvent.UnlinkEvent(
        BUSYBarWebSocket(
            cloudId = deviceId,
            hardwareId = hardwareId,
            name = null
        )
    )

    is WebSocketEventInternal.Protobuf -> null
}
