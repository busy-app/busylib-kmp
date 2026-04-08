package net.flipper.bridge.connection.feature.events.impl

import BSB_State.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.ConsumableUpdateEvent
import net.flipper.bridge.connection.feature.events.proto.protomapper.BSBProtobufEventMapper
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.bridge.connection.transport.common.api.serial.StatusStreamingEvent
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.verbose
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

class FEventsFeatureApiImpl(
    streamingApi: FStatusStreamingApi?,
    private val scope: CoroutineScope,
    override val TAG: String = "FEventsFeatureApi"
) : FEventsFeatureApi, LogTagProvider {
    private val busyLibUpdateEventFlow = MutableSharedFlow<BusyLibUpdateEvent>()

    override fun onBusyLibEvent(event: BusyLibUpdateEvent) {
        scope.launch { busyLibUpdateEventFlow.emit(event) }
    }

    override fun getBusyLibUpdateEvents(): Flow<ConsumableUpdateEvent.BusyLib<*>> {
        return busyLibUpdateEventFlow
            .map { busyLibUpdateEvent -> ConsumableUpdateEvent.BusyLib(busyLibUpdateEvent) }
    }

    private fun collectProtobufChanges(streamingApi: FStatusStreamingApi) {
        streamingApi
            .getEvents()
            .onEach { event ->
                when (event) {
                    is StatusStreamingEvent.Protobuf -> {
                        onProtobufStatesUpdate(data = event)
                            .onFailure { t -> error(t) { "Failed to process $event" } }
                    }
                }
            }
            .launchIn(scope)
    }

    private suspend fun onProtobufStatesUpdate(
        data: StatusStreamingEvent.Protobuf
    ) = runSuspendCatching {
        val state = State.ADAPTER.decode(data.data)
        verbose { "Process ${state.updates.size} updates: $state" }
        val updates = state.updates.mapNotNull { update ->
            BSBProtobufEventMapper.map(update)
        }
        updates.forEach { update ->
            busyLibUpdateEventFlow.emit(update)
        }
    }

    init {
        if (streamingApi != null) {
            collectProtobufChanges(streamingApi)
        }
    }

    @Inject
    class Factory : FDeviceFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FDeviceFeatureApi {
            return FEventsFeatureApiImpl(
                scope = scope,
                streamingApi = connectedDevice as? FStatusStreamingApi
            )
        }
    }

    @ContributesTo(BusyLibGraph::class)
    interface Component {
        @Provides
        @IntoMap
        fun provideFeatureFactory(
            factory: Factory
        ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
            return FDeviceFeature.EVENTS to factory
        }
    }
}
