package net.flipper.bridge.connection.feature.events.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.impl.bits.BitIndicationEventsFlow
import net.flipper.bridge.connection.feature.events.impl.ws.WSEventsFlow
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.ConsumableUpdateEvent
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.verbose
import kotlin.time.Duration.Companion.seconds

@Inject
class FEventsFeatureApiImpl(
    @Assisted private val metaInfoApi: FTransportMetaInfoApi,
    @Assisted private val scope: CoroutineScope
) : FEventsFeatureApi, LogTagProvider {
    override val TAG = "FEventsFeatureApi"
    private val bitIndicationEventsFlow by lazy { BitIndicationEventsFlow() }
    private val wsEventsFlow by lazy { WSEventsFlow() }
    private val busyLibEventsFlow = MutableSharedFlow<BusyLibUpdateEvent>()
    private val sharedIndicationFlow = combine(
        flow = metaInfoApi.get(TransportMetaInfoKey.EVENTS_INDICATION),
        flow2 = metaInfoApi.get(TransportMetaInfoKey.WS_EVENT),
    ) { bitsMaskFlowResult, wsEventsFlowResult ->
        val flows = mutableListOf<Flow<ConsumableUpdateEvent.Bsb>>(emptyFlow())
        bitsMaskFlowResult.getOrNull()?.let { flow ->
            flows += bitIndicationEventsFlow.getEventFlow(flow)
        }
        wsEventsFlowResult.getOrNull()?.let { flow ->
            flows += wsEventsFlow.getEventFlow(flow)
        }
        flows.merge()
    }.flatMapLatest { it }
        .onEach { verbose { "Receive update event: $it" } }
        .shareIn(scope, SharingStarted.WhileSubscribed(5.seconds))

    override fun getBsbUpdateEvents(): Flow<ConsumableUpdateEvent.Bsb> = sharedIndicationFlow

    override fun onBusyLibEvent(event: BusyLibUpdateEvent) {
        scope.launch { busyLibEventsFlow.emit(event) }
    }

    override fun getBusyLibUpdateEvents(): Flow<ConsumableUpdateEvent.BusyLib<*>> {
        return busyLibEventsFlow.map { busyLibUpdateEvent ->
            ConsumableUpdateEvent.BusyLib(busyLibUpdateEvent)
        }
    }

    @Inject
    class InternalFactory(
        private val factory: (FTransportMetaInfoApi, CoroutineScope) -> FEventsFeatureApiImpl
    ) {
        operator fun invoke(
            metaInfoApi: FTransportMetaInfoApi,
            scope: CoroutineScope
        ): FEventsFeatureApiImpl = factory(metaInfoApi, scope)
    }
}
