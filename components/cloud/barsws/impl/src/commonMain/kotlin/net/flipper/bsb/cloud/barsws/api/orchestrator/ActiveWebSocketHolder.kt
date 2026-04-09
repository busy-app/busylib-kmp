package net.flipper.bsb.cloud.barsws.api.orchestrator

import kotlinx.coroutines.sync.Mutex
import net.flipper.bsb.cloud.barsws.api.model.InternalWebSocketRequest
import net.flipper.bsb.cloud.barsws.api.utils.BSBWebSocket
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.ktx.common.withLock
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.uuid.Uuid

class ActiveWebSocketHolder(private val logger: LogTagProvider) {
    private val mutex = Mutex()
    private val activeSubscriptionsSet = mutableSetOf<Uuid>()
    private var currentWebSocket: BSBWebSocket? = null

    suspend fun invalidateSubscribers(
        subscriberCounts: Map<Uuid, Int>,
        webSocketApi: BSBWebSocket
    ) = logger.withLock(mutex, "invalidate") {
        if (currentWebSocket !== webSocketApi) {
            activeSubscriptionsSet.clear()
            currentWebSocket = webSocketApi
        }

        val shouldBeActive = subscriberCounts.filter { (_, count) -> count > 0 }
            .map { (uuid, _) -> uuid }
            .toSet()
        val toSubscribe = shouldBeActive.minus(activeSubscriptionsSet)
        val toUnsubscribe = activeSubscriptionsSet.minus(shouldBeActive)

        logger.info { "Should be active: $shouldBeActive, toSubscribe: $toSubscribe, toUnsubscribe: $toUnsubscribe" }

        if (toSubscribe.isNotEmpty()) {
            webSocketApi.send(InternalWebSocketRequest.SubscribeState(toSubscribe.toList()))
        }
        if (toUnsubscribe.isNotEmpty()) {
            webSocketApi.send(InternalWebSocketRequest.UnsubscribeState(toUnsubscribe.toList()))
        }

        activeSubscriptionsSet.addAll(toSubscribe)
        activeSubscriptionsSet.removeAll(toUnsubscribe)
    }

    suspend fun resetSubscribers() = logger.withLock(mutex, "clear") {
        runSuspendCatching {
            if (activeSubscriptionsSet.isNotEmpty()) {
                currentWebSocket?.send(
                    InternalWebSocketRequest.UnsubscribeState(
                        activeSubscriptionsSet.toList()
                    )
                )
            }
        }.onFailure {
            logger.error(it) { "Failed to remove subscription" }
        }
        activeSubscriptionsSet.clear()
        currentWebSocket = null
        logger.info { "Websocket is null, so activeSubscriptionsSet should be null" }
    }
}
