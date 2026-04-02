package net.flipper.bsb.cloud.rest.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.http.URLProtocol
import io.ktor.http.path
import kotlinx.coroutines.CoroutineDispatcher
import dev.zacsweers.metro.Inject
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.bsb.cloud.rest.model.BusyCloudTicketResponse
import net.flipper.bsb.cloud.rest.utils.run
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.ktor.di.qualifier.KtorNetworkClientQualifier
import net.flipper.core.ktor.di.qualifier.NetworkCoroutineDispatcher
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, binding = binding<BusyCloudWebSocketTicketApi>())
class BusyCloudWebSocketTicketApiImpl(
    @KtorNetworkClientQualifier
    private val httpClient: HttpClient,
    @NetworkCoroutineDispatcher
    private val dispatcher: CoroutineDispatcher,
    private val bsbHostApi: BUSYLibHostApi,
) : BusyCloudWebSocketTicketApi {
    override suspend fun getTicketToken(
        principal: BUSYLibUserPrincipal.Token
    ): Result<String> {
        return principal.run(dispatcher) {
            httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = bsbHostApi.getHost().value
                    path("/api/v0/auth/ticket")
                }
                addAuth()
            }.body<BusyCloudTicketResponse>().token
        }
    }
}
