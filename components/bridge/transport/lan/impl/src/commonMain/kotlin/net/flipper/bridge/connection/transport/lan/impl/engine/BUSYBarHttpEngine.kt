package net.flipper.bridge.connection.transport.lan.impl.engine

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.takeFrom
import io.ktor.utils.io.InternalAPI

class BUSYBarHttpEngine(
    private val delegate: HttpClientEngine,
    private val host: String
) : HttpClientEngineBase("busy-bar") {
    override val config: HttpClientEngineConfig = delegate.config

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val newRequest = HttpRequestBuilder().takeFrom(data)

        newRequest.url.host = host

        val newRequestData = newRequest.build()

        return delegate.execute(newRequestData)
    }
}
