package net.flipper.bsb.cloud.barsws.api.utils

import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketApi
import net.flipper.bsb.cloud.barsws.api.utils.wrappers.BSBWebSocketFactory
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.ktx.common.wrapWebsocket
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.verbose
import kotlin.time.Duration.Companion.seconds

private val NETWORK_DISPATCHER = FlipperDispatchers.default

interface CloudWebSocketApiInternal : CloudWebSocketApi {
    fun getWSInternalFlow(): Flow<BSBWebSocketInternal?>
}

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, binding<CloudWebSocketApi>())
@ContributesBinding(BusyLibGraph::class, binding<CloudWebSocketApiInternal>())
class CloudWebSocketApiImpl(
    networkStateApi: BUSYLibNetworkStateApi,
    principalApi: BUSYLibPrincipalApi,
    hostApi: BUSYLibHostApi,
    private val webSocketFactory: BSBWebSocketFactory,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = NETWORK_DISPATCHER
) : CloudWebSocketApiInternal, LogTagProvider {
    override val TAG = "CloudWebSocketApi"

    private val wsStateFlow = combine(
        networkStateApi.isNetworkAvailableFlow,
        principalApi.getPrincipalFlow(),
        hostApi.getHost()
    ) { isNetworkAvailable, principal, host ->
        if (isNetworkAvailable && principal is BUSYLibUserPrincipal.Token) {
            wrapWebsocket {
                channelFlow {
                    val ws = webSocketFactory.create(
                        logger = this@CloudWebSocketApiImpl,
                        principal = principal,
                        busyHost = host,
                        scope = this,
                        dispatcher = dispatcher
                    )
                    send(ws)
                    // Suspend until the underlying transport dies; then the channelFlow
                    // completes, wrapWebsocket's collect returns, and the outer retry
                    // loop reconnects with exponential backoff.
                    ws.awaitClosed()
                    close()
                }
            }
        } else {
            info {
                "Failed to init websocket. " +
                    "isNetworkAvailable: $isNetworkAvailable, " +
                    "principal: $principal, host: $host"
            }
            flowOf<BSBWebSocketInternal?>(null)
        }
    }.flatMapLatest { it }
        .onEach {
            verbose { "New ws: $it" }
        }
        .shareIn(scope, SharingStarted.WhileSubscribed(stopTimeout = 30.seconds), replay = 1)

    override fun getWSFlow() = wsStateFlow
    override fun getWSInternalFlow() = wsStateFlow
}
