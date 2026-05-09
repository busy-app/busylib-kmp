package net.flipper.bridge.connection.feature.rpc.generated.api

import net.flipper.bridge.connection.feature.rpc.generated.model.ConnectRequestConfig
import net.flipper.bridge.connection.feature.rpc.generated.model.NetworkResponse
import net.flipper.bridge.connection.feature.rpc.generated.model.StatusResponse
import net.flipper.bridge.connection.feature.rpc.generated.model.SuccessResponse

interface WiFiApi {

    /**
     */
    suspend fun apiWifiConnectPost(connectRequestConfig: ConnectRequestConfig): kotlin.Result<SuccessResponse>

    /**
     */
    suspend fun apiWifiDisconnectPost(): kotlin.Result<SuccessResponse>

    /**
     */
    suspend fun apiWifiStatusGet(): kotlin.Result<StatusResponse>

    /**
     */
    suspend fun getWifiNetworks(): kotlin.Result<NetworkResponse>
}
