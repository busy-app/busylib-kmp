package com.flipperdevices.bridge.connection.feature.rpc.api.exposed

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.model.BleStatusResponse
import com.flipperdevices.bridge.connection.feature.rpc.api.model.BusyBarStatus
import com.flipperdevices.bridge.connection.feature.rpc.api.model.BusyBarStatusPower
import com.flipperdevices.bridge.connection.feature.rpc.api.model.BusyBarStatusSystem
import com.flipperdevices.bridge.connection.feature.rpc.api.model.BusyBarVersion
import com.flipperdevices.bridge.connection.feature.rpc.api.model.ConnectRequestConfig
import com.flipperdevices.bridge.connection.feature.rpc.api.model.NetworkResponse
import com.flipperdevices.bridge.connection.feature.rpc.api.model.SuccessResponse
import com.flipperdevices.bridge.connection.feature.rpc.api.model.WifiStatusResponse

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
