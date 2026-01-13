package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcStreamingApi
import net.flipper.core.busylib.ktx.common.runSuspendCatching

class FRpcStreamingApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher
) : FRpcStreamingApi {
    override suspend fun getScreen(display: Int): Result<String> {
        return runSuspendCatching(dispatcher) {
            httpClient.get {
                url("/api/screen")
                header(HttpHeaders.Accept, "image/bmp")
                parameter("display", display)
            }.bodyAsText()
        }
    }
}
