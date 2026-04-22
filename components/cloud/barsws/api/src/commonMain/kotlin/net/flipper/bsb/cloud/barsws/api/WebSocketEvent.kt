package net.flipper.bsb.cloud.barsws.api

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
}

data class BUSYBarWebSocket(
    val cloudId: Uuid,
    val hardwareId: String,
    val name: String?
)
