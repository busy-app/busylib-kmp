package net.flipper.bridge.connection.transport.combined.impl

import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.flipper.bridge.connection.transport.combined.impl.connections.AutoReconnectConnection
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class FCombinedHttpEngine(
    scope: CoroutineScope,
    connections: List<AutoReconnectConnection>,
) : HttpClientEngineBase("bb-combined-http"), LogTagProvider {
    override val TAG = "FCombinedHttpEngine"

    override val config = HttpClientEngineConfig()

    private val delegates = combine(connections.map { it.stateFlow }) { states ->
        states.filterIsInstance<FInternalTransportConnectionStatus.Connected>() // Only connected
            .map { it.deviceApi }.filterIsInstance<FHTTPDeviceApi>() // Only supported HTTP
            .map { deviceApi ->
                deviceApi.getCapabilities().map { deviceApi to it }
            } // Extract capabilities
    }.flatMapLatest { apis ->
        combine(apis) { it } // Array<Pair<FHTTPDeviceApi, List<FHTTPTransportCapability>>>
    }.stateIn(scope, SharingStarted.Eagerly, arrayOf())

    @InternalAPI
    @Suppress("ForbiddenComment")
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val currentDelegates = delegates.value
        check(currentDelegates.isEmpty()) { "No connected devices" }

        val (selectedDelegate, _) = currentDelegates.first() // TODO: Add logic to handle capabilities
        info { "Dispatch request $data to $selectedDelegate" }

        return selectedDelegate.getDeviceHttpEngine().execute(data)
    }
}
