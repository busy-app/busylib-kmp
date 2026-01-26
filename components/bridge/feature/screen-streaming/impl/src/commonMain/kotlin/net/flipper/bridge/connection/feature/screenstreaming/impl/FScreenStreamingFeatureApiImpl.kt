package net.flipper.bridge.connection.feature.screenstreaming.impl

import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.screenstreaming.api.FScreenStreamingFeatureApi
import net.flipper.bridge.connection.feature.screenstreaming.model.BusyImageFormat
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.core.busylib.log.LogTagProvider
import kotlin.io.encoding.Base64

@Inject
class FScreenStreamingFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcFeatureApi
) : FScreenStreamingFeatureApi, LogTagProvider {
    override val TAG: String = "FScreenStreamingFeatureApi"

    override suspend fun getBusyImageFormat(): CResult<BusyImageFormat> {
        return rpcFeatureApi.fRpcStreamingApi
            .getScreen(0)
            .map { base64 -> base64.replace("\\s".toRegex(), "") }
            .mapCatching(Base64::decode)
            .map(::BusyImageFormat)
            .toCResult()
    }

    @Inject
    class InternalFactory(
        private val factory: (FRpcFeatureApi) -> FScreenStreamingFeatureApiImpl
    ) {
        operator fun invoke(
            rpcFeatureApi: FRpcFeatureApi
        ): FScreenStreamingFeatureApiImpl = factory(rpcFeatureApi)
    }
}
