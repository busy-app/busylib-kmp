package com.flipperdevices.bridge.connection.transport.common.api.serial

import io.ktor.client.engine.HttpClientEngine

interface FHTTPDeviceApi {
    fun getDeviceHttpEngine(): HttpClientEngine
}
