package com.flipperdevices.bridge.connection.connectionbuilder.impl

import com.flipperdevices.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import com.flipperdevices.bridge.connection.transport.common.api.DeviceConnectionApi
import com.flipperdevices.bridge.connection.transport.common.api.FConnectedDeviceApi
import com.flipperdevices.bridge.connection.transport.common.api.FDeviceConnectionConfig
import com.flipperdevices.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass

class FDeviceConfigToConnectionImpl(
    private val configToConnectionMap: Map<KClass<*>, DeviceConnectionApi<*, *>>
) : FDeviceConfigToConnection {
    override suspend fun <API : FConnectedDeviceApi, CONFIG : FDeviceConnectionConfig<API>> connect(
        scope: CoroutineScope,
        config: CONFIG,
        listener: FTransportConnectionStatusListener
    ): Result<API> = runCatching {
        val connectionApiUntyped = configToConnectionMap.entries.find { (qualifier, _) ->
            qualifier.isInstance(
                config
            )
        } ?: throw NotImplementedError("Can't find connection for config $config")

        @Suppress("UNCHECKED_CAST")
        val connectionApi =
            connectionApiUntyped.value as? DeviceConnectionApi<API, CONFIG>
                ?: throw NotImplementedError("Can't map to connection api")

        connectionApi.connect(scope, config, listener).getOrThrow()
    }
}
