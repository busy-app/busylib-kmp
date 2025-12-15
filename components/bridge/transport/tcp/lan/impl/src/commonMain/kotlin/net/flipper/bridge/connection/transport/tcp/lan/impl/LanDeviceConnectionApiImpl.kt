package net.flipper.bridge.connection.transport.tcp.lan.impl

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.lan.FLanApi
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig
import net.flipper.bridge.connection.transport.tcp.lan.LanDeviceConnectionApi
import net.flipper.bridge.connection.transport.tcp.lan.impl.monitor.FLanConnectionMonitorApi
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(BusyLibGraph::class, LanDeviceConnectionApi::class)
class LanDeviceConnectionApiImpl(
    private val connectionMonitor: FLanConnectionMonitorApi.Factory
) : LanDeviceConnectionApi {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FLanDeviceConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): Result<FLanApi> = runCatching {
        val lanApi = FLanApiImpl(
            listener = listener,
            connectionMonitor = connectionMonitor,
            config = config,
            scope = scope
        )
        lanApi.startMonitoring()
        return@runCatching lanApi
    }
}
