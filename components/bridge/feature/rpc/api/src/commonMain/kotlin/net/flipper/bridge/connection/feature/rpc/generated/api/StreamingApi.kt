package net.flipper.bridge.connection.feature.rpc.generated.api

import kotlin.String

interface StreamingApi {

    /**
     * Get single frame for requested screen
     */
    suspend fun apiScreenGet(display: kotlin.Int): kotlin.Result<kotlin.String>

    /**
     * Device status streaming WebSocket endpoint
     */
    suspend fun connectWebSocket(): kotlin.Result<Unit>
}
