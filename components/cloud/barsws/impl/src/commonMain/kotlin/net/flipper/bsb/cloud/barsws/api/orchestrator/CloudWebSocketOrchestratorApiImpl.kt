package net.flipper.bsb.cloud.barsws.api.orchestrator

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.withContext
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketOrchestratorApi
import net.flipper.bsb.cloud.barsws.api.ProtobufBase64
import net.flipper.bsb.cloud.barsws.api.model.WebSocketEventInternal
import net.flipper.bsb.cloud.barsws.api.utils.CloudWebSocketApiInternal
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.verbose
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, binding<CloudWebSocketOrchestratorApi>())
class CloudWebSocketOrchestratorApiImpl(
    private val webSocketApi: CloudWebSocketApiInternal,
    scope: CoroutineScope
) : CloudWebSocketOrchestratorApi, LogTagProvider {
    override val TAG = "CloudWebSocketOrchestratorApi"

    private val activeWebSocketHolder = ActiveWebSocketHolder(this as LogTagProvider)

    private val subscriberCountsFlow = MutableStateFlow(mapOf<Uuid, Int>())

    private val subscriberCountsFlowWithDebounce = subscriberCountsFlow
        .debounce(1.seconds)
        .stateIn(scope, SharingStarted.Eagerly, subscriberCountsFlow.value)

    private val wsEventSharedFlow = getWSFlow()
        .distinctUntilChanged()
        .flatMapLatest { webSocket ->
            if (webSocket != null) {
                info { "New websocket, collecting events and syncing subscribers" }
                merge(
                    webSocket.getEventsFlowInternal(),
                    subscriberCountsFlowWithDebounce
                        .transform { subscriberCounts ->
                            verbose { "Syncing subscribers: $subscriberCounts" }
                            activeWebSocketHolder.invalidateSubscribers(
                                subscriberCounts,
                                webSocket
                            )
                        }
                )
            } else {
                info { "Websocket is null, resetting subscribers" }
                flow {
                    activeWebSocketHolder.resetSubscribers()
                }
            }
        }
        .filterIsInstance<WebSocketEventInternal.Protobuf>()
        .shareIn(scope, SharingStarted.Lazily)

    private fun getWSFlow() = subscriberCountsFlowWithDebounce
        .map {
            it.isNotEmpty()
        }
        .distinctUntilChanged()
        .onEach {
            verbose { "Need ws (after distinctUntilChanged): $it" }
        }
        .flatMapLatest { needWs ->
            if (needWs) {
                verbose { "Choosing wsFlow" }
                webSocketApi.getWSInternalFlow()
            } else {
                verbose { "Choosing null (no subscribers)" }
                flowOf(null)
            }
        }

    override fun getEventsFlow(cloudId: Uuid): Flow<ProtobufBase64> {
        return flow {
            val result = subscriberCountsFlow.updateAndGet { map ->
                map.plus(cloudId to map.getOrElse(cloudId, { 0 }) + 1)
            }
            info { "Subscribe $cloudId, new map is $result" }
            try {
                wsEventSharedFlow
                    .filter { it.cloudId == cloudId }
                    .collect {
                        emit(ProtobufBase64(it.state))
                    }
            } finally {
                withContext(NonCancellable) {
                    val result = subscriberCountsFlow.updateAndGet { map ->
                        val currentValue = map[cloudId]
                        if (currentValue == null || currentValue <= 1) {
                            map.minus(cloudId)
                        } else {
                            map.plus(cloudId to currentValue - 1)
                        }
                    }
                    info { "Unsubscribe $cloudId, new map is $result" }
                }
            }
        }
    }
}
