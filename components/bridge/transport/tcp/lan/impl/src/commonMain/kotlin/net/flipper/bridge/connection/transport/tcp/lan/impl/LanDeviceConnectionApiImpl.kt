package net.flipper.bridge.connection.transport.tcp.lan.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.lan.FLanApi
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig
import net.flipper.bridge.connection.transport.tcp.lan.LanDeviceConnectionApi
import net.flipper.bridge.lanmonitor.api.LanMonitorApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.runSuspendCatching

@Inject
@ContributesBinding(BusyLibGraph::class, binding = binding<LanDeviceConnectionApi>())
class LanDeviceConnectionApiImpl(
    private val lanMonitorApi: LanMonitorApi
) : LanDeviceConnectionApi {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FLanDeviceConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): Result<FLanApi> = runSuspendCatching {
        val lanApi = FLanApiImpl(
            listener = listener,
            currentConfig = config,
            scope = scope,
            lanMonitorApi = lanMonitorApi
        )
        lanApi.startMonitoring()
        return@runSuspendCatching lanApi
    }
}
