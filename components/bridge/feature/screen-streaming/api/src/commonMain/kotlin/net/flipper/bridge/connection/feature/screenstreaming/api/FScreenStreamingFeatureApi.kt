package net.flipper.bridge.connection.feature.screenstreaming.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.screenstreaming.model.BusyImageFormat
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow

interface FScreenStreamingFeatureApi : FDeviceFeatureApi {
    suspend fun getBusyImageFormat(): CResult<BusyImageFormat>

    val busyImageFormatFlow: WrappedFlow<BusyImageFormat>
}
