package net.flipper.bridge.connection.feature.rpc.api.exposed

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.BleStatusResponse
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatus
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusPower
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusSystem
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarVersion
import net.flipper.bridge.connection.feature.rpc.api.model.ConnectRequestConfig
import net.flipper.bridge.connection.feature.rpc.api.model.NetworkResponse
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.bridge.connection.feature.rpc.api.model.WifiStatusResponse

@Suppress("TooManyFunctions")
interface FRpcFeatureApi : FDeviceFeatureApi {
    suspend fun getVersion(): Result<BusyBarVersion>

    suspend fun getStatus(): Result<BusyBarStatus>
    suspend fun getStatusSystem(): Result<BusyBarStatusSystem>
    suspend fun getStatusPower(): Result<BusyBarStatusPower>
    suspend fun getWifiNetworks(): Result<NetworkResponse>
    suspend fun connectWifi(config: ConnectRequestConfig): Result<SuccessResponse>
    suspend fun disconnectWifi(): Result<SuccessResponse>
    suspend fun getWifiStatus(): Result<WifiStatusResponse>
    suspend fun getBleStatus(): Result<BleStatusResponse>
    suspend fun getScreen(display: Int): Result<String>
}
