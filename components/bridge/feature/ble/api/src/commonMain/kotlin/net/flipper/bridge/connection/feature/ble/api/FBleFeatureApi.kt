package net.flipper.bridge.connection.feature.ble.api

import net.flipper.bridge.connection.feature.ble.api.model.FBleStatus
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FBleFeatureApi : FDeviceFeatureApi {
    fun getBleStatus(): WrappedFlow<FBleStatus>
}
