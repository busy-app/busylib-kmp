package net.flipper.bsb.cloud.barsws.api.orchestrator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketOrchestratorApi
import net.flipper.bsb.cloud.barsws.api.ProtobufBase64
import net.flipper.bsb.cloud.barsws.api.model.WebSocketEventInternal
import net.flipper.bsb.cloud.barsws.api.utils.CloudWebSocketApiInternal
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.verbose
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, CloudWebSocketOrchestratorApi::class)
class CloudWebSocketOrchestratorApiImpl(
    private val webSocketApi: CloudWebSocketApiInternal,
    scope: CoroutineScope
) : CloudWebSocketOrchestratorApi, LogTagProvider {
    override val TAG = "CloudWebSocketOrchestratorApi"

    private val activeWebSocketHolder = ActiveWebSocketHolder(this as LogTagProvider)

    private val subscriberCountsFlow = MutableStateFlow(mapOf<Uuid, Int>())
    private val subscriberCountsFlowWithDebounce = subscriberCountsFlow
        .debounce(1.seconds)
    private val wsEventSharedFlow = getWSEventsFlow()
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

    private fun getWSEventsFlow(): Flow<WebSocketEventInternal.Protobuf> {
        return combine(
            subscriberCountsFlowWithDebounce.onEach {
                verbose { "Events combine: subscriberCounts=$it" }
            },
            getWSFlow().onEach {
                verbose { "Events combine: ws=$it" }
            },
        ) { subscriberCounts, webSocket ->
            verbose { "Events combine triggered: subscriberCounts=$subscriberCounts, webSocket=$webSocket" }
            if (webSocket != null) {
                info { "Events combine: invalidating subscribers and collecting events" }
                activeWebSocketHolder.invalidateSubscribers(
                    subscriberCounts,
                    webSocket
                )
                webSocket.getEventsFlowInternal()
            } else {
                info { "Events combine: webSocket is null, resetting subscribers" }
                activeWebSocketHolder.resetSubscribers()
                flowOf()
            }
        }.flatMapLatest { it }
            .filterIsInstance<WebSocketEventInternal.Protobuf>()
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
