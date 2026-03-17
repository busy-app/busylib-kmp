package net.flipper.bsb.cloud.rest.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.URLProtocol
import io.ktor.http.path
import kotlinx.coroutines.CoroutineDispatcher
import me.tatarka.inject.annotations.Inject
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.bsb.cloud.rest.model.BusyCloudAccessTokenRequest
import net.flipper.bsb.cloud.rest.model.BusyCloudAccessTokenResponse
import net.flipper.bsb.cloud.rest.utils.run
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.ktor.di.qualifier.KtorNetworkClientQualifier
import net.flipper.core.ktor.di.qualifier.NetworkCoroutineDispatcher
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.uuid.Uuid

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, BusyCloudAccessTokenApi::class)
class BusyCloudAccessTokenApiImpl(
    @KtorNetworkClientQualifier
    private val httpClient: HttpClient,
    @NetworkCoroutineDispatcher
    private val dispatcher: CoroutineDispatcher,
    private val bsbHostApi: BUSYLibHostApi,
) : BusyCloudAccessTokenApi {
    override suspend fun generateAccessToken(
        principal: BUSYLibUserPrincipal.Token,
        deviceId: Uuid
    ): Result<BusyCloudAccessTokenResponse> {
        return principal.run(dispatcher) {
            httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = bsbHostApi.getHost().value
                    path("/api/v0/bars/$deviceId/access-token")
                }
                addAuth()
                setBody(BusyCloudAccessTokenRequest())
            }.body<BusyCloudAccessTokenResponse>()
        }
    }
}
