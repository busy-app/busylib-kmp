package net.flipper.bridge.connection.feature.info.api

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusSystem
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarVersion
import net.flipper.bridge.connection.feature.rpc.api.model.StatusFirmware
import net.flipper.busylib.core.wrapper.CResult

interface FDeviceInfoFeatureApi : FDeviceFeatureApi {
    suspend fun getDeviceInfo(): CResult<BusyBarStatusSystem>
    suspend fun getDeviceFirmware(): CResult<StatusFirmware>
    val deviceVersionFlow: Flow<BusyBarVersion>
}
