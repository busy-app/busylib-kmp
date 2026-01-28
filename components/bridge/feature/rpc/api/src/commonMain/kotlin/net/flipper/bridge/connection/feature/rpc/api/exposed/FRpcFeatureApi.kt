package net.flipper.bridge.connection.feature.rpc.api.exposed

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi

@Suppress("TooManyFunctions")
interface FRpcFeatureApi : FDeviceFeatureApi {
    val fRpcSystemApi: FRpcSystemApi
    val fRpcWifiApi: FRpcWifiApi
    val fRpcBleApi: FRpcBleApi
    val fRpcSettingsApi: FRpcSettingsApi
    val fRpcStreamingApi: FRpcStreamingApi
    val fRpcAssetsApi: FRpcAssetsApi
    val fRpcUpdaterApi: FRpcUpdaterApi
    val fRpcMatterApi: FRpcMatterApi
}
