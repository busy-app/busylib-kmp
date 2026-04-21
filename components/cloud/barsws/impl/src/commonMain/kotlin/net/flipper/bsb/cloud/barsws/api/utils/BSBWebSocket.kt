package net.flipper.bsb.cloud.barsws.api.utils

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.URLProtocol
import io.ktor.http.path
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.barsws.api.BSBWebSocket
import net.flipper.bsb.cloud.barsws.api.WebSocketEvent
import net.flipper.bsb.cloud.barsws.api.model.InternalWebSocketRequest
import net.flipper.bsb.cloud.barsws.api.model.WebSocketEventInternal
import net.flipper.bsb.cloud.barsws.api.model.toPublic
import net.flipper.bsb.cloud.barsws.api.utils.wrappers.BSBWebSocketSession
import net.flipper.bsb.cloud.barsws.api.utils.wrappers.KtorBSBWebSocketSession
import net.flipper.bsb.cloud.rest.api.BusyCloudWebSocketTicketApi
import net.flipper.core.busylib.ktx.common.launchOnCompletion
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.sensitive
import net.flipper.core.busylib.log.verbose

interface BSBWebSocketInternal : BSBWebSocket {
    fun getEventsFlowInternal(): Flow<WebSocketEventInternal>

    suspend fun send(request: InternalWebSocketRequest)
}

class BSBWebSocketImpl(
    private val session: BSBWebSocketSession,
    logger: LogTagProvider,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher
) : BSBWebSocketInternal, LogTagProvider by logger {
    private val eventsFlow = channelFlow {
        while (currentCoroutineContext().isActive) {
            val message = try {
                session.receive()
            } catch (e: ClosedReceiveChannelException) {
                error(e) { "Channel closed for receive" }
                throw e
            } catch (e: SerializationException) {
                error(e) { "Failed deserialize message from websocket" }
                null
            }

            verbose { "Received message: $message" }
            message?.let { send(it) }
        }
    }.flowOn(dispatcher).shareIn(scope, SharingStarted.WhileSubscribed(), 1)

    override fun getEventsFlow(): Flow<WebSocketEvent> {
        return eventsFlow.mapNotNull { it.toPublic() }
    }

    override fun getEventsFlowInternal(): Flow<WebSocketEventInternal> {
        return eventsFlow
    }

    override suspend fun send(request: InternalWebSocketRequest) {
        info { "Send $request" }
        session.send(request)
    }
}

@Suppress("LongParameterList", "MagicNumber")
suspend fun getBSBWebSocket(
    httpClient: HttpClient,
    ticketApi: BusyCloudWebSocketTicketApi,
    logger: LogTagProvider,
    principal: BUSYLibUserPrincipal.Token,
    busyHost: String,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher
): BSBWebSocketInternal {
    return withContext(dispatcher) {
        val webSocketSession = httpClient.webSocketSession {
            url {
                host = busyHost
                path("/api/v1/bars/ws")
                protocol = URLProtocol.WSS
                port = 443
            }
        }

        val session = KtorBSBWebSocketSession(webSocketSession)

        scope.launchOnCompletion {
            session.close()
        }
        val ticketToken = ticketApi.getTicketToken(principal).getOrThrow()

        sensitive { "Received ticket token: $ticketToken" }

        session.send(InternalWebSocketRequest.Authorization(ticketToken))

        return@withContext BSBWebSocketImpl(session, logger, scope, dispatcher)
    }
}
