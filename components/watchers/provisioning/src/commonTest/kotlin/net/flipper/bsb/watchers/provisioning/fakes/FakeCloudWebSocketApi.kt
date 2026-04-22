package net.flipper.bsb.watchers.provisioning.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import net.flipper.bsb.cloud.barsws.api.BSBWebSocket
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketApi
import net.flipper.bsb.cloud.barsws.api.WebSocketEvent

internal object FakeCloudWebSocketApi : CloudWebSocketApi {
    private val fakeBSBWebSocket = object : BSBWebSocket {
        override fun getEventsFlow(): Flow<WebSocketEvent> = emptyFlow()
    }

    override fun getWSFlow(): Flow<BSBWebSocket?> = flowOf(fakeBSBWebSocket)
}
