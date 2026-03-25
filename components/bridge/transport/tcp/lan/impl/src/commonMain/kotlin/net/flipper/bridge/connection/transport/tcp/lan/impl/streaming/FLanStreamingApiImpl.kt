package net.flipper.bridge.connection.transport.tcp.lan.impl.streaming

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.bridge.connection.transport.common.api.serial.HEADER_NAME_REQUEST_CAPABILITY
import net.flipper.bridge.connection.transport.common.api.serial.StatusStreamingEvent
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.ktx.common.getExponentialDelay
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.verbose
import kotlin.time.Duration.Companion.seconds

class FLanStreamingApiImpl(
    httpClient: HttpClient,
    scope: CoroutineScope
) : FStatusStreamingApi, LogTagProvider {
    override val TAG = "FLanStreamingApi"
    private val eventsFlow = channelFlow {
        info { "Start flow" }
        var retryCount = 0
        while (currentCoroutineContext().isActive) {
            var session: DefaultClientWebSocketSession? = null
            try {
                session = httpClient.webSocketSession("/api/status/ws") {
                    headers[HEADER_NAME_REQUEST_CAPABILITY] =
                        FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED.ordinal.toString()
                }
                info { "Init websocket $session" }
                session.send(Frame.Text("{\"enabled\": true}"))
                retryCount = 0
                for (frame in session.incoming) {
                    verbose { "Received frame $frame" }
                    if (frame is Frame.Binary) {
                        send(frame.readBytes())
                    }
                }
                info { "WebSocket session ended normally, reconnecting..." }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error(e) { "WebSocket error, retryCount=$retryCount" }
            } finally {
                withContext(NonCancellable) {
                    session?.close()
                }
            }
            val delayDuration = getExponentialDelay(retryCount)
            info { "Reconnecting WebSocket in $delayDuration (retry #$retryCount)" }
            delay(delayDuration)
            retryCount++
        }
    }.flowOn(FlipperDispatchers.default)
        .map { StatusStreamingEvent.Protobuf(it) }
        .shareIn(scope, SharingStarted.WhileSubscribed(5.seconds), 0)

    override fun getEvents() = eventsFlow
}