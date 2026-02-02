package net.flipper.bsb.cloud.barsws.api

import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import me.tatarka.inject.annotations.Inject
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.bsb.cloud.barsws.api.utils.getHttpClient
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.log.LogTagProvider
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

internal val NETWORK_DISPATCHER = FlipperDispatchers.default

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, CloudWebSocketBarsApi::class)
class CloudWebSocketBarsApiImpl(
    networkStateApi: BUSYLibNetworkStateApi,
    principalApi: BUSYLibPrincipalApi,
    hostApi: BUSYLibHostApi,
    scope: CoroutineScope
) : CloudWebSocketBarsApi, LogTagProvider {
    override val TAG = "CloudWebSocketBarsApiImpl"

    private val webSocketFactory = WebSocketFactory(
        httpClient = getHttpClient(), logger = this
    )

    private val wsStateFlow = combine(
        networkStateApi.isNetworkAvailableFlow,
        principalApi.getPrincipalFlow(),
        hostApi.getHost()
    ) { isNetworkAvailable, principal, host ->
        if (isNetworkAvailable && principal is BUSYLibUserPrincipal.Token) {
            wrapWebsocket {
                webSocketFactory.open(principal, host)
            }
        } else flowOf()
    }.flatMapLatest { it }
        .shareIn(scope, SharingStarted.WhileSubscribed())


    override fun getWSFlow() = wsStateFlow
}