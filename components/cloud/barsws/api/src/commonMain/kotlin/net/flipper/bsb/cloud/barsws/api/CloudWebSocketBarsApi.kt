package net.flipper.bsb.cloud.barsws.api

import kotlinx.coroutines.flow.Flow

interface CloudWebSocketBarsApi {
    fun getWSFlow(): Flow<BSBWebSocket>
}

interface BSBWebSocket {
    fun getEventsFlow(): Flow<WebSocketEvent>

    suspend fun send(request: WebSocketRequest)
}
