package net.flipper.bsb.cloud.barsws.api

import kotlin.uuid.Uuid

sealed interface WebSocketRequest {
    data class Subscribe(
        val deviceId: Uuid
    ) : WebSocketRequest

    data class Unsubscribe(
        val deviceId: Uuid
    ) : WebSocketRequest
}
