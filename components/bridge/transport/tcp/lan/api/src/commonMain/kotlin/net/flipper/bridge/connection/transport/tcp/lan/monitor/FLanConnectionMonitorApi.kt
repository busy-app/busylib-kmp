package net.flipper.bridge.connection.transport.tcp.lan.monitor

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig

interface FLanConnectionMonitorApi {
    fun startMonitoring(
        scope: CoroutineScope,
        deviceApi: FConnectedDeviceApi
    )
    fun stopMonitoring()

    fun interface Factory {
        operator fun invoke(
            listener: FTransportConnectionStatusListener,
            config: FLanDeviceConnectionConfig
        ): FLanConnectionMonitorApi
    }
}
