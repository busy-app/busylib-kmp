package net.flipper.bridge.connection.feature.about.api

import net.flipper.bridge.connection.feature.about.model.BusyBarAboutDevice
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.busylib.core.wrapper.CResult

interface FAboutFeatureApi : FDeviceFeatureApi {
    suspend fun getAboutDevice(): CResult<BusyBarAboutDevice>
}
