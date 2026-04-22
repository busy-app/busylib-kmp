package net.flipper.bsb.watchers.provisioning.fakes

import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcAssetsApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcBleApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcMatterApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSettingsApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcStreamingApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSystemApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcTimeZoneApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcUpdaterApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcWifiApi

internal class FakeRpcFeatureApi(
    private val systemApi: FRpcSystemApi
) : FRpcFeatureApi {
    override val fRpcSystemApi: FRpcSystemApi get() = systemApi
    override val fRpcWifiApi: FRpcWifiApi get() = error("Not used in test")
    override val fRpcBleApi: FRpcBleApi get() = error("Not used in test")
    override val fRpcSettingsApi: FRpcSettingsApi get() = error("Not used in test")
    override val fRpcStreamingApi: FRpcStreamingApi get() = error("Not used in test")
    override val fRpcAssetsApi: FRpcAssetsApi get() = error("Not used in test")
    override val fRpcUpdaterApi: FRpcUpdaterApi get() = error("Not used in test")
    override val fRpcMatterApi: FRpcMatterApi get() = error("Not used in test")
    override val fRpcTimeZoneApi: FRpcTimeZoneApi get() = error("Not used in test")
}
