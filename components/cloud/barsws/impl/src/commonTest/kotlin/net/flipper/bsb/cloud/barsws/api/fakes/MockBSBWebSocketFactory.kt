package net.flipper.bsb.cloud.barsws.api.fakes

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.barsws.api.utils.BSBWebSocketInternal
import net.flipper.bsb.cloud.barsws.api.utils.wrappers.BSBWebSocketFactory
import net.flipper.core.busylib.log.LogTagProvider

internal class MockBSBWebSocketFactory(
    private val onWebSocketCreated: () -> Unit = {},
    private val onWebSocketClosed: () -> Unit = {},
    private val onWebSocketCreatedForHost: (String) -> Unit = {}
) : BSBWebSocketFactory {

    private var lastCreatedWebSocket: MockBSBWebSocket? = null

    override suspend fun create(
        logger: LogTagProvider,
        principal: BUSYLibUserPrincipal.Token,
        busyHost: String,
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher
    ): BSBWebSocketInternal {
        onWebSocketCreated()
        onWebSocketCreatedForHost(busyHost)

        val webSocket = MockBSBWebSocket(onWebSocketClosed)
        lastCreatedWebSocket = webSocket
        scope.coroutineContext[Job]?.invokeOnCompletion {
            webSocket.close()
        }
        return webSocket
    }

    fun getLastCreatedWebSocket(): MockBSBWebSocket? = lastCreatedWebSocket
}
