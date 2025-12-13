package net.flipper.bridge.connection.transport.lan.impl.monitor

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.lan.FLanDeviceConnectionConfig

expect class FLanConnectionMonitor(
    listener: FTransportConnectionStatusListener,
    config: FLanDeviceConnectionConfig
) {
    fun startMonitoring(
        scope: CoroutineScope,
        deviceApi: FConnectedDeviceApi
    )
    fun stopMonitoring()
}
