package net.flipper.bridge.connection.transport.common.api.serial

import io.ktor.client.engine.HttpClientEngine

interface FHTTPDeviceApi {
    fun getDeviceHttpEngine(): HttpClientEngine

    fun hasCapability(capability: FHTTPTransportCapability): Boolean = false
}
