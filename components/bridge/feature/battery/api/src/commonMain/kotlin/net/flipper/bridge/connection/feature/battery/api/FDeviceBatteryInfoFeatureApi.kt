package net.flipper.bridge.connection.feature.battery.api

import net.flipper.bridge.connection.feature.battery.model.BSBDeviceBatteryInfo
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FDeviceBatteryInfoFeatureApi : FDeviceFeatureApi {
    fun getDeviceBatteryInfo(): WrappedFlow<BSBDeviceBatteryInfo>
}
