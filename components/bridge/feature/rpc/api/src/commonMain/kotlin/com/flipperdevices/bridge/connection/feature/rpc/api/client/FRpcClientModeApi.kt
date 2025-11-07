package com.flipperdevices.bridge.connection.feature.rpc.api.client

import com.flipperdevices.bridge.connection.feature.rpc.api.model.RpcLinkedAccountInfo
import kotlinx.coroutines.flow.StateFlow

interface FRpcClientModeApi {
    enum class HttpClientMode {
        CRITICAL, DEFAULT
    }

    val httpClientModeFlow: StateFlow<HttpClientMode>

    fun updateClientMode(info: RpcLinkedAccountInfo, email: String?)
}
