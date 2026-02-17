package net.flipper.bridge.connection.transport.combined.impl

import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
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
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

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
                if (apis.isEmpty()) flowOf(arrayOf())
                else combine(apis) { it }
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, arrayOf())

    @InternalAPI
    @Suppress("ForbiddenComment")
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val currentDelegates = delegates.value
        check(currentDelegates.isNotEmpty()) { "No connected devices" }

        val (selectedDelegate, _) = currentDelegates.first() // TODO: Add logic to handle capabilities
        info { "Dispatch request $data to $selectedDelegate" }

        return selectedDelegate.getDeviceHttpEngine().execute(data)
    }
}
