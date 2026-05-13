package net.flipper.bsb.cloud.barsws.api.fakes

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.flipper.bsb.cloud.barsws.api.WebSocketEvent
import net.flipper.bsb.cloud.barsws.api.model.InternalWebSocketRequest
import net.flipper.bsb.cloud.barsws.api.model.WebSocketEventInternal
import net.flipper.bsb.cloud.barsws.api.utils.BSBWebSocketInternal

internal class MockBSBWebSocket(
    private val onClose: () -> Unit = {}
) : BSBWebSocketInternal {
    private val _sentRequests = mutableListOf<InternalWebSocketRequest>()
    private val requestsMutex = Mutex()
    val sentRequests: List<InternalWebSocketRequest> get() = _sentRequests.toList()

    private val _eventsFlow = MutableSharedFlow<WebSocketEventInternal>(
        extraBufferCapacity = 64
    )
    private val closedSignal = CompletableDeferred<Unit>()

    override fun getEventsFlow(): Flow<WebSocketEvent> = emptyFlow()

    override fun getEventsFlowInternal(): Flow<WebSocketEventInternal> = _eventsFlow

    override suspend fun send(request: InternalWebSocketRequest) {
        requestsMutex.withLock {
            _sentRequests.add(request)
        }
    }

    override suspend fun awaitClosed() {
        closedSignal.await()
    }

    suspend fun emitEvent(event: WebSocketEventInternal) {
        _eventsFlow.emit(event)
    }

    fun close() {
        closedSignal.complete(Unit)
        onClose()
    }
}
