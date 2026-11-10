package net.flipper.bridge.connection.feature.telemetry.api.fake

import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcAssetsApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcBleApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcBusyApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcMatterApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSettingsApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcStorageApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcStreamingApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSystemApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcTimeZoneApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcUpdaterApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcWifiApi

internal class FakeRpcFeatureApi(
    override val fRpcSystemApi: FRpcSystemApi,
    override val fRpcSettingsApi: FRpcSettingsApi
) : FRpcFeatureApi {
    override val fRpcWifiApi: FRpcWifiApi
        get() = error("not used")
    override val fRpcBleApi: FRpcBleApi
        get() = error("not used")
    override val fRpcStreamingApi: FRpcStreamingApi
        get() = error("not used")
    override val fRpcAssetsApi: FRpcAssetsApi
        get() = error("not used")
    override val fRpcStorageApi: FRpcStorageApi
        get() = error("not used")
    override val fRpcUpdaterApi: FRpcUpdaterApi
        get() = error("not used")
    override val fRpcMatterApi: FRpcMatterApi
        get() = error("not used")
    override val fRpcTimeZoneApi: FRpcTimeZoneApi
        get() = error("not used")
    override val fRpcBusyApi: FRpcBusyApi
        get() = error("not used")
}
