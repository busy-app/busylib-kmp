package net.flipper.bridge.connection.transport.combined.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.combined.CombinedConnectionApi
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionApi
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(BusyLibGraph::class, CombinedConnectionApi::class)
class CombinedConnectionApiImpl : CombinedConnectionApi {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FCombinedConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): Result<FCombinedConnectionApi> = runCatching {
        listener.onStatusUpdate(FInternalTransportConnectionStatus.Connecting)

        TODO()
    }
}
