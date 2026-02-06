package net.flipper.bsb.cloud.barsws.api

sealed interface WebSocketRequest {
    data class Subscribe(
        val deviceId: String
    ) : WebSocketRequest

    data class Unsubscribe(
        val deviceId: String
    ) : WebSocketRequest
}
