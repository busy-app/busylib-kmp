package net.flipper.bsb.cloud.barsws.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flipper.bsb.cloud.barsws.api.WebSocketEvent
import kotlin.uuid.Uuid

@Serializable
sealed interface WebSocketEventInternal {

    @Serializable
    @SerialName("device.linked")
    data class LinkDevice(
        @SerialName("device")
        val device: BUSYBarWebSocketInternal
    ) : WebSocketEventInternal

    @Serializable
    @SerialName("device.unlinked")
    data class UnlinkDevice(
        @SerialName("device")
        val device: BUSYBarWebSocketInternal
    ) : WebSocketEventInternal

    @Serializable
    @SerialName("device.name-updated")
    data class NameUpdated(
        @SerialName("device")
        val device: BUSYBarWebSocketInternal
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
        device.toPublic()
    )

    is WebSocketEventInternal.NameUpdated -> WebSocketEvent.NameChangeEvent(
        device.toPublic()
    )

    is WebSocketEventInternal.UnlinkDevice -> WebSocketEvent.UnlinkEvent(
        device.toPublic()
    )

    is WebSocketEventInternal.Protobuf -> null
}
