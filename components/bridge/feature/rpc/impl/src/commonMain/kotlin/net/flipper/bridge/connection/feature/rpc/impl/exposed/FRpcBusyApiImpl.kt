package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CoroutineDispatcher
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcBusyApi
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.core.busylib.ktx.common.cache.ObjectCache
import net.flipper.core.busylib.ktx.common.cache.getOrElse
import net.flipper.core.busylib.ktx.common.runSuspendCatching

class FRpcBusyApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher,
    private val objectCache: ObjectCache
) : FRpcBusyApi {

    override suspend fun getBusySnapshot(ignoreCache: Boolean): Result<String> {
        return runSuspendCatching(dispatcher) {
            objectCache.getOrElse(ignoreCache) {
                httpClient.get("/api/busy/snapshot").bodyAsText()
            }
        }
    }

    override suspend fun setBusySnapshot(rawJson: String): Result<Unit> {
        return runSuspendCatching(dispatcher) {
            httpClient.put("/api/busy/snapshot") {
                setBody(TextContent(rawJson, ContentType.Application.Json))
            }.body<SuccessResponse>()
        }.map { }
    }

    override suspend fun setBusyProfile(slot: String, rawJson: String): Result<Unit> {
        return runSuspendCatching(dispatcher) {
            httpClient.put("/api/busy/profiles/$slot") {
                setBody(TextContent(rawJson, ContentType.Application.Json))
            }.body<SuccessResponse>()
        }.map { }
    }
}
