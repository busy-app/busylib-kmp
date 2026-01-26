package net.flipper.bridge.connection.transport.combined.noop

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.combined.CombinedConnectionApi
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionApi
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Suppress("ForbiddenComment")
// TODO: Remove this workaround for optional binds when migrate on metro finished
@Inject
@ContributesBinding(BusyLibGraph::class, CombinedConnectionApi::class)
class CombinedConnectionApiNoop : CombinedConnectionApi {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FCombinedConnectionConfig,
        listener: FTransportConnectionStatusListener,
        connectionBuilder: FDeviceConfigToConnection
    ): Result<FCombinedConnectionApi> = Result.failure(NotImplementedError())
}
