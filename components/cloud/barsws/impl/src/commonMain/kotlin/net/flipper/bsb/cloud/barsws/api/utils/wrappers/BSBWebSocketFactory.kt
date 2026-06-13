package net.flipper.bsb.cloud.barsws.api.utils.wrappers

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.barsws.api.utils.BSBWebSocketInternal
import net.flipper.bsb.cloud.barsws.api.utils.getBSBWebSocket
import net.flipper.bsb.cloud.rest.api.BusyCloudWebSocketTicketApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.ktor.di.qualifier.KtorNetworkClientQualifier

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
    ): BSBWebSocketInternal
}

/**
 * Default implementation that creates real WebSocket connections using Ktor.
 */
@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, binding = binding<BSBWebSocketFactory>())
class BSBWebSocketFactoryImpl(
    @KtorNetworkClientQualifier
    private val httpClient: HttpClient,
    private val ticketApi: BusyCloudWebSocketTicketApi
) : BSBWebSocketFactory {

    override suspend fun create(
        logger: LogTagProvider,
        principal: BUSYLibUserPrincipal.Token,
        busyHost: String,
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher
    ): BSBWebSocketInternal {
        return getBSBWebSocket(
            httpClient = httpClient,
            ticketApi = ticketApi,
            logger = logger,
            principal = principal,
            busyHost = busyHost,
            scope = scope,
            dispatcher = dispatcher
        )
    }
}
