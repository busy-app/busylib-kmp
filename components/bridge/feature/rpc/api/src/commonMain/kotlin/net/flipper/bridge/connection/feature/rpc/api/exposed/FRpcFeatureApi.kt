package net.flipper.bridge.connection.feature.rpc.api.exposed

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.generated.api.AssetsApi
import net.flipper.bridge.connection.feature.rpc.generated.api.BLEApi
import net.flipper.bridge.connection.feature.rpc.generated.api.BusyApi
import net.flipper.bridge.connection.feature.rpc.generated.api.SettingsApi
import net.flipper.bridge.connection.feature.rpc.generated.api.SmartHomeApi
import net.flipper.bridge.connection.feature.rpc.generated.api.StreamingApi
import net.flipper.bridge.connection.feature.rpc.generated.api.SystemApi
import net.flipper.bridge.connection.feature.rpc.generated.api.TimeApi
import net.flipper.bridge.connection.feature.rpc.generated.api.UpdaterApi
import net.flipper.bridge.connection.feature.rpc.generated.api.WiFiApi

@Suppress("TooManyFunctions")
interface FRpcFeatureApi : FDeviceFeatureApi {
    val fRpcSystemApi: SystemApi
    val fRpcWifiApi: WiFiApi
    val fRpcBleApi: BLEApi
    val fRpcSettingsApi: SettingsApi
    val fRpcStreamingApi: StreamingApi
    val fRpcAssetsApi: AssetsApi
    val fRpcUpdaterApi: UpdaterApi
    val fRpcMatterApi: SmartHomeApi
    val fRpcTimeZoneApi: TimeApi
    val fRpcBusyApi: BusyApi
}
