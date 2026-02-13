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
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.token.ProxyTokenProvider
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

typealias BUSYCloudHttpEngineFactory = (HttpClientEngine, ProxyTokenProvider) -> BUSYCloudHttpEngine

@Inject
class BUSYCloudHttpEngine(
    @Assisted private val delegate: HttpClientEngine,
    @Assisted private val tokenProvider: ProxyTokenProvider,
    private val hostApi: BUSYLibHostApi
) : HttpClientEngineBase("busy-bar"), LogTagProvider {
    override val TAG = "BUSYCloudHttpEngine"
    override val config: HttpClientEngineConfig = delegate.config

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val result = try {
            makeRequest(data, tokenProvider.getToken())
        } catch (e: Throwable) {
            throw e
        }

        return result
    }

    @InternalAPI
    private suspend fun makeRequest(
        data: HttpRequestData,
        token: String
    ): HttpResponseData {
        val newRequest = HttpRequestBuilder().takeFrom(data)

        newRequest.url.host = hostApi.getProxyHost().value
        newRequest.url.protocol = URLProtocol.HTTPS
        newRequest.url.port = 443
        newRequest.headers["Authorization"] = "Bearer $token"

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
