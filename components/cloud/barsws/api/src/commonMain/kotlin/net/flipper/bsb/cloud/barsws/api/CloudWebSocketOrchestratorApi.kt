package net.flipper.bsb.cloud.barsws.api

import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

interface CloudWebSocketOrchestratorApi {
    fun getEventsFlow(cloudId: Uuid): Flow<Pair<String, Any>>
}
