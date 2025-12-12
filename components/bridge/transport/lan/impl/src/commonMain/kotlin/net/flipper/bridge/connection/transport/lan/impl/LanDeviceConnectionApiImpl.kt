package net.flipper.bridge.connection.transport.lan.impl

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.lan.FLanApi
import net.flipper.bridge.connection.transport.lan.FLanDeviceConnectionConfig
import net.flipper.bridge.connection.transport.lan.LanDeviceConnectionApi
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(BusyLibGraph::class, LanDeviceConnectionApi::class)
class LanDeviceConnectionApiImpl : LanDeviceConnectionApi {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FLanDeviceConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): Result<FLanApi> = runCatching {
        val lanApi = FLanApiImpl(listener, config, scope)
        return@runCatching lanApi
    }
}
