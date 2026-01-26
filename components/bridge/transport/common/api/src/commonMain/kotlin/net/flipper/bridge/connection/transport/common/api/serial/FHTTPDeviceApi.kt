package net.flipper.bridge.connection.transport.common.api.serial

import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface FHTTPDeviceApi {
    fun getDeviceHttpEngine(): HttpClientEngine

    fun hasCapability(
        capability: FHTTPTransportCapability
    ): StateFlow<Boolean> = MutableStateFlow(false)
}
