package net.flipper.bridge.connection.feature.firmwareupdate.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedSharedFlow

interface FFirmwareUpdateFeatureApi : FDeviceFeatureApi {
    val updateStatusFlow: WrappedSharedFlow<UpdateStatus>
    suspend fun setAutoUpdate(isEnabled: Boolean): CResult<Unit>
    suspend fun getAutoUpdate(): CResult<Boolean>

    val updateVersionFlow: WrappedFlow<BsbUpdateVersion>
    val updateVersionChangelog: WrappedFlow<String>
}
