package net.flipper.bridge.connection.transport.combined.impl

import io.ktor.client.engine.DEFAULT_CAPABILITIES
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineCapability
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.websocket.WebSocketCapability
import io.ktor.client.plugins.websocket.WebSocketExtensionsCapability
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.transport.combined.impl.connections.SharedConnectionPool
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.attributes.RequestCapabilityKey
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.verbose

@OptIn(ExperimentalCoroutinesApi::class)
class FCombinedHttpEngine(
    private val connectionPool: SharedConnectionPool,
) : HttpClientEngineBase("bb-combined-http"), LogTagProvider {
    override val TAG = "FCombinedHttpEngine"

    override val config = HttpClientEngineConfig()

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val currentDelegates = connectionPool.get().first().map { snapshot ->
            if (snapshot.status !is FInternalTransportConnectionStatus.Connected) {
                return@map null
            }
            val deviceApi = snapshot.status.deviceApi
            if (deviceApi !is FHTTPDeviceApi) {
                return@map null
            }
            if (snapshot.capabilities == null) {
                return@map null
            }
            deviceApi to snapshot.capabilities
        }.filterNotNull()
        check(currentDelegates.isNotEmpty()) { "No connected devices" }

        val requestedCapabilities = data.attributes.getOrNull(RequestCapabilityKey)

        val filteredDelegates = if (requestedCapabilities.isNullOrEmpty()) {
            currentDelegates.toList()
        } else {
            currentDelegates.filter { it.second.containsAll(requestedCapabilities) }
                .also {
                    if (it.isEmpty()) {
                        error("No delegate with capabilities $requestedCapabilities. Existed: $currentDelegates")
                    }
                }
        }.map { it.first }

        return executeWithRetry(data, filteredDelegates)
    }

    @InternalAPI
    private suspend fun executeWithRetry(
        data: HttpRequestData,
        filteredDelegates: List<FHTTPDeviceApi>
    ): HttpResponseData {
        var lastException: Throwable? = null
        for (delegate in filteredDelegates) {
            runSuspendCatching {
                verbose { "Dispatch request ${data.url} to $delegate" }
                delegate.getDeviceHttpEngine().execute(data)
            }.onSuccess {
                return it
            }.onFailure {
                error(it) { "Delegate $delegate failed, trying next" }
                lastException = it
            }
        }
        throw lastException ?: error("No delegates available")
    }

    // It should be as wide as possible
    override val supportedCapabilities: Set<HttpClientEngineCapability<*>>
        get() = DEFAULT_CAPABILITIES + WebSocketCapability + WebSocketExtensionsCapability
}
