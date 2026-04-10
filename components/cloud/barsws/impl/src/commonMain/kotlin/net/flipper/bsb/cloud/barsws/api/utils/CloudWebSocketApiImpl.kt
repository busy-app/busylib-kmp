package net.flipper.bsb.cloud.barsws.api.utils

import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import me.tatarka.inject.annotations.Inject
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.bsb.cloud.barsws.api.utils.wrappers.BSBWebSocketFactory
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

private val NETWORK_DISPATCHER = FlipperDispatchers.default

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, CloudWebSocketApi::class)
class CloudWebSocketApiImpl(
    networkStateApi: BUSYLibNetworkStateApi,
    principalApi: BUSYLibPrincipalApi,
    hostApi: BUSYLibHostApi,
    private val webSocketFactory: BSBWebSocketFactory,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = NETWORK_DISPATCHER
) : CloudWebSocketApi, LogTagProvider {
    override val TAG = "CloudWebSocketApi"

    private val wsStateFlow = combine(
        networkStateApi.isNetworkAvailableFlow,
        principalApi.getPrincipalFlow(),
        hostApi.getHost()
    ) { isNetworkAvailable, principal, host ->
        if (isNetworkAvailable && principal is BUSYLibUserPrincipal.Token) {
            wrapWebsocket {
                channelFlow {
                    webSocketFactory.create(
                        logger = this@CloudWebSocketApiImpl,
                        principal = principal,
                        busyHost = host,
                        scope = this,
                        dispatcher = dispatcher
                    ).let { send(it) }
                    awaitClose()
                }
            }
        } else {
            info {
                "Failed to init websocket. " +
                    "isNetworkAvailable: $isNetworkAvailable, " +
                    "principal: $principal, host: $host"
            }
            flowOf<BSBWebSocket?>(null)
        }
    }.flatMapLatest { it }
        .shareIn(scope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), replay = 1)

    override fun getWSFlow() = wsStateFlow
}
