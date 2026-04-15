package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionType
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

private val INACTIVITY_TIMEOUT = 10.seconds

class CloudDeviceMonitor(
    private val eventSource: FStatusStreamingApi,
    private val scope: CoroutineScope,
    private val deviceApi: FConnectedDeviceApi,
    private val deviceId: Uuid,
) : LogTagProvider {
    override val TAG = "CloudDeviceMonitor"
    private val wsEventFlow = eventSource
        .getEvents()
        .shareIn(scope, SharingStarted.Lazily, 1)

    private val connectingState = wsEventFlow.transformLatest {
        emit(
            FInternalTransportConnectionStatus.Connected(
                scope = scope,
                deviceApi = deviceApi,
                connectionType = FInternalTransportConnectionType.CLOUD
            )
        )
        delay(INACTIVITY_TIMEOUT) // Should be interrupted by any event from websocket
        emit(FInternalTransportConnectionStatus.Connecting(FInternalTransportConnectionType.CLOUD))
    }.stateIn(
        scope,
        SharingStarted.Lazily,
        FInternalTransportConnectionStatus.Connecting(FInternalTransportConnectionType.CLOUD)
    )

    fun subscribe(listener: FTransportConnectionStatusListener) {
        info { "Start monitoring for $deviceId" }
        connectingState
            .onEach { info { "Change connecting state for $deviceId to $it" } }
            .onEach(listener::onStatusUpdate)
            .launchIn(scope)
    }
}
