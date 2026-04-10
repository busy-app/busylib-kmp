package net.flipper.bsb.cloud.barsws.api.orchestrator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketOrchestratorApi
import net.flipper.bsb.cloud.barsws.api.model.WebSocketEvent
import net.flipper.bsb.cloud.barsws.api.utils.CloudWebSocketApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.log.LogTagProvider
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, CloudWebSocketOrchestratorApi::class)
class CloudWebSocketOrchestratorApiImpl(
    private val webSocketApi: CloudWebSocketApi,
    scope: CoroutineScope
) : CloudWebSocketOrchestratorApi, LogTagProvider {
    override val TAG = "CloudWebSocketOrchestratorApi"

    private val activeWebSocketHolder = ActiveWebSocketHolder(this as LogTagProvider)

    private val subscriberCountsFlow = MutableStateFlow(mapOf<Uuid, Int>())
    private val wsEventSharedFlow = getWSEventsFlow()
        .shareIn(scope, SharingStarted.Companion.Lazily)

    private fun getWSFlow() = subscriberCountsFlow.map {
        it.isNotEmpty()
    }.distinctUntilChanged()
        .flatMapLatest { needWs ->
            if (needWs) {
                webSocketApi.getWSFlow()
            } else {
                flowOf(null)
            }
        }

    private fun getWSEventsFlow(): Flow<WebSocketEvent> {
        return combine(
            subscriberCountsFlow,
            getWSFlow(),
        ) { subscriberCounts, webSocket ->
            if (webSocket != null) {
                activeWebSocketHolder.invalidateSubscribers(
                    subscriberCounts,
                    webSocket
                )
                webSocket.getEventsFlow()
            } else {
                activeWebSocketHolder.resetSubscribers()
                flowOf()
            }
        }.flatMapLatest { it }
    }

    override fun getEventsFlow(cloudId: Uuid): Flow<Pair<String, Any>> {
        return flow {
            subscriberCountsFlow.update { map ->
                map.plus(cloudId to map.getOrElse(cloudId, { 0 }) + 1)
            }
            try {
                wsEventSharedFlow
                    .filter { it.barId == cloudId }
                    .transform { event ->
                        event.values.forEach { (k, v) -> emit(k to v) }
                    }.collect { emit(it) }
            } finally {
                withContext(NonCancellable) {
                    subscriberCountsFlow.update { map ->
                        val currentValue = map[cloudId]
                        if (currentValue == null || currentValue <= 1) {
                            map.minus(cloudId)
                        } else {
                            map.plus(cloudId to currentValue - 1)
                        }
                    }
                }
            }
        }
    }
}
