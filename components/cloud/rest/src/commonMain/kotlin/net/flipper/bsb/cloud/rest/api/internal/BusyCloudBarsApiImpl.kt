package net.flipper.bsb.cloud.rest.api.internal

import io.ktor.client.request.delete
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import me.tatarka.inject.annotations.Inject
import net.flipper.bsb.cloud.rest.api.BusyCloudBarsApi
import net.flipper.bsb.cloud.rest.model.InternalCloudNetworkContext
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, BusyCloudBarsApi::class)
class BusyCloudBarsApiImpl(
    private val networkContext: InternalCloudNetworkContext
) : BusyCloudBarsApi {
    override suspend fun unlinkBusyBar(uuid: String): Result<Unit> {
        return runSuspendCatching(networkContext.networkDispatcher) {
            val response = networkContext.httpClient.delete {
                url("${networkContext.bsbHostApi.getHost().value}/api/v0/bars/$uuid")
            }
            when (response.status) {
                HttpStatusCode.OK -> Result.success(Unit)
                else -> Result.failure(Exception("Failed to delete bar ${response.bodyAsText()}"))
            }
        }
    }
}
