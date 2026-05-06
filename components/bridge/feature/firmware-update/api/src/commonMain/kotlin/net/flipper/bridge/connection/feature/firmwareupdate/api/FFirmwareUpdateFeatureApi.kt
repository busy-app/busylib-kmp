package net.flipper.bridge.connection.feature.firmwareupdate.api

import kotlinx.coroutines.flow.StateFlow
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedSharedFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow

interface FFirmwareUpdateFeatureApi : FDeviceFeatureApi {
    val updateStatusFlow: WrappedStateFlow<BsbUpdateStatus>
    val isAutoUpdateEnabledFlow: WrappedSharedFlow<Boolean>
    suspend fun setAutoUpdate(isEnabled: Boolean): CResult<Unit>

    val updateVersionFlow: WrappedStateFlow<BsbUpdateVersion>
    val updateVersionChangelog: WrappedFlow<String?>
}
