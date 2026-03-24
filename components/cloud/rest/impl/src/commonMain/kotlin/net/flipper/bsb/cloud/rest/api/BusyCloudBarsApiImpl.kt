package net.flipper.bsb.cloud.rest.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.isSuccess
import io.ktor.http.path
import kotlinx.coroutines.CoroutineDispatcher
import me.tatarka.inject.annotations.Inject
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.bsb.cloud.rest.model.BSBApiPinRequest
import net.flipper.bsb.cloud.rest.model.BusyCloudBar
import net.flipper.bsb.cloud.rest.model.BusyCloudBarsListResponse
import net.flipper.bsb.cloud.rest.utils.run
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.ktor.di.qualifier.KtorNetworkClientQualifier
import net.flipper.core.ktor.di.qualifier.NetworkCoroutineDispatcher
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.uuid.Uuid

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, BusyCloudBarsApi::class)
class BusyCloudBarsApiImpl(
    @KtorNetworkClientQualifier
    private val httpClient: HttpClient,
    @NetworkCoroutineDispatcher
    private val dispatcher: CoroutineDispatcher,
    private val bsbHostApi: BUSYLibHostApi,
) : BusyCloudBarsApi {
    override suspend fun unlinkBusyBar(
        principal: BUSYLibUserPrincipal.Token,
        uuid: Uuid,
    ): Result<Unit> {
        return principal.run(dispatcher) {
            val response = httpClient.delete {
                url {
                    protocol = URLProtocol.HTTPS
                    host = bsbHostApi.getHost().value
                    path("/api/v0/bars/$uuid")
                }
                addAuth()
            }
            when (response.status) {
                HttpStatusCode.OK -> Unit
                else -> error("Failed to delete bar ${response.bodyAsText()}")
            }
        }
    }

    override suspend fun linkBusyBar(
        principal: BUSYLibUserPrincipal.Token,
        pin: String
    ): Result<Unit> {
        return principal.run(dispatcher) {
            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = bsbHostApi.getHost().value
                    path("/api/v0/bars/link")
                }
                addAuth()
                setBody(BSBApiPinRequest(pin))
            }
            check(response.status.isSuccess()) { "Failed link busy bar" }
        }
    }

    override suspend fun getBarsList(
        principal: BUSYLibUserPrincipal.Token
    ): Result<List<BusyCloudBar>> {
        return principal.run(dispatcher) {
            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = bsbHostApi.getHost().value
                    path("/api/v0/bars/list")
                }
                addAuth()
            }.body<BusyCloudBarsListResponse>()
            response.success.bars
        }
    }
}
