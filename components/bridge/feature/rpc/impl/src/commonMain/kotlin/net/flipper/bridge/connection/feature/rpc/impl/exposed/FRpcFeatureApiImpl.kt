package net.flipper.bridge.connection.feature.rpc.impl.exposed

import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
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

@Suppress("LongParameterList")
class FRpcFeatureApiImpl(
    override val fRpcSystemApi: SystemApi,
    override val fRpcWifiApi: WiFiApi,
    override val fRpcBleApi: BLEApi,
    override val fRpcSettingsApi: SettingsApi,
    override val fRpcStreamingApi: StreamingApi,
    override val fRpcAssetsApi: AssetsApi,
    override val fRpcUpdaterApi: UpdaterApi,
    override val fRpcMatterApi: SmartHomeApi,
    override val fRpcTimeZoneApi: TimeApi,
    override val fRpcBusyApi: BusyApi,
) : FRpcFeatureApi
