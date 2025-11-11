package net.flipper.bridge.connection.transport.common.api

import kotlinx.coroutines.CoroutineScope

interface DeviceConnectionApi<API : FConnectedDeviceApi, CONFIG : FDeviceConnectionConfig<API>> {
    suspend fun connect(
        scope: CoroutineScope,
        config: CONFIG,
        listener: FTransportConnectionStatusListener
    ): Result<API>
}

/**
 * Used to bypass DI issues and avoid having generics work in the dependency graph
 */
class DeviceConnectionApiHolder(val deviceConnectionApi: DeviceConnectionApi<*, *>)
fun DeviceConnectionApi<*, *>.toHolder() = DeviceConnectionApiHolder(this)
