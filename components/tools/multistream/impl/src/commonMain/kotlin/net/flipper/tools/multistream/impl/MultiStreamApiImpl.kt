package net.flipper.tools.multistream.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.feature.events.impl.FEventsFeatureApiImpl
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.getFiltered
import net.flipper.bridge.connection.feature.screenstreaming.api.FScreenStreamingFeatureApi
import net.flipper.bridge.connection.feature.screenstreaming.impl.delegates.ScreenFramesProvider
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.deviceOrNull
import net.flipper.bridge.connection.transport.tcp.lan.impl.metainfo.FCloudStreamingApi
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketOrchestratorApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.tools.multistream.api.MultiStreamApi
import net.flipper.tools.multistream.api.MultiStreamState

@Inject
@ContributesBinding(BusyLibGraph::class, binding = binding<MultiStreamApi>())
class MultiStreamApiImpl(
    private val orchestrator: FDeviceOrchestrator,
    private val featureProvider: FFeatureProvider,
    private val wsOrchestrator: CloudWebSocketOrchestratorApi
) : MultiStreamApi, LogTagProvider {
    override val TAG = "MultiStreamApi"

    override fun get(
        busyBar: BUSYBar
    ): WrappedFlow<MultiStreamState> {
        return orchestrator.getState().flatMapLatest { state ->
            if (state.deviceOrNull?.uniqueId == busyBar.uniqueId) {
                if (state is FDeviceConnectStatus.Connected) {
                    return@flatMapLatest featureProvider
                        .getFiltered<FScreenStreamingFeatureApi>(state)
                        .filterIsInstance<FFeatureStatus.Supported<*>>()
                        .filter { fFeatureStatus -> fFeatureStatus.featureApi is FScreenStreamingFeatureApi }
                        .filterIsInstance<FFeatureStatus.Supported<FScreenStreamingFeatureApi>>()
                        .flatMapLatest { status ->
                            status.featureApi.busyImageFormatFlow.map {
                                MultiStreamState.Frame(it)
                            }
                        }
                } else {
                    return@flatMapLatest emptyFlow()
                }
            } else {
                createCloudStreamFlow(busyBar)
            }
        }.wrap()
    }

    private fun createCloudStreamFlow(busyBar: BUSYBar): Flow<MultiStreamState> {
        val cloudId = busyBar.cloud ?: return flowOf(MultiStreamState.Empty)

        return channelFlow {
            val scope: CoroutineScope = this
            val eventsApi = FEventsFeatureApiImpl(
                streamingApi = FCloudStreamingApi(
                    deviceId = cloudId.deviceId,
                    orchestrator = wsOrchestrator
                ),
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
