package net.flipper.bsb.cloud.barsws.api.orchestrator

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import net.flipper.bsb.cloud.barsws.api.model.InternalWebSocketRequest
import net.flipper.bsb.cloud.barsws.api.utils.BSBWebSocketInternal
import net.flipper.core.busylib.ktx.common.withLock
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.verbose
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

private val SEND_TIMEOUT = 5.seconds

class ActiveWebSocketHolder(private val logger: LogTagProvider) {
    private val mutex = Mutex()
    private val activeSubscriptionsSet = mutableSetOf<Uuid>()
    private var currentWebSocket: BSBWebSocketInternal? = null

    suspend fun invalidateSubscribers(
        subscriberCounts: Map<Uuid, Int>,
        webSocketApi: BSBWebSocketInternal
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
            safeSend(
                webSocketApi,
                InternalWebSocketRequest.SubscribeState(toSubscribe.toList())
            ).onSuccess {
                activeSubscriptionsSet.addAll(toSubscribe)
            }
        }
        if (toUnsubscribe.isNotEmpty()) {
            safeSend(
                webSocketApi,
                InternalWebSocketRequest.UnsubscribeState(toUnsubscribe.toList())
            ).onSuccess {
                activeSubscriptionsSet.removeAll(toUnsubscribe)
            }
        }
    }

    suspend fun resetSubscribers() = logger.withLock(mutex, "clear") {
        if (activeSubscriptionsSet.isNotEmpty()) {
            currentWebSocket?.let {
                safeSend(
                    it,
                    InternalWebSocketRequest.UnsubscribeState(
                        activeSubscriptionsSet.toList()
                    )
                )
            }
        }
        activeSubscriptionsSet.clear()
        currentWebSocket = null
        logger.info { "Websocket is null, so activeSubscriptionsSet should be null" }
    }

    private suspend fun safeSend(
        webSocketApi: BSBWebSocketInternal,
        request: InternalWebSocketRequest
    ): Result<Unit> {
        logger.verbose { "Send $request" }
        @Suppress("RunCatchingInSuspendRule")
        return runCatching { // By design ignore cancellation exception from webSocketApi.send
            withTimeout(SEND_TIMEOUT) {
                webSocketApi.send(
                    request
                )
            }
        }.onFailure {
            logger.error(it) { "Failed to send request $request" }
        }
    }
}
