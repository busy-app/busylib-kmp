package net.flipper.bsb.cloud.barsws.api

import kotlinx.coroutines.flow.Flow

interface CloudWebSocketBarsApi {
    fun getWSFlow(): Flow<WebSocketEvent>
}
