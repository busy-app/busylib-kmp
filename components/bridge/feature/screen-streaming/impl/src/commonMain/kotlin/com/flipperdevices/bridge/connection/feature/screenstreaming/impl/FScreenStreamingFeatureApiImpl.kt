package com.flipperdevices.bridge.connection.feature.screenstreaming.impl

import com.flipperdevices.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import com.flipperdevices.bridge.connection.feature.screenstreaming.api.FScreenStreamingFeatureApi
import com.flipperdevices.bridge.connection.feature.screenstreaming.model.BusyImageFormat
import com.flipperdevices.core.busylib.log.LogTagProvider
import kotlin.io.encoding.Base64

class FScreenStreamingFeatureApiImpl(
    private val rpcFeatureApi: FRpcFeatureApi
) : FScreenStreamingFeatureApi, LogTagProvider {
    override val TAG: String = "FScreenStreamingFeatureApi"

    override suspend fun getBusyImageFormat(): Result<BusyImageFormat> {
        return rpcFeatureApi.getScreen(0)
            .map { base64 -> base64.replace("\\s".toRegex(), "") }
            .mapCatching(Base64::decode)
            .map(::BusyImageFormat)
    }

    interface InternalFactory {
        operator fun invoke(
            rpcFeatureApi: FRpcFeatureApi
        ): FScreenStreamingFeatureApiImpl
    }
}
