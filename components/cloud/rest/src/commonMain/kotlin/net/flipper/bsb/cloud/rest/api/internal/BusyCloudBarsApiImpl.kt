package net.flipper.bsb.cloud.rest.api.internal

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineDispatcher
import me.tatarka.inject.annotations.Inject
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.bsb.cloud.rest.api.BusyCloudBarsApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.ktor.di.qualifier.KtorNetworkClientQualifier
import net.flipper.core.ktor.di.qualifier.NetworkCoroutineDispatcher
import net.flipper.core.ktor.util.addAuthHeader
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
        return runSuspendCatching(dispatcher) {
            val response = httpClient.delete {
                url("${bsbHostApi.getHost().value}/api/v0/bars/$uuid")
                addAuthHeader(principal)
            }
            when (response.status) {
                HttpStatusCode.OK -> Unit
                else -> error("Failed to delete bar ${response.bodyAsText()}")
            }
        }
    }
}
