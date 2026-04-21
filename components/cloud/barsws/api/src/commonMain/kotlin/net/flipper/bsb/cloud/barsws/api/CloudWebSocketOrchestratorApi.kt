package net.flipper.bsb.cloud.barsws.api

import kotlinx.coroutines.flow.Flow
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@JvmInline
value class ProtobufBase64(val data: String)

interface CloudWebSocketOrchestratorApi {
    fun getEventsFlow(cloudId: Uuid): Flow<ProtobufBase64>
}
