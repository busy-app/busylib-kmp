package net.flipper.bridge.connection.transport.tcp.lan.impl.streaming

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.bridge.connection.transport.common.api.serial.HEADER_NAME_REQUEST_CAPABILITY
import net.flipper.bridge.connection.transport.common.api.serial.StatusStreamingEvent
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.ktx.common.wrapWebsocket
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.verbose
import kotlin.time.Duration.Companion.seconds

class FLanStreamingApiImpl(
    private val httpClient: HttpClient,
    scope: CoroutineScope
) : FStatusStreamingApi, LogTagProvider {
    override val TAG = "FLanStreamingApi"
    private val eventsFlow = wrapWebsocket {
        getWebSocket()
    }.onEach {
        verbose { "Received frame $it" }
    }.filterIsInstance<Frame.Binary>()
        .flowOn(FlipperDispatchers.default)
        .map { StatusStreamingEvent.Protobuf(it.data) }
        .shareIn(scope, SharingStarted.WhileSubscribed(5.seconds), 0)

    private suspend fun getWebSocket(): Flow<Frame> {
        val session = httpClient.webSocketSession("/api/status/ws") {
            headers[HEADER_NAME_REQUEST_CAPABILITY] =
                FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED.ordinal.toString()
        }
        info { "Init websocket $session" }
        session.send(Frame.Text("{\"enable\":true}"))
        return session.incoming.receiveAsFlow().onCompletion {
            withContext(NonCancellable) {
                session.close()
            }
        }
    }

    override fun getEvents() = eventsFlow
}
