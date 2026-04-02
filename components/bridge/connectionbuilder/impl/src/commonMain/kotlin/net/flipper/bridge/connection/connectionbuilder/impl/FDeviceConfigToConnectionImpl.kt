package net.flipper.bridge.connection.connectionbuilder.impl

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.combined.CombinedConnectionApi
import net.flipper.bridge.connection.transport.combined.FCombinedConnectionConfig
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApi
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApiHolder
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlin.reflect.KClass

@ContributesBinding(BusyLibGraph::class, binding = binding<FDeviceConfigToConnection>())
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

        val connectionApiUntyped = configToConnectionMap.entries
            .find { (qualifier, _) -> qualifier.isInstance(config) }
            ?: throw NotImplementedError("Can't find connection for config $config")

        @Suppress("UNCHECKED_CAST")
        val connectionApi = connectionApiUntyped.value
            .deviceConnectionApi as? DeviceConnectionApi<API, CONFIG>
            ?: throw NotImplementedError("Can't map to connection api")

        connectionApi.connect(scope, config, listener).getOrThrow()
    }
}
