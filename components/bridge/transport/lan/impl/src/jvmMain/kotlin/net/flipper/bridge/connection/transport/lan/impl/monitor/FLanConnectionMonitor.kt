package net.flipper.bridge.connection.transport.lan.impl.monitor

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.lan.FLanDeviceConnectionConfig

actual class FLanConnectionMonitor actual constructor(
    private val listener: FTransportConnectionStatusListener,
    private val config: FLanDeviceConnectionConfig
) {
    actual fun startMonitoring(
        scope: CoroutineScope,
        deviceApi: FConnectedDeviceApi
    ) {
        listener.onStatusUpdate(
            FInternalTransportConnectionStatus.Connected(
                scope = scope,
                deviceApi = deviceApi
            )
        )
    }

    actual fun stopMonitoring() {}
}
