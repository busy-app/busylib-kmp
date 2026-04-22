package net.flipper.bsb.cloud.barsws.api.fakes

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.SerializationException
import net.flipper.bsb.cloud.barsws.api.model.InternalWebSocketRequest
import net.flipper.bsb.cloud.barsws.api.model.WebSocketEventInternal
import net.flipper.bsb.cloud.barsws.api.utils.wrappers.BSBWebSocketSession

internal class MockBSBWebSocketSession(
    private val failOnSend: Boolean = false
) : BSBWebSocketSession {
    private val _eventChannel = Channel<WebSocketEventInternal>(Channel.UNLIMITED)
    private var _shouldThrowSerializationError = false
    private var _isClosed = false

    val sentRequests = mutableListOf<InternalWebSocketRequest>()

    suspend fun emitEvent(event: WebSocketEventInternal) {
        if (!_isClosed) {
            _eventChannel.send(event)
        }
    }

    fun emitSerializationError() {
        _shouldThrowSerializationError = true
    }

    fun simulateClose() {
        _isClosed = true
        _eventChannel.close()
    }

    override suspend fun receive(): WebSocketEventInternal {
        if (_shouldThrowSerializationError) {
            _shouldThrowSerializationError = false
            throw SerializationException("Mock deserialization error")
        }
        if (_isClosed) {
            throw CancellationException("Session closed")
        }
        return _eventChannel.receive()
    }

    override suspend fun send(request: InternalWebSocketRequest) {
        if (failOnSend) {
            error("Mock send failure")
        }
        sentRequests.add(request)
    }

    override suspend fun close() {
        _isClosed = true
        _eventChannel.close()
    }
}
