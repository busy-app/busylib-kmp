package net.flipper.bridge.connection.feature.screenstreaming.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.screenstreaming.api.FScreenStreamingFeatureApi
import net.flipper.bridge.connection.feature.screenstreaming.model.BusyImageFormat
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.TickFlow
import net.flipper.core.busylib.ktx.common.asFlow
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.throttleLatest
import net.flipper.core.busylib.ktx.common.transformWhileSubscribed
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.seconds

@Inject
class FScreenStreamingFeatureApiImpl(
    @Assisted private val scope: CoroutineScope,
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

    override val busyImageFormatFlow: WrappedFlow<BusyImageFormat> = TickFlow(TICK_DELAY)
        .transformWhileSubscribed(
            timeout = TICK_DELAY.minus(1.seconds),
            scope = scope,
            transformFlow = { flow ->
                flow.throttleLatest { _ ->
                    exponentialRetry {
                        getBusyImageFormat()
                            .toKotlinResult()
                            .onFailure { throwable -> error(throwable) { "Failed to get busy image format" } }
                    }
                }
            }
        ).asFlow().wrap()

    @Inject
    class InternalFactory(
        private val factory: (CoroutineScope, FRpcFeatureApi) -> FScreenStreamingFeatureApiImpl
    ) {
        operator fun invoke(
            scope: CoroutineScope,
            rpcFeatureApi: FRpcFeatureApi
        ): FScreenStreamingFeatureApiImpl = factory(scope, rpcFeatureApi)
    }

    companion object {
        private val TICK_DELAY = 3.seconds
    }
}
