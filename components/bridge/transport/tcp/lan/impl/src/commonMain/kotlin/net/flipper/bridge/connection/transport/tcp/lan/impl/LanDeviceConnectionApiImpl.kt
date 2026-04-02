package net.flipper.bridge.connection.transport.tcp.lan.impl

import kotlinx.coroutines.CoroutineScope
import dev.zacsweers.metro.Inject
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.lan.FLanApi
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig
import net.flipper.bridge.connection.transport.tcp.lan.LanDeviceConnectionApi
import net.flipper.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding

@Inject
@ContributesBinding(BusyLibGraph::class, binding = binding<LanDeviceConnectionApi>())
class LanDeviceConnectionApiImpl : LanDeviceConnectionApi {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FLanDeviceConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): Result<FLanApi> = runCatching {
        val lanApi = FLanApiImpl(
            listener = listener,
            currentConfig = config,
            scope = scope
        )
        lanApi.startMonitoring()
        return@runCatching lanApi
    }
}
