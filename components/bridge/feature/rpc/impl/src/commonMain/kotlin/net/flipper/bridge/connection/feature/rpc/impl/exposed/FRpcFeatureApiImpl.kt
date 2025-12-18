package net.flipper.bridge.connection.feature.rpc.impl.exposed

import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcAssetsApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcBleApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSettingsApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcStreamingApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSystemApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcUpdaterApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcWifiApi

@Suppress("LongParameterList")
class FRpcFeatureApiImpl(
    override val fRpcSystemApi: FRpcSystemApi,
    override val fRpcWifiApi: FRpcWifiApi,
    override val fRpcBleApi: FRpcBleApi,
    override val fRpcSettingsApi: FRpcSettingsApi,
    override val fRpcStreamingApi: FRpcStreamingApi,
    override val fRpcAssetsApi: FRpcAssetsApi,
    override val fRpcUpdaterApi: FRpcUpdaterApi
) : FRpcFeatureApi
