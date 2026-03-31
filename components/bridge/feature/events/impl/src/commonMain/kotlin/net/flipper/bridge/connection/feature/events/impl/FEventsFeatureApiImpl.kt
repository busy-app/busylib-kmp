package net.flipper.bridge.connection.feature.events.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.impl.bits.BitIndicationEventsFlow
import net.flipper.bridge.connection.feature.events.impl.ws.WSEventsFlow
import net.flipper.bridge.connection.feature.events.internal.api.FEventsInternalApi
import net.flipper.bridge.connection.feature.events.model.BsbUpdateEvent
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.ConsumableUpdateEvent
import net.flipper.bridge.connection.feature.events.proto.api.FEventsProtoApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.merge
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.verbose
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.time.Duration.Companion.seconds

class FEventsFeatureApiImpl(
    metaInfoApi: FTransportMetaInfoApi,
    streamingApi: FStatusStreamingApi?,
    private val scope: CoroutineScope,
) : FEventsFeatureApi, LogTagProvider {
    override val TAG = "FEventsFeatureApi"
    private val bitIndicationEventsFlow by lazy { BitIndicationEventsFlow() }
    private val wsEventsFlow by lazy { WSEventsFlow() }

    private val fEventsInternalApi = FEventsInternalApi()
    private val fEventsProtoApi = FEventsProtoApi(
        scope = scope,
        streamingApi = streamingApi
    )

    private val sharedIndicationFlow = merge(
        metaInfoApi.get(TransportMetaInfoKey.EVENTS_INDICATION)
            .mapNotNull { bitsMaskFlowResult -> bitsMaskFlowResult.getOrNull() }
            .flatMapLatest { flow -> bitIndicationEventsFlow.getEventFlow(flow) },
        metaInfoApi.get(TransportMetaInfoKey.WS_EVENT)
            .mapNotNull { bitsMaskFlowResult -> bitsMaskFlowResult.getOrNull() }
            .flatMapLatest { flow -> wsEventsFlow.getEventFlow(flow) },
        fEventsInternalApi.getBsbEventsFlow(),
    )
        .onEach { verbose { "Receive update event: $it" } }
        .shareIn(scope, SharingStarted.WhileSubscribed(5.seconds))

    override fun getBsbUpdateEvents(): Flow<ConsumableUpdateEvent.Bsb> = sharedIndicationFlow

    override fun onBsbEvent(event: BsbUpdateEvent) {
        scope.launch { fEventsInternalApi.onBsbEvent(event) }
    }

    override fun onBusyLibEvent(event: BusyLibUpdateEvent) {
        scope.launch { fEventsInternalApi.onBusyLibEvent(event) }
    }

    override fun getBusyLibUpdateEvents(): Flow<ConsumableUpdateEvent.BusyLib<*>> {
        return merge(
            fEventsInternalApi.getBusyLibEventsFlow(),
            fEventsProtoApi.getBusyLibEventsFlow()
        )
    }

    @Inject
    class Factory : FDeviceFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FDeviceFeatureApi? {
            return FEventsFeatureApiImpl(
                metaInfoApi = connectedDevice as? FTransportMetaInfoApi
                    ?: return null,
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
