package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.lan.FLanApi
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig

actual fun getConnectionMonitorApi(
    listener: FTransportConnectionStatusListener,
    config: FLanDeviceConnectionConfig,
    scope: CoroutineScope,
    deviceApi: FLanApi
): FLanConnectionMonitorApi {
    return FHttpLanConnectionMonitor(
        listener,
        config,
        scope,
        deviceApi
    )
}
