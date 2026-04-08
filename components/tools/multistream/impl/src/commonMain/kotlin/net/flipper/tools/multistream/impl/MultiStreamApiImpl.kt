package net.flipper.tools.multistream.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.feature.events.impl.FEventsFeatureApiImpl
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.ConsumableUpdateEvent
import net.flipper.bridge.connection.feature.screenstreaming.api.FScreenStreamingFeatureApi
import net.flipper.bridge.connection.feature.screenstreaming.impl.delegates.ScreenFramesProvider
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.deviceOrNull
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketBarsApi
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.tools.multistream.api.MultiStreamApi
import net.flipper.tools.multistream.api.MultiStreamState

class MultiStreamApiImpl(
    private val orchestrator: FDeviceOrchestrator,
    private val screenStreamingApi: FScreenStreamingFeatureApi,
    private val webSocketBarsApi: CloudWebSocketBarsApi
) : MultiStreamApi, LogTagProvider {
    override val TAG = "MultiStreamApi"

    override fun get(
        busyBar: BUSYBar
    ): Flow<MultiStreamState> {
        return orchestrator.getState().flatMapLatest { state ->
            if (state.deviceOrNull?.uniqueId == busyBar.uniqueId) {
                return@flatMapLatest screenStreamingApi.busyImageFormatFlow.map {
                    MultiStreamState.Frame(it)
                }
            } else createCloudStreamFlow(busyBar)
        }
    }

    private fun createCloudStreamFlow(busyBar: BUSYBar): Flow<MultiStreamState> {
        val cloudId = busyBar.cloud ?: return flowOf(MultiStreamState.Empty)

        webSocketBarsApi.getWSFlow()
        return channelFlow {
            val scope: CoroutineScope = this
            val eventsApi = FEventsFeatureApiImpl(
                streamingApi = null,
                scope = scope,
                TAG = "$TAG-FEventsFeatureApi"
            )

            val screenFlow: Flow<BusyLibUpdateEvent.Frame> = eventsApi
                .getBusyLibUpdateEvents()
                .mapNotNull {
                    it.busyLibUpdateEvent
                }.filterIsInstance<BusyLibUpdateEvent.Frame>()

            ScreenFramesProvider(screenFlow).getScreens().collect {
                send(MultiStreamState.Frame(it))
            }
        }
    }
}