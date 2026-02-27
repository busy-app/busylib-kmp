package net.flipper.bridge.connection.transport.common.api.serial

import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface FHTTPDeviceApi {
    fun getDeviceHttpEngine(): HttpClientEngine

    fun getCapabilities(): Flow<List<FHTTPTransportCapability>> = flowOf()
}

fun FHTTPDeviceApi.hasCapability(
    capability: FHTTPTransportCapability
): Flow<Boolean> {
    return getCapabilities()
        .map { it.contains(capability) }
}
