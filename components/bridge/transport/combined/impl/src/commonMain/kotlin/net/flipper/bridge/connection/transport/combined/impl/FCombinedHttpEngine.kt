package net.flipper.bridge.connection.transport.combined.impl

import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.utils.io.InternalAPI
import net.flipper.bridge.connection.transport.combined.impl.connections.AutoReconnectConnection
import net.flipper.core.busylib.log.LogTagProvider

class FCombinedHttpEngine(
    private val connections: List<AutoReconnectConnection>,
) : HttpClientEngineBase("bb-combined-http"), LogTagProvider {
    override val TAG = "FCombinedHttpEngine"

    override val config = HttpClientEngineConfig()

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        TODO()
    }
}