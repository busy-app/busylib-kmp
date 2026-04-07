package net.flipper.bridge.connection.feature.info.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.info.model.BsbBusyBarStatusSystem
import net.flipper.bridge.connection.feature.info.model.BsbBusyBarVersion
import net.flipper.bridge.connection.feature.info.model.BsbStatusFirmware
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FDeviceInfoFeatureApi : FDeviceFeatureApi {
    suspend fun getDeviceInfo(): CResult<BsbBusyBarStatusSystem>
    suspend fun getDeviceFirmware(): CResult<BsbStatusFirmware>
    val deviceVersionFlow: WrappedFlow<BsbBusyBarVersion>
}
