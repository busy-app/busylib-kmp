package net.flipper.bridge.connection.feature.screenstreaming.impl.delegates

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.screenstreaming.model.BusyImageFormat
import net.flipper.core.busylib.ktx.common.TickFlow
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.throttleLatest
import net.flipper.core.busylib.log.error
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.seconds

private val TICK_DELAY = 3.seconds

class TickFlowScreenFramesProvider(
    private val scope: CoroutineScope,
    private val rpcFeatureApi: FRpcFeatureApi
) : ScreenFramesProvider {
    private val screensSharedFlow = TickFlow(TICK_DELAY)
        .throttleLatest { _ ->
            exponentialRetry {
                getBusyImageFormat()
                    .onFailure { throwable ->
                        error(throwable) { "Failed to get busy image format" }
                    }
            }
        }

    override fun getScreens(): Flow<BusyImageFormat> {
        return screensSharedFlow
    }

    private suspend fun getBusyImageFormat(): Result<BusyImageFormat> {
        return rpcFeatureApi.fRpcStreamingApi
            .getScreen(0)
            .map { base64 -> base64.replace("\\s".toRegex(), "") }
            .mapCatching(Base64::decode)
            .map(::BusyImageFormat)
    }
}
