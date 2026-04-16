package net.flipper.bridge.connection.transport.tcp.common.monitor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformLatest
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.cancelPrevious
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import kotlin.time.Duration.Companion.seconds

private val INACTIVITY_TIMEOUT = 10.seconds

class WSEventsDeviceMonitor(
    private val eventSource: FStatusStreamingApi,
    private val scope: CoroutineScope,
    private val deviceApi: FConnectedDeviceApi,
    private val config: FDeviceConnectionConfig<*>,
    private val listener: FTransportConnectionStatusListener
) : FConnectionMonitorApi, LogTagProvider {
    override val TAG = "WSEventsDeviceMonitor"
    private val singleJobScope = scope.asSingleJobScope()

    override suspend fun startMonitoring() {
        singleJobScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
            info { "Start monitoring for $config" }

            val wsEventFlow = eventSource
                .getEvents()
            val connectingState = wsEventFlow.transformLatest {
                emit(
                    FInternalTransportConnectionStatus.Connected(
                        scope = scope,
                        deviceApi = deviceApi,
                        connectionType = FInternalTransportConnectionType.CLOUD
                    )
                )
                delay(INACTIVITY_TIMEOUT) // Should be interrupted by any event from websocket
                emit(FInternalTransportConnectionStatus.Connecting)
            }

            connectingState.onEach { info { "Change connecting state for $config to $it" } }
                .collect(listener::onStatusUpdate)
        }
    }

    override fun stopMonitoring() {
        singleJobScope.cancelPrevious()
    }
}
