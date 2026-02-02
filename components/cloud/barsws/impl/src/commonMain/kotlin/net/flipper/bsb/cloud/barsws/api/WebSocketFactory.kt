package net.flipper.bsb.cloud.barsws.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.URLProtocol
import io.ktor.http.path
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.barsws.api.model.InternalWebsocketEvent
import net.flipper.bsb.cloud.barsws.api.model.toPublic
import net.flipper.bsb.cloud.barsws.api.utils.addAuthHeader
import net.flipper.core.busylib.ktx.common.getExponentialDelay
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info

class WebSocketFactory(
    private val httpClient: HttpClient,
    private val logger: LogTagProvider
) {
    suspend fun open(
        principal: BUSYLibUserPrincipal.Token,
        busyHost: String,
    ): Flow<WebSocketEvent> {
        return channelFlow {
            withContext(NETWORK_DISPATCHER) {
                httpClient.webSocket(
                    request = {
                        url {
                            host = busyHost
                            path("/api/v1/bars/ws")
                            protocol = URLProtocol.WSS
                        }
                        addAuthHeader(principal)
                    }
                ) {
                    logger.info { "Connected to websocket" }
                    while (currentCoroutineContext().isActive) {
                        val message = try {
                            receiveDeserialized<InternalWebsocketEvent>()
                        } catch (e: SerializationException) {
                            logger.error(e) { "Failed deserialize message from websocket" }
                            null
                        }

                        logger.info { "Received message: $message" }
                        message?.let { send(it.toPublic()) }
                    }
                }
            }
        }
    }
}


fun <T> LogTagProvider.wrapWebsocket(
    block: suspend () -> Flow<T>
) = flow<T> {
    var retryCount = 0
    while (currentCoroutineContext().isActive) {
        info { "Subscribe to websocket" }
        block().catch {
            retryCount++
            error(it) { "Failed request websocket" }
        }.collect {
            retryCount = 0
            info { "Receive changes by websocket: $it" }
            emit(it)
        }
        val delayTimeout = getExponentialDelay(retryCount)
        info { "Stop loop, wait ${delayTimeout.inWholeMilliseconds}ms" }
        delay(duration = delayTimeout)
    }
}