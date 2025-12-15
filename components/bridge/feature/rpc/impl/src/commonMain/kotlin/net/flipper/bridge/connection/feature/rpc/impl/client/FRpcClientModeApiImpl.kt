package net.flipper.bridge.connection.feature.rpc.impl.client

import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.bridge.connection.feature.rpc.api.client.FRpcClientModeApi
import net.flipper.bridge.connection.feature.rpc.api.model.RpcLinkedAccountInfo
import kotlin.uuid.Uuid

class FRpcClientModeApiImpl : FRpcClientModeApi {
    override val httpClientModeFlow = MutableStateFlow(FRpcClientModeApi.HttpClientMode.CRITICAL)
    override fun updateClientMode(info: RpcLinkedAccountInfo, userId: Uuid?) {
        httpClientModeFlow.value = when {
            !info.linked -> FRpcClientModeApi.HttpClientMode.DEFAULT
            userId == info.userId -> FRpcClientModeApi.HttpClientMode.DEFAULT
            else -> FRpcClientModeApi.HttpClientMode.CRITICAL
        }
    }
}
