package net.flipper.bridge.connection.transport.common.api.serial

import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface FHTTPDeviceApi {
    fun getDeviceHttpEngine(): HttpClientEngine

    /**
     * Don't make it just flowO
     * If it's not overridden - it should be emptyList,
     * which means device api has no capabilities
     */
    fun getCapabilities(): Flow<List<FHTTPTransportCapability>> = flowOf(emptyList())
}

fun FHTTPDeviceApi.hasCapability(
    capability: FHTTPTransportCapability
): Flow<Boolean> {
    return getCapabilities()
        .map { it.contains(capability) }
}
