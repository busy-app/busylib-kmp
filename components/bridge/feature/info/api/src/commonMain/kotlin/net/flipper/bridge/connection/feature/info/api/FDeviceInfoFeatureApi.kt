package net.flipper.bridge.connection.feature.info.api

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.info.api.model.BSBDeviceInfo
import net.flipper.busylib.core.wrapper.WrappedStateFlow

interface FDeviceInfoFeatureApi : FDeviceFeatureApi {
    suspend fun getDeviceInfo(): BSBDeviceInfo?

    fun getDeviceName(scope: CoroutineScope): WrappedStateFlow<String>
}
