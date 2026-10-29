package net.flipper.bsb.cloud.barsws.api

import kotlin.time.Instant
import kotlin.uuid.Uuid

sealed interface WebSocketEvent {
    data class LinkEvent(
        val device: BUSYBarWebSocket
    ) : WebSocketEvent

    data class UnlinkEvent(
        val device: BUSYBarWebSocket
    ) : WebSocketEvent

    data class NameChangeEvent(
        val device: BUSYBarWebSocket
    ) : WebSocketEvent

    data class BusyBarAccessUpdatedEvent(
        val granted: Boolean,
        val graceUntil: Instant?
    ) : WebSocketEvent
}

data class BUSYBarWebSocket(
    val cloudId: Uuid,
    val hardwareId: String,
    val name: String?
)
