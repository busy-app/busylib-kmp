package net.flipper.bsb.cloud.barsws.api.utils.wrappers

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.websocket.close
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import net.flipper.bsb.cloud.barsws.api.model.InternalWebSocketRequest
import net.flipper.bsb.cloud.barsws.api.model.WebSocketEventInternal

/**
 * Abstraction for WebSocket session operations.
 * This interface allows BSBWebSocketImpl to be tested without depending on Ktor's final classes.
 */
interface BSBWebSocketSession {
    /**
     * Receives and deserializes the next message from the WebSocket.
     * @return The deserialized JsonObject
     * @throws kotlinx.serialization.SerializationException if deserialization fails
     * @throws kotlinx.coroutines.channels.ClosedReceiveChannelException if a channel was closed
     */
    suspend fun receive(): WebSocketEventInternal

    /**
     * Serializes and sends a request through the WebSocket.
     * @param request The request to send
     */
    suspend fun send(request: InternalWebSocketRequest)

    /**
     * Suspends until the underlying WebSocket session terminates (either normally
     * or due to a transport failure). Returns immediately if it is already closed.
     */
    suspend fun awaitClosed()

    /**
     * Closes the WebSocket session.
     */
    suspend fun close()
}

/**
 * Ktor implementation of BSBWebSocketSession that wraps DefaultClientWebSocketSession.
 */
class KtorBSBWebSocketSession(
    private val session: DefaultClientWebSocketSession
) : BSBWebSocketSession {
    override suspend fun receive(): WebSocketEventInternal {
        return session.receiveDeserialized()
    }

    override suspend fun send(request: InternalWebSocketRequest) {
        if (session.isActive.not()) {
            throw IllegalStateException("WebSocket is closed")
        }
        session.sendSerialized(request)
    }

    override suspend fun awaitClosed() {
        session.coroutineContext.job.join()
    }

    override suspend fun close() {
        session.close()
    }
}
