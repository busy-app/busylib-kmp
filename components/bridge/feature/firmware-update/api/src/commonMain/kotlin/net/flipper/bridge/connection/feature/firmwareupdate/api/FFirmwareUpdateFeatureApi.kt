package net.flipper.bridge.connection.feature.firmwareupdate.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedSharedFlow

interface FFirmwareUpdateFeatureApi : FDeviceFeatureApi {
    val availableVersionFlow: WrappedSharedFlow<String?>
    val updateStatusFlow: WrappedSharedFlow<BsbUpdateStatus>
    val isAutoUpdateEnabledFlow: WrappedSharedFlow<Boolean>
    suspend fun setAutoUpdate(isEnabled: Boolean): CResult<Unit>

    val updateVersionFlow: WrappedFlow<BsbUpdateVersion>
    val updateVersionChangelog: WrappedFlow<String>
}
