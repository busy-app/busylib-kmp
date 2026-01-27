package net.flipper.bridge.connection.transport.common.api.serial

import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

interface FHTTPDeviceApi {
    fun getDeviceHttpEngine(): HttpClientEngine

    fun getCapabilities(): StateFlow<List<FHTTPTransportCapability>> =
        MutableStateFlow(emptyList())
}

fun FHTTPDeviceApi.hasCapability(
    capability: FHTTPTransportCapability
): Flow<Boolean> {
    return getCapabilities()
        .map { it.contains(capability) }
}
