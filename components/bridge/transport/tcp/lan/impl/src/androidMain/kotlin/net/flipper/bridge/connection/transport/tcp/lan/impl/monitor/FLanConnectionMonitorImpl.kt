package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.lan.FLanApi
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig
import net.flipper.core.busylib.log.LogTagProvider

class FLanConnectionMonitorImpl(
    private val listener: FTransportConnectionStatusListener,
    private val scope: CoroutineScope,
    private val deviceApi: FLanApi
) : FLanConnectionMonitorApi, LogTagProvider {
    override val TAG: String = "FLanConnectionMonitor"

    override suspend fun startMonitoring() {
        listener.onStatusUpdate(
            FInternalTransportConnectionStatus.Connected(
                scope = scope,
                deviceApi = deviceApi
            )
        )
    }

    override fun stopMonitoring() = Unit
}

actual fun getConnectionMonitorApi(
    listener: FTransportConnectionStatusListener,
    config: FLanDeviceConnectionConfig,
    scope: CoroutineScope,
    deviceApi: FLanApi
): FLanConnectionMonitorApi {
    return FLanConnectionMonitorImpl(
        listener,
        scope,
        deviceApi
    )
}
