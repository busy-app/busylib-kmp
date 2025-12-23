package net.flipper.bridge.connection.feature.ble.api

import net.flipper.bridge.connection.feature.ble.api.model.FBleStatus
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.busylib.core.wrapper.WrappedSharedFlow

interface FBleFeatureApi : FDeviceFeatureApi {
    fun getBleStatus(): WrappedSharedFlow<FBleStatus>
}
