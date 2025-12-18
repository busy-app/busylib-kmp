package net.flipper.bridge.connection.feature.rpc.api.exposed

interface FRpcStreamingApi {
    suspend fun getScreen(display: Int): Result<String>
}
