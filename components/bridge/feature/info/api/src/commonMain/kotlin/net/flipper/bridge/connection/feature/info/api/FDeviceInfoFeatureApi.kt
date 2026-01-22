package net.flipper.bridge.connection.feature.info.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusSystem

interface FDeviceInfoFeatureApi : FDeviceFeatureApi {
    suspend fun getDeviceInfo(): Result<BusyBarStatusSystem>
}
