package net.flipper.bsb.cloud.barsws.api

import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
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
import net.flipper.bsb.cloud.barsws.api.utils.wrapWebsocket
import net.flipper.bsb.cloud.barsws.api.utils.wrappers.BSBWebSocketFactory
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.log.LogTagProvider
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

private val NETWORK_DISPATCHER = FlipperDispatchers.default

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, CloudWebSocketBarsApi::class)
class CloudWebSocketBarsApiImpl(
    networkStateApi: BUSYLibNetworkStateApi,
    principalApi: BUSYLibPrincipalApi,
    hostApi: BUSYLibHostApi,
    private val webSocketFactory: BSBWebSocketFactory,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = NETWORK_DISPATCHER
) : CloudWebSocketBarsApi, LogTagProvider {
    override val TAG = "CloudWebSocketBarsApiImpl"

    private val wsStateFlow = combine(
        networkStateApi.isNetworkAvailableFlow,
        principalApi.getPrincipalFlow(),
        hostApi.getHost()
    ) { isNetworkAvailable, principal, host ->
        if (isNetworkAvailable && principal is BUSYLibUserPrincipal.Token) {
            wrapWebsocket {
                channelFlow {
                    webSocketFactory.create(
                        logger = this@CloudWebSocketBarsApiImpl,
                        principal = principal,
                        busyHost = host,
                        scope = this,
                        dispatcher = dispatcher
                    ).let { send(it) }
                }
            }
        } else {
            flowOf()
        }
    }.flatMapLatest { it }
        .shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)

    override fun getWSFlow() = wsStateFlow
}
