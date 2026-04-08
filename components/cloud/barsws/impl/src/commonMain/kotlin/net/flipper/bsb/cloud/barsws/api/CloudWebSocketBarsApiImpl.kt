package net.flipper.bsb.cloud.barsws.api

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import net.flipper.bsb.cloud.barsws.api.model.InternalWebSocketRequest
import net.flipper.bsb.cloud.barsws.api.utils.BSBWebSocket
import net.flipper.bsb.cloud.barsws.api.utils.CloudWebSocketApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, CloudWebSocketBarsApi::class)
class CloudWebSocketBarsApiImpl(
    private val webSocketApi: CloudWebSocketApi
) : CloudWebSocketBarsApi, LogTagProvider {
    override val TAG = "BUSYBarWebSocketOrchestrator"

    private val mutex = Mutex()
    private val subscriberCounts = mutableMapOf<Uuid, Int>()
    private val subscribedWs = mutableMapOf<Uuid, BSBWebSocket>()

    @Suppress("LongMethod")
    override fun getEventsFlow(cloudId: Uuid): Flow<Pair<String, Any>> {
        return flow {
            mutex.withLock {
                val newCount = subscriberCounts.getOrDefault(cloudId, 0) + 1
                subscriberCounts[cloudId] = newCount
                info { "Increase subscriber count for $cloudId, new count is: $newCount" }
            }
            try {
                emitAll(
                    webSocketApi.getWSFlow()
                        .filterNotNull()
                        .flatMapLatest { ws ->
                            ws.getEventsFlow()
                                .onStart {
                                    val shouldSubscribe = mutex.withLock {
                                        if (subscribedWs[cloudId] !== ws) {
                                            subscribedWs[cloudId] = ws
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                    if (shouldSubscribe) {
                                        info { "Subscribe to busy bar: $cloudId" }
                                        ws.send(
                                            InternalWebSocketRequest.SubscribeState(
                                                listOf(cloudId)
                                            )
                                        )
                                    }
                                }
                        }
                        .filter { it.barId == cloudId }
                        .transform { event ->
                            event.values.forEach { (k, v) -> emit(k to v) }
                        }
                )
            } finally {
                withContext(NonCancellable) {
                    val shouldUnsubscribe = mutex.withLock {
                        val count = (subscriberCounts[cloudId] ?: 0) - 1
                        if (count <= 0) {
                            subscriberCounts.remove(cloudId)
                            subscribedWs.remove(cloudId)
                            true
                        } else {
                            info { "Decrease subscriber count for $cloudId, new count is: $count" }
                            subscriberCounts[cloudId] = count
                            false
                        }
                    }
                    if (shouldUnsubscribe) {
                        info { "Unsubscribe from busy bar: $cloudId" }
                        runSuspendCatching {
                            (webSocketApi.getWSFlow().first())
                                ?.send(
                                    InternalWebSocketRequest.UnsubscribeState(
                                        listOf(cloudId)
                                    )
                                )
                        }
                    }
                }
            }
        }
    }
}
