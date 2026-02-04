package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudApi
import net.flipper.bsb.cloud.barsws.api.BSBWebSocket
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketBarsApi
import net.flipper.bsb.cloud.barsws.api.WebSocketEvent
import net.flipper.bsb.cloud.barsws.api.WebSocketRequest
import net.flipper.core.busylib.ktx.common.launchOnCompletion
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.info
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.seconds

private val INACTIVITY_TIMEOUT = 5.seconds

class CloudDeviceMonitor(
    private val webSocketBarsApi: CloudWebSocketBarsApi,
    private val listener: FTransportConnectionStatusListener,
    private val scope: CoroutineScope,
) : LogTagProvider {
    override val TAG = "CloudDeviceMonitor"
    private val connectingState = MutableStateFlow<FInternalTransportConnectionStatus>(
        FInternalTransportConnectionStatus.Connecting
    )
    private val wsEventFlow = MutableSharedFlow<WebSocketEvent>(replay = 1)

    @Volatile
    private var currentWebSocket: BSBWebSocket? = null

    fun launch(
        deviceApi: FCloudApi,
        deviceId: String
    ) {
        info { "Start monitoring for $deviceId" }
        connectingState
            .onEach { debug { "Change connecting state for $deviceId to $it" } }
            .onEach(listener::onStatusUpdate)
            .launchIn(scope)

        webSocketBarsApi.getWSFlow()
            .flatMapLatest {
                debug { "Receive websocket $it, try to subscribe to it" }
                currentWebSocket = it
                it?.send(WebSocketRequest.Subscribe(deviceId))
                it?.getEventsFlow() ?: flowOf()
            }
            .onEach { debug { "Receive event $it from websocket" } }
            .onEach(wsEventFlow::emit)
            .launchIn(scope)

        scope.launchOnCompletion {
            currentWebSocket?.send(WebSocketRequest.Unsubscribe(deviceId))
        }

        scope.launch {
            wsEventFlow
                .collectLatest {
                    connectingState.emit(
                        FInternalTransportConnectionStatus.Connected(
                            scope = scope,
                            deviceApi = deviceApi
                        )
                    )
                    delay(INACTIVITY_TIMEOUT) // Should be interrupted by any event from websocket
                    connectingState.emit(
                        FInternalTransportConnectionStatus.Connecting
                    )
                }
        }
    }
}
