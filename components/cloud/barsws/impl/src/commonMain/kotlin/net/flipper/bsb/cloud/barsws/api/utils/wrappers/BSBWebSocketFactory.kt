package net.flipper.bsb.cloud.barsws.api.utils.wrappers

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.barsws.api.BSBWebSocket
import net.flipper.bsb.cloud.barsws.api.getBSBWebSocket
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.ktor.getHttpClient
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Factory interface for creating BSBWebSocket instances.
 * This abstraction allows for easier testing by enabling mock implementations.
 */
interface BSBWebSocketFactory {
    /**
     * Creates a new BSBWebSocket connection.
     *
     * @param logger Logger for WebSocket operations
     * @param principal User authentication token
     * @param busyHost The host to connect to
     * @param scope CoroutineScope for the WebSocket lifetime
     * @param dispatcher Dispatcher for WebSocket operations
     * @return A connected BSBWebSocket instance
     */
    suspend fun create(
        logger: LogTagProvider,
        principal: BUSYLibUserPrincipal.Token,
        busyHost: String,
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher
    ): BSBWebSocket
}

/**
 * Default implementation that creates real WebSocket connections using Ktor.
 */
@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, BSBWebSocketFactory::class)
class BSBWebSocketFactoryImpl : BSBWebSocketFactory {
    private val httpClient: HttpClient = getHttpClient()

    override suspend fun create(
        logger: LogTagProvider,
        principal: BUSYLibUserPrincipal.Token,
        busyHost: String,
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher
    ): BSBWebSocket {
        return getBSBWebSocket(
            httpClient = httpClient,
            logger = logger,
            principal = principal,
            busyHost = busyHost,
            scope = scope,
            dispatcher = dispatcher
        )
    }
}
