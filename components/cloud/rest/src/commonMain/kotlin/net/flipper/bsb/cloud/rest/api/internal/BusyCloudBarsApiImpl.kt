package net.flipper.bsb.cloud.rest.api.internal

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineDispatcher
import me.tatarka.inject.annotations.Inject
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.bsb.cloud.rest.api.BusyCloudBarsApi
import net.flipper.bsb.cloud.rest.util.addAuthHeader
import net.flipper.bsb.cloud.rest.util.requireTokenPrincipal
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.runSuspendCatching
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
    private val principalApi: BUSYLibPrincipalApi
) : BusyCloudBarsApi {
    override suspend fun unlinkBusyBar(uuid: Uuid): Result<Unit> {
        return runSuspendCatching(dispatcher) {
            val response = httpClient.delete {
                url("${bsbHostApi.getHost().value}/api/v0/bars/$uuid")
                addAuthHeader(principalApi.requireTokenPrincipal())
            }

            when (response.status) {
                HttpStatusCode.OK -> Result.success(Unit)
                else -> Result.failure(Exception("Failed to delete bar ${response.bodyAsText()}"))
            }
        }
    }
}
