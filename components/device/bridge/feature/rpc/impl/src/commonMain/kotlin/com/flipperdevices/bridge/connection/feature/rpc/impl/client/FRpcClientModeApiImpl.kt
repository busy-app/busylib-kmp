package com.flipperdevices.bridge.connection.feature.rpc.impl.client

import com.flipperdevices.bridge.connection.feature.rpc.api.client.FRpcClientModeApi
import com.flipperdevices.bridge.connection.feature.rpc.api.model.RpcLinkedAccountInfo
import kotlinx.coroutines.flow.MutableStateFlow

class FRpcClientModeApiImpl : FRpcClientModeApi {
    override val httpClientModeFlow = MutableStateFlow(FRpcClientModeApi.HttpClientMode.CRITICAL)
    override fun updateClientMode(info: RpcLinkedAccountInfo, userId: String?) {
        httpClientModeFlow.value = when (info.state) {
            RpcLinkedAccountInfo.State.DISCONNECTED,
            RpcLinkedAccountInfo.State.ERROR,
            RpcLinkedAccountInfo.State.NOT_LINKED -> FRpcClientModeApi.HttpClientMode.DEFAULT

            RpcLinkedAccountInfo.State.LINKED -> {
                if (userId == info.id?.toString()) {
                    FRpcClientModeApi.HttpClientMode.DEFAULT
                } else {
                    FRpcClientModeApi.HttpClientMode.CRITICAL
                }
            }
        }
    }
}
