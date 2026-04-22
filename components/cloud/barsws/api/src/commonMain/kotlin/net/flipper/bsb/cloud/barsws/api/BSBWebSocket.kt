package net.flipper.bsb.cloud.barsws.api

import kotlinx.coroutines.flow.Flow

interface BSBWebSocket {
    fun getEventsFlow(): Flow<WebSocketEvent>
}
