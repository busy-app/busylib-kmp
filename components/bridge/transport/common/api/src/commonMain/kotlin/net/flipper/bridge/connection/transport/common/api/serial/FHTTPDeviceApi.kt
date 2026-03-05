package net.flipper.bridge.connection.transport.common.api.serial

import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface FHTTPDeviceApi {
    fun getDeviceHttpEngine(): HttpClientEngine

    /**
     * Default implementation returns a `flowOf(emptyList())`, meaning this device API has no capabilities.
     * Implementations should override this and return a Flow that emits the current list of capabilities
     * instead of just wrapping a static list with `flowOf()`.
     */
    fun getCapabilities(): Flow<List<FHTTPTransportCapability>> = flowOf(emptyList())
}

fun FHTTPDeviceApi.hasCapability(
    capability: FHTTPTransportCapability
): Flow<Boolean> {
    return getCapabilities()
        .map { it.contains(capability) }
}
