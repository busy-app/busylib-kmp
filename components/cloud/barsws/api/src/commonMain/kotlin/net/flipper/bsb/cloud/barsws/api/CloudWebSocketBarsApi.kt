package net.flipper.bsb.cloud.barsws.api

import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

interface CloudWebSocketBarsApi {
    fun getEventsFlow(cloudId: Uuid): Flow<Pair<String, Any>>
}
