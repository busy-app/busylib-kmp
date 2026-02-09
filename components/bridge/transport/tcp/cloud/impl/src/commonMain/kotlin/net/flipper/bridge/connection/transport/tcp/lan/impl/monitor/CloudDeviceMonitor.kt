package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudApi
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketBarsApi
import net.flipper.bsb.cloud.barsws.api.WebSocketRequest
import net.flipper.core.busylib.ktx.common.launchOnCompletion
import net.flipper.core.busylib.ktx.common.onLatest
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.info
import kotlin.time.Duration.Companion.seconds

private val INACTIVITY_TIMEOUT = 10.seconds

class CloudDeviceMonitor(
    private val webSocketBarsApi: CloudWebSocketBarsApi,
    private val scope: CoroutineScope,
    private val deviceApi: FCloudApi,
    private val deviceId: String
) : LogTagProvider {
    override val TAG = "CloudDeviceMonitor"
    private val currentWebSocketFlow = webSocketBarsApi
        .getWSFlow()
        .shareIn(scope, SharingStarted.Lazily, 1)
    private val wsEventFlow = currentWebSocketFlow
        .flatMapLatest { it?.getEventsFlow() ?: flowOf() }
        .shareIn(scope, SharingStarted.Lazily, 1)

    private val connectingState = wsEventFlow.mapLatest {
        FInternalTransportConnectionStatus.Connected(
            scope = scope,
            deviceApi = deviceApi
        )
        delay(INACTIVITY_TIMEOUT) // Should be interrupted by any event from websocket
        FInternalTransportConnectionStatus.Connecting
    }.stateIn(scope, SharingStarted.Lazily, FInternalTransportConnectionStatus.Connecting)

    private fun collectWebSocketChange() {
        currentWebSocketFlow
            .onLatest { it?.send(WebSocketRequest.Subscribe(deviceId)) }
            .launchIn(scope)
    }

    init {
        collectWebSocketChange()

        scope.launchOnCompletion {
            currentWebSocketFlow.firstOrNull()?.send(WebSocketRequest.Unsubscribe(deviceId))
        }
    }

    fun subscribe(listener: FTransportConnectionStatusListener) {
        info { "Start monitoring for $deviceId" }
        connectingState
            .onEach { debug { "Change connecting state for $deviceId to $it" } }
            .onEach(listener::onStatusUpdate)
            .launchIn(scope)
    }

    class Factory(
        private val webSocketBarsApi: CloudWebSocketBarsApi,
        private val scope: CoroutineScope,
    ) {
        fun create(
            deviceApi: FCloudApi,
            deviceId: String
        ): CloudDeviceMonitor {
            return CloudDeviceMonitor(
                webSocketBarsApi = webSocketBarsApi,
                scope = scope,
                deviceApi = deviceApi,
                deviceId = deviceId
            )
        }
    }
}
