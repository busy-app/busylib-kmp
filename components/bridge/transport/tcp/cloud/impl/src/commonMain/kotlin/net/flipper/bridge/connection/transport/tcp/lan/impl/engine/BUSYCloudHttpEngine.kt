package net.flipper.bridge.connection.transport.tcp.lan.impl.engine

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.takeFrom
import io.ktor.http.URLProtocol
import io.ktor.utils.io.InternalAPI
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class BUSYCloudHttpEngine(
    private val delegate: HttpClientEngine,
    private val authToken: String,
    private val host: String
) : HttpClientEngineBase("busy-bar"), LogTagProvider {
    override val TAG = "BUSYCloudHttpEngine"
    override val config: HttpClientEngineConfig = delegate.config

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val newRequest = HttpRequestBuilder().takeFrom(data)

        newRequest.url.host = host
        newRequest.url.protocol = URLProtocol.HTTPS
        newRequest.url.port = 443
        newRequest.headers["Authorization"] = "Bearer $authToken"

        val newRequestData = newRequest.build()
        info { "Process $newRequestData" }

        val result = delegate.execute(newRequestData)
        info { "Return $result" }
        return result
    }

    override fun close() {
        delegate.close()
        super.close()
    }
}
