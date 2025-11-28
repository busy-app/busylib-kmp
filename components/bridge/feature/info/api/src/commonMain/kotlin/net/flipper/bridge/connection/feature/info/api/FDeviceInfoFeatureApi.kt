package net.flipper.bridge.connection.feature.info.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.info.api.model.BSBDeviceInfo

interface FDeviceInfoFeatureApi : FDeviceFeatureApi {
    suspend fun getDeviceInfo(): BSBDeviceInfo?

    fun getDeviceName(scope: CoroutineScope): StateFlow<String>
}
