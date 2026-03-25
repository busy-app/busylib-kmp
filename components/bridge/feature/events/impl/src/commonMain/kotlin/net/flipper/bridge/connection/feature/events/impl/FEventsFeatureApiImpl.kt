package net.flipper.bridge.connection.feature.events.impl

import BSB_State.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.impl.bits.BitIndicationEventsFlow
import net.flipper.bridge.connection.feature.events.impl.protomapper.BSBProtobufEventMapper
import net.flipper.bridge.connection.feature.events.impl.ws.WSEventsFlow
import net.flipper.bridge.connection.feature.events.model.BsbUpdateEvent
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.ConsumableUpdateEvent
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.bridge.connection.transport.common.api.serial.StatusStreamingEvent
import net.flipper.core.busylib.ktx.common.launchIn
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.verbose
import kotlin.time.Duration.Companion.seconds

class FEventsFeatureApiImpl(
    metaInfoApi: FTransportMetaInfoApi,
    private val scope: CoroutineScope,
    streamingApi: FStatusStreamingApi?
) : FEventsFeatureApi, LogTagProvider {
    override val TAG = "FEventsFeatureApi"
    private val bitIndicationEventsFlow by lazy { BitIndicationEventsFlow() }
    private val wsEventsFlow by lazy { WSEventsFlow() }
    private val busyLibEventsFlow = MutableSharedFlow<BusyLibUpdateEvent>()

    init {
        if (streamingApi != null) {
            collectProtobufChanges(streamingApi)
        }
    }

    private val internalBsbEventsFlow = MutableSharedFlow<BsbUpdateEvent>()

    private val sharedIndicationFlow = merge(
        metaInfoApi.get(TransportMetaInfoKey.EVENTS_INDICATION)
            .mapNotNull { bitsMaskFlowResult -> bitsMaskFlowResult.getOrNull() }
            .flatMapLatest { flow -> bitIndicationEventsFlow.getEventFlow(flow) },
        metaInfoApi.get(TransportMetaInfoKey.WS_EVENT)
            .mapNotNull { bitsMaskFlowResult -> bitsMaskFlowResult.getOrNull() }
            .flatMapLatest { flow -> wsEventsFlow.getEventFlow(flow) },
        internalBsbEventsFlow
            .map { event -> ConsumableUpdateEvent.Bsb(event, null) },
    )
        .onEach { verbose { "Receive update event: $it" } }
        .shareIn(scope, SharingStarted.WhileSubscribed(5.seconds))

    override fun getBsbUpdateEvents(): Flow<ConsumableUpdateEvent.Bsb> = sharedIndicationFlow

    override fun onBsbEvent(event: BsbUpdateEvent) {
        scope.launch { internalBsbEventsFlow.emit(event) }
    }

    override fun onBusyLibEvent(event: BusyLibUpdateEvent) {
        scope.launch {
            val subscriptionCount = busyLibEventsFlow.subscriptionCount.first()
            if (subscriptionCount <= 0) return@launch
            busyLibEventsFlow.emit(event)
        }
    }

    override fun getBusyLibUpdateEvents(): Flow<ConsumableUpdateEvent.BusyLib<*>> {
        return busyLibEventsFlow.map { busyLibUpdateEvent ->
            ConsumableUpdateEvent.BusyLib(busyLibUpdateEvent)
        }
    }

    private fun collectProtobufChanges(streamingApi: FStatusStreamingApi) {
        streamingApi
            .getEvents()
            .onEach { event ->
                when (event) {
                    is StatusStreamingEvent.Protobuf -> onProtobufStatesUpdate(data = event)
                }.onFailure { error(it) { "Failed to process $event" } }
            }.launchIn(scope)
    }

    private suspend fun onProtobufStatesUpdate(
        data: StatusStreamingEvent.Protobuf
    ) = runSuspendCatching {
        val state = State.ADAPTER.decode(data.data)
        verbose { "Process ${state.updates.size} updates" }
        val updates = state.updates.mapNotNull { update ->
            BSBProtobufEventMapper.map(update)
        }
        updates.forEach { update ->
            busyLibEventsFlow.emit(update)
        }
    }
}
