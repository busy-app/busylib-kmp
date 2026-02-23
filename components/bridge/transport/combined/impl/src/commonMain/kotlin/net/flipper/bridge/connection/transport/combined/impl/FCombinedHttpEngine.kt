package net.flipper.bridge.connection.transport.combined.impl

import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineCapability
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.takeFrom
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.flipper.bridge.connection.transport.combined.impl.connections.AutoReconnectConnection
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.bridge.connection.transport.common.api.serial.HEADER_NAME_REQUEST_CAPABILITY
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.verbose

@OptIn(ExperimentalCoroutinesApi::class)
class FCombinedHttpEngine(
    scope: CoroutineScope,
    connectionsFlow: StateFlow<List<AutoReconnectConnection>>,
) : HttpClientEngineBase("bb-combined-http"), LogTagProvider {
    override val TAG = "FCombinedHttpEngine"

    override val config = HttpClientEngineConfig()

    private val delegates = connectionsFlow.flatMapLatest { connections ->
        if (connections.isEmpty()) {
            flowOf(arrayOf())
        } else {
            combine(connections.map { it.stateFlow }) { states ->
                states.filterIsInstance<FInternalTransportConnectionStatus.Connected>()
                    .map { it.deviceApi }.filterIsInstance<FHTTPDeviceApi>()
                    .map { deviceApi ->
                        deviceApi.getCapabilities().map { deviceApi to it }
                    }
            }.flatMapLatest { apis ->
                if (apis.isEmpty()) {
                    flowOf(arrayOf())
                } else {
                    combine(apis) { it }
                }
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, arrayOf())

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val currentDelegates = delegates.value
        check(currentDelegates.isNotEmpty()) { "No connected devices" }

        val requestedCapability = data.headers[HEADER_NAME_REQUEST_CAPABILITY]?.toIntOrNull()?.let {
            FHTTPTransportCapability.entries.getOrNull(it)
        }

        val filteredDelegates = if (requestedCapability == null) {
            currentDelegates.toList()
        } else {
            currentDelegates.filter { it.second.contains(requestedCapability) }.also {
                if (it.isEmpty()) {
                    error("No delegate with capability $requestedCapability")
                }
            }
        }.map { it.first }

        val requestBuilder = HttpRequestBuilder()
            .takeFrom(data)
        requestBuilder.headers.remove(HEADER_NAME_REQUEST_CAPABILITY)
        return executeWithRetry(requestBuilder.build(), filteredDelegates)
    }

    @InternalAPI
    private suspend fun executeWithRetry(
        data: HttpRequestData,
        filteredDelegates: List<FHTTPDeviceApi>
    ): HttpResponseData {
        var lastException: Throwable? = null
        for (delegate in filteredDelegates) {
            try {
                verbose { "Dispatch request $data to $delegate" }
                return delegate.getDeviceHttpEngine().execute(data)
            } catch (e: Throwable) {
                error(e) { "Delegate $delegate failed, trying next" }
                lastException = e
            }
        }
        throw lastException ?: error("No delegates available")
    }

    override val supportedCapabilities: Set<HttpClientEngineCapability<*>>
        get() = delegates.value.map { it.first }
            .flatMap { it.getDeviceHttpEngine().supportedCapabilities }.toSet()
}
