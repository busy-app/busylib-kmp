package net.flipper.bridge.connection.feature.info.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.info.api.model.BSBDeviceInfo
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FDeviceInfoFeatureApi : FDeviceFeatureApi {
    suspend fun getDeviceInfo(): BSBDeviceInfo?

    fun getDeviceName(): WrappedFlow<String>
}
