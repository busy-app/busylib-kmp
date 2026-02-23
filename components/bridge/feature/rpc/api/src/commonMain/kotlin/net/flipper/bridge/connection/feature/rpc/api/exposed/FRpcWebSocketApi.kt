package net.flipper.bridge.connection.feature.rpc.api.exposed

import kotlinx.coroutines.flow.Flow

interface FRpcWebSocketApi {
    suspend fun getScreenFrames(): Result<Flow<ByteArray>>
}