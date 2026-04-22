package net.flipper.bsb.cloud.barsws.api.fakes

import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.bsb.cloud.barsws.api.utils.CloudWebSocketApiImpl

internal data class CloudWebSocketApiTestSetup(
    val api: CloudWebSocketApiImpl,
    val networkStateApi: BUSYLibNetworkStateApi,
    val principalApi: BUSYLibPrincipalApi,
    val hostApi: BUSYLibHostApi,
    val webSocketFactory: MockBSBWebSocketFactory
)
