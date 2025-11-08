package com.flipperdevices.bridge.connection.feature.screenstreaming.impl

import com.flipperdevices.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import com.flipperdevices.bridge.connection.feature.screenstreaming.api.FScreenStreamingFeatureApi
import com.flipperdevices.bridge.connection.feature.screenstreaming.model.BusyImageFormat
import com.flipperdevices.core.busylib.log.LogTagProvider
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import me.tatarka.inject.annotations.Inject
import kotlin.io.encoding.Base64

@Inject
class FScreenStreamingFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcFeatureApi
) : FScreenStreamingFeatureApi, LogTagProvider {
    override val TAG: String = "FScreenStreamingFeatureApi"

    override suspend fun getBusyImageFormat(): Result<BusyImageFormat> {
        return rpcFeatureApi.getScreen(0)
            .map { base64 -> base64.replace("\\s".toRegex(), "") }
            .mapCatching(Base64::decode)
            .map(::BusyImageFormat)
    }

    @AssistedFactory
    interface InternalFactory {
        operator fun invoke(
            rpcFeatureApi: FRpcFeatureApi
        ): FScreenStreamingFeatureApiImpl
    }
}
