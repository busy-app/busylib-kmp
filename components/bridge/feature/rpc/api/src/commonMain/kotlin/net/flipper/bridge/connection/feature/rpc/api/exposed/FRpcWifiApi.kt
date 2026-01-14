package net.flipper.bridge.connection.feature.rpc.api.exposed

import net.flipper.bridge.connection.feature.rpc.api.model.ConnectRequestConfig
import net.flipper.bridge.connection.feature.rpc.api.model.NetworkResponse
import net.flipper.bridge.connection.feature.rpc.api.model.StatusResponse
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse

interface FRpcWifiApi {
    suspend fun getWifiStatus(ignoreCache: Boolean): Result<StatusResponse>
    suspend fun connectWifi(config: ConnectRequestConfig): Result<SuccessResponse>
    suspend fun disconnectWifi(): Result<SuccessResponse>
    suspend fun getWifiNetworks(): Result<NetworkResponse>
}
