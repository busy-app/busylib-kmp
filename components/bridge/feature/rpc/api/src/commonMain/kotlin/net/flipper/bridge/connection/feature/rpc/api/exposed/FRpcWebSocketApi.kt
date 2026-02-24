package net.flipper.bridge.connection.feature.rpc.api.exposed

import kotlinx.coroutines.flow.Flow

interface FRpcWebSocketApi {
    fun getScreenFrames(): Flow<ByteArray>
}
