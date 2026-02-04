package net.flipper.bsb.cloud.barsws.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.URLProtocol
import io.ktor.http.path
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.barsws.api.model.toInternal
import net.flipper.bsb.cloud.barsws.api.model.toPublic
import net.flipper.bsb.cloud.barsws.api.utils.addAuthHeader
import net.flipper.bsb.cloud.barsws.api.utils.wrappers.BSBWebSocketSession
import net.flipper.bsb.cloud.barsws.api.utils.wrappers.KtorBSBWebSocketSession
import net.flipper.core.busylib.ktx.common.launchOnCompletion
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info

class BSBWebSocketImpl(
    private val session: BSBWebSocketSession,
    logger: LogTagProvider,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher
) : BSBWebSocket, LogTagProvider by logger {
    private val eventsFlow = channelFlow {
        while (currentCoroutineContext().isActive) {
            val message = try {
                session.receive()
            } catch (e: SerializationException) {
                error(e) { "Failed deserialize message from websocket" }
                null
            }

            info { "Received message: $message" }
            message?.let { send(it.toPublic()) }
        }
    }.flowOn(dispatcher)
        .shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)

    override fun getEventsFlow(): Flow<WebSocketEvent> {
        return eventsFlow
    }

    override suspend fun send(request: WebSocketRequest) {
        val requestInternal = request.toInternal()
        info { "Send $requestInternal" }
        session.send(requestInternal)
    }
}

suspend fun getBSBWebSocket(
    httpClient: HttpClient,
    logger: LogTagProvider,
    principal: BUSYLibUserPrincipal.Token,
    busyHost: String,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher
): BSBWebSocket {
    return withContext(dispatcher) {
        val webSocketSession = httpClient.webSocketSession {
            url {
                host = busyHost
                path("/api/v1/bars/ws")
                protocol = URLProtocol.WSS
            }
            addAuthHeader(principal)
        }

        val session = KtorBSBWebSocketSession(webSocketSession)

        scope.launchOnCompletion {
            session.close()
        }

        return@withContext BSBWebSocketImpl(session, logger, scope, dispatcher)
    }
}
