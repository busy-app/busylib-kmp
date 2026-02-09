package net.flipper.bsb.cloud.barsws.api.utils.wrappers

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.websocket.close
import net.flipper.bsb.cloud.barsws.api.model.InternalWebSocketEvent
import net.flipper.bsb.cloud.barsws.api.model.InternalWebSocketRequest

/**
 * Abstraction for WebSocket session operations.
 * This interface allows BSBWebSocketImpl to be tested without depending on Ktor's final classes.
 */
interface BSBWebSocketSession {
    /**
     * Receives and deserializes the next message from the WebSocket.
     * @return The deserialized InternalWebSocketEvent
     * @throws kotlinx.serialization.SerializationException if deserialization fails
     */
    suspend fun receive(): InternalWebSocketEvent

    /**
     * Serializes and sends a request through the WebSocket.
     * @param request The request to send
     */
    suspend fun send(request: InternalWebSocketRequest)

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

    override suspend fun receive(): InternalWebSocketEvent {
        return session.receiveDeserialized()
    }

    override suspend fun send(request: InternalWebSocketRequest) {
        session.sendSerialized(request)
    }

    override suspend fun close() {
        session.close()
    }
}
