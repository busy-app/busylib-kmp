package net.flipper.bridge.connection.connectionbuilder.impl

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.combined.CombinedConnectionApi
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionConfig
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApi
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApiHolder
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import kotlin.reflect.KClass

@Inject
@ContributesBinding(BusyLibGraph::class, FDeviceConfigToConnection::class)
class FDeviceConfigToConnectionImpl(
    private val configToConnectionMap: Map<KClass<*>, DeviceConnectionApiHolder>,
    private val combinedConnectionApi: CombinedConnectionApi
) : FDeviceConfigToConnection {
    override suspend fun <API : FConnectedDeviceApi, CONFIG : FDeviceConnectionConfig<API>> connect(
        scope: CoroutineScope,
        config: CONFIG,
        listener: FTransportConnectionStatusListener
    ): Result<API> = runCatching {
        if (config is FCombinedConnectionConfig) {
            @Suppress("UNCHECKED_CAST")
            val result = combinedConnectionApi.connect(
                scope = scope,
                config = config,
                listener = listener,
                connectionBuilder = this
            ) as Result<API>

            return@runCatching result.getOrThrow()
        }

        val connectionApiUntyped = configToConnectionMap.entries.find { (qualifier, _) ->
            qualifier.isInstance(
                config
            )
        } ?: throw NotImplementedError("Can't find connection for config $config")

        @Suppress("UNCHECKED_CAST")
        val connectionApi =
            connectionApiUntyped.value.deviceConnectionApi as? DeviceConnectionApi<API, CONFIG>
                ?: throw NotImplementedError("Can't map to connection api")

        connectionApi.connect(scope, config, listener).getOrThrow()
    }
}
