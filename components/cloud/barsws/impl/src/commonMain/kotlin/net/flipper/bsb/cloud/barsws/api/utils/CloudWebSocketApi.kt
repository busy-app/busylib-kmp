package net.flipper.bsb.cloud.barsws.api.utils

import kotlinx.coroutines.flow.Flow

interface CloudWebSocketApi {
    fun getWSFlow(): Flow<BSBWebSocket?>
}
