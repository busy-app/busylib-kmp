package net.flipper.bridge.connection.feature.finishsetup.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.finishsetup.model.FFinishSetupState
import net.flipper.busylib.core.wrapper.WrappedSharedFlow

interface FFinishSetupFeatureApi : FDeviceFeatureApi {
    val taskListResourceFlow: WrappedSharedFlow<FFinishSetupState>
}
