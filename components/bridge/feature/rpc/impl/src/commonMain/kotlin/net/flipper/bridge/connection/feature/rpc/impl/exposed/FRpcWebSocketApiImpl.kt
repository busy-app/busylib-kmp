package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcWebSocketApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.bridge.connection.transport.common.api.serial.HEADER_NAME_REQUEST_CAPABILITY
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.verbose

class FRpcWebSocketApiImpl(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher
) : FRpcWebSocketApi, LogTagProvider {
    override val TAG = "FRpcWebSocketApi"

    override suspend fun getScreenFrames(): Result<Flow<ByteArray>> {
        return runSuspendCatching(dispatcher) {
            channelFlow {
                var session: DefaultClientWebSocketSession? = null
                try {
                    session = httpClient.webSocketSession("/api/screen/ws") {
                        headers[HEADER_NAME_REQUEST_CAPABILITY] =
                            FHTTPTransportCapability.BB_WEBSOCKET_SUPPORTED.ordinal.toString()
                    }
                    info { "Init websocket $session" }
                    session.send(Frame.Text("{\"display\":0}"))
                    for (frame in session.incoming) {
                        verbose { "Received frame $frame" }
                        if (frame is Frame.Binary) {
                            send(frame.readBytes())
                        }
                    }
                } finally {
                    withContext(NonCancellable) {
                        session?.close()
                    }
                }
            }.flowOn(dispatcher)
        }
    }
}
