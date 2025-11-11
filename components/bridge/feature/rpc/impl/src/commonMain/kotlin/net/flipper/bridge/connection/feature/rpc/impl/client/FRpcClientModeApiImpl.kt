package net.flipper.bridge.connection.feature.rpc.impl.client

import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.bridge.connection.feature.rpc.api.client.FRpcClientModeApi
import net.flipper.bridge.connection.feature.rpc.api.model.RpcLinkedAccountInfo

class FRpcClientModeApiImpl : FRpcClientModeApi {
    override val httpClientModeFlow = MutableStateFlow(FRpcClientModeApi.HttpClientMode.CRITICAL)
    override fun updateClientMode(info: RpcLinkedAccountInfo, email: String?) {
        httpClientModeFlow.value = when (info.state) {
            RpcLinkedAccountInfo.State.DISCONNECTED,
            RpcLinkedAccountInfo.State.ERROR,
            RpcLinkedAccountInfo.State.NOT_LINKED -> FRpcClientModeApi.HttpClientMode.DEFAULT

            RpcLinkedAccountInfo.State.LINKED -> {
                if (email == info.email) {
                    FRpcClientModeApi.HttpClientMode.DEFAULT
                } else {
                    FRpcClientModeApi.HttpClientMode.CRITICAL
                }
            }
        }
    }
}
