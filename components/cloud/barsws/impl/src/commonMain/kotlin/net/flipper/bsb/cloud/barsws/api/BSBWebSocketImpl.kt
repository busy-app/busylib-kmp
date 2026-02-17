package net.flipper.bsb.cloud.barsws.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.post
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.barsws.api.model.InternalTicketRequest
import net.flipper.bsb.cloud.barsws.api.model.InternalWebSocketRequest
import net.flipper.bsb.cloud.barsws.api.model.toInternal
import net.flipper.bsb.cloud.barsws.api.utils.addAuthHeader
import net.flipper.bsb.cloud.barsws.api.utils.wrappers.BSBWebSocketSession
import net.flipper.bsb.cloud.barsws.api.utils.wrappers.KtorBSBWebSocketSession
import net.flipper.core.busylib.ktx.common.launchOnCompletion
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.sensitive
import net.flipper.core.busylib.log.verbose
import net.flipper.core.busylib.log.warn
import kotlin.uuid.Uuid

private const val JSON_KEY_BAR_ID = "bar_id"

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

            verbose { "Received message: $message" }
            val webSocketEvent = message?.let { getWebSocketEvent(it) }
            webSocketEvent?.let { send(it) }
        }
    }.flowOn(dispatcher)
        .shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)

    private fun getWebSocketEvent(jsonObject: JsonObject): WebSocketEvent? {
        val barIdPrimitive = jsonObject[JSON_KEY_BAR_ID] as? JsonPrimitive
        if (barIdPrimitive == null) {
            warn { "Receive websocket event ($jsonObject), but bar id is null" }
            return null
        }
        val barId = Uuid.parseOrNull(barIdPrimitive.content)
        if (barId == null) {
            warn { "Failed parse bar id: ${barIdPrimitive.content}" }
            return null
        }
        return WebSocketEvent(
            barId = barId,
            values = jsonObject.filterKeys { it != JSON_KEY_BAR_ID }
        )
    }

    override fun getEventsFlow(): Flow<WebSocketEvent> {
        return eventsFlow
    }

    override suspend fun send(request: WebSocketRequest) {
        val requestInternal = request.toInternal()
        info { "Send $requestInternal" }
        session.send(requestInternal)
    }
}

@Suppress("LongParameterList")
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
                port = 443
            }
        }

        val session = KtorBSBWebSocketSession(webSocketSession)

        scope.launchOnCompletion {
            session.close()
        }
        val ticketToken = getTicketToken(httpClient, busyHost, principal)

        sensitive { "Received ticket token: $ticketToken" }

        session.send(InternalWebSocketRequest.Authorization(ticketToken))

        return@withContext BSBWebSocketImpl(session, logger, scope, dispatcher)
    }
}

private suspend fun getTicketToken(
    httpClient: HttpClient,
    busyHost: String,
    principal: BUSYLibUserPrincipal.Token
): String {
    return httpClient.post {
        url {
            host = busyHost
            path("/api/v0/auth/ticket")
            protocol = URLProtocol.HTTPS
            port = 443
        }
        addAuthHeader(principal)
    }.body<InternalTicketRequest>().token
}
