package net.flipper.bridge.connection.feature.screenstreaming.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.get
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcStreamingApi
import net.flipper.bridge.connection.feature.screenstreaming.api.FScreenStreamingFeatureApi
import net.flipper.bridge.connection.feature.screenstreaming.impl.delegates.ScreenFramesProvider
import net.flipper.bridge.connection.feature.screenstreaming.model.BusyImageFormat
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import kotlin.io.encoding.Base64

private const val DEFAULT_BB_WIDTH = 72
private const val DEFAULT_BB_HEIGHT = 16


class FScreenStreamingFeatureApiImpl(
    private val scope: CoroutineScope,
    fEventsFeatureApi: FEventsFeatureApi,
    rpcApi: FRpcFeatureApi
) : FScreenStreamingFeatureApi, LogTagProvider {
    override val TAG: String = "FScreenStreamingFeatureApi"

    private val screenFramesProvider = ScreenFramesProvider(
        getFramesFlow(
            eventsApi = fEventsFeatureApi,
            streamingApi = rpcApi.fRpcStreamingApi
        )
    )

    override val busyImageFormatFlow: WrappedFlow<BusyImageFormat> = screenFramesProvider
        .getScreens()
        .wrap()

    private fun getFramesFlow(
        eventsApi: FEventsFeatureApi, streamingApi: FRpcStreamingApi
    ): Flow<BusyLibUpdateEvent.Frame> {
        return eventsApi.get(
            scope = scope,
            initial = { _ ->
                streamingApi.getScreen(display = 0).mapCatching { rawData ->
                    BusyLibUpdateEvent.Frame(
                        screen = BusyLibUpdateEvent.Frame.Screen.FRONT,
                        data = Base64.decode(rawData.replace("\\s".toRegex(), "")),
                        encoding = BusyLibUpdateEvent.Frame.Encoding.PLAIN,
                        pixelFormat = BusyLibUpdateEvent.Frame.PixelFormat.RGB888,
                        height = DEFAULT_BB_HEIGHT,
                        width = DEFAULT_BB_WIDTH
                    )
                }.onFailure {
                    error(it) { "Failed get screen frame" }
                }
            }
        )
    }
}
