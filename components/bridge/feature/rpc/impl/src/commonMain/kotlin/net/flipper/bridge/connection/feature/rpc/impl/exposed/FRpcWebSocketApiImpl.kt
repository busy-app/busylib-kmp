package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcWebSocketApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.bridge.connection.transport.common.api.serial.HEADER_NAME_REQUEST_CAPABILITY
import net.flipper.core.busylib.ktx.common.getExponentialDelay
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.verbose

class FRpcWebSocketApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher
) : FRpcWebSocketApi, LogTagProvider {
    override val TAG = "FRpcWebSocketApi"

    override fun getScreenFrames(): Flow<ByteArray> {
        return channelFlow {
            var retryCount = 0
            while (currentCoroutineContext().isActive) {
                var session: DefaultClientWebSocketSession? = null
                try {
                    session = httpClient.webSocketSession("/api/screen/ws") {
                        headers[HEADER_NAME_REQUEST_CAPABILITY] =
                            FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED.ordinal.toString()
                    }
                    info { "Init websocket $session" }
                    session.send(Frame.Text("{\"display\":0}"))
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
        }.flowOn(dispatcher)
    }
}
