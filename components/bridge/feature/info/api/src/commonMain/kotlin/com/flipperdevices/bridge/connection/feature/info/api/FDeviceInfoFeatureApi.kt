package com.flipperdevices.bridge.connection.feature.info.api

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.info.api.model.BSBDeviceInfo

interface FDeviceInfoFeatureApi : FDeviceFeatureApi {
    suspend fun getDeviceInfo(): BSBDeviceInfo?
}
