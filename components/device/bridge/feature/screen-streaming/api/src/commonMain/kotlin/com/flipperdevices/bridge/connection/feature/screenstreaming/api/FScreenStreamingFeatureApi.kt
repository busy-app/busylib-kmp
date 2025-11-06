package com.flipperdevices.bridge.connection.feature.screenstreaming.api

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.screenstreaming.model.BusyImageFormat

interface FScreenStreamingFeatureApi : FDeviceFeatureApi {
    suspend fun getBusyImageFormat(): Result<BusyImageFormat>
}
