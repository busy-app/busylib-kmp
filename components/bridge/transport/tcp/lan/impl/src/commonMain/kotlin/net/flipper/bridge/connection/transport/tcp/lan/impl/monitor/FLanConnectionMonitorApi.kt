package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.lan.FLanApi
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig

interface FLanConnectionMonitorApi {
    suspend fun startMonitoring()
    fun stopMonitoring()
}

expect fun getConnectionMonitorApi(
    listener: FTransportConnectionStatusListener,
    config: FLanDeviceConnectionConfig,
    scope: CoroutineScope,
    deviceApi: FLanApi
): FLanConnectionMonitorApi
