package net.flipper.bridge.connection.feature.screenstreaming.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.screenstreaming.model.BusyImageFormat

interface FScreenStreamingFeatureApi : FDeviceFeatureApi {
    suspend fun getBusyImageFormat(): Result<BusyImageFormat>
}
