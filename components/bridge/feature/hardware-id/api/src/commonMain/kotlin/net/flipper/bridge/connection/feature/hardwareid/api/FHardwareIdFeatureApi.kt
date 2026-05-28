package net.flipper.bridge.connection.feature.hardwareid.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FHardwareIdFeatureApi : FDeviceFeatureApi {
    fun getHardwareIdFlow(): WrappedFlow<String?>
}
