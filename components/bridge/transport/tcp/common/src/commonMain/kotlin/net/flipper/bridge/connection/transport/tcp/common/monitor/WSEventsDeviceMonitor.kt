package net.flipper.bridge.connection.transport.tcp.common.monitor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalDisconnectedReason
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.cancelPrevious
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import kotlin.time.Duration.Companion.seconds

private val INACTIVITY_TIMEOUT = 10.seconds

/**
 * While Connecting, the stream may stay silent up to
 * 30 seconds before reconnect is triggered
 */
private val SILENT_CONNECTION_TIMEOUT = 30.seconds

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
            val connectingStatus = FInternalTransportConnectionStatus
                .Connecting(config.getTransportTypes())

            eventSource.getEvents()
                .transformLatest {
                    emit(
                        FInternalTransportConnectionStatus.Connected(
                            scope = scope,
                            deviceApi = deviceApi,
                            connectionTypes = config.getTransportTypes()
                        )
                    )
                    delay(INACTIVITY_TIMEOUT) // Should be interrupted by any event from websocket
                    emit(connectingStatus)
                }
                .onStart { emit(connectingStatus) }
                .distinctUntilChanged()
                .transformLatest { status ->
                    emit(status)
                    if (status is FInternalTransportConnectionStatus.Connecting) {
                        delay(SILENT_CONNECTION_TIMEOUT)
                        info { "No events for $SILENT_CONNECTION_TIMEOUT, treat $config as dead" }
                        emit(
                            FInternalTransportConnectionStatus
                                .Disconnected(FInternalDisconnectedReason.OTHER)
                        )
                    }
                }
                .onEach { info { "Change connecting state for $config to $it" } }
                .collect(listener::onStatusUpdate)
        }
    }

    override fun stopMonitoring() {
        singleJobScope.cancelPrevious()
    }
}
